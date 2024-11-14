package dev.ikm.maven;

import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.ConceptAssembler;
import dev.ikm.tinkar.composer.template.*;
import dev.ikm.tinkar.entity.transaction.Transaction;
import dev.ikm.tinkar.ext.lang.owl.Rf2OwlToLogicAxiomTransformer;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import dev.ikm.tinkar.terms.TinkarTerm;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import dev.ikm.maven.datastore.proxy.DatastoreProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import static dev.ikm.tinkar.terms.TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE;
import static dev.ikm.tinkar.terms.TinkarTerm.ENGLISH_LANGUAGE;

@Mojo(name = "run-snomed-transformation", defaultPhase = LifecyclePhase.INSTALL)
public class SnomedTransformationMojo extends AbstractMojo {
    private static final Logger LOG = LoggerFactory.getLogger(SnomedTransformationMojo.class.getSimpleName());
    public void execute() throws MojoExecutionException
    {

        File datastore = new File(System.getProperty("user.home") + "/Solor/" + "generated-data");
        File inputFileOrDirectory = new File("/Users/jsteinhafel/Downloads/SnomedCT_InternationalRF2_PRODUCTION_20241001T120000Z/Full/Terminology");
        if(!inputFileOrDirectory.exists()){
            throw new RuntimeException("Invalid input directory or file. Directory or file does not exist");
        }
        String controllerName = "Open SpinedArrayStore";

        transformFile(datastore, inputFileOrDirectory, controllerName);
    }

    /**
     * Transforms each snomed file in a directory based on filename
     *
     * @param datastore location of datastore to write entities to
     * @param inputFileOrDirectory directory containing snomed files
     * @param controllerName type of datastore to start
     */
    public void transformFile(File datastore, File inputFileOrDirectory, String controllerName){
        LOG.info("########## Snomed Transformer Starting...");
        if(inputFileOrDirectory == null || !inputFileOrDirectory.exists()){
            throw new RuntimeException("Invalid input directory or file. Directory or file does not exist" + inputFileOrDirectory);
        }
        CachingService.clearAll();
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastore);
        PrimitiveData.selectControllerByName(controllerName);
        PrimitiveData.start();
        Composer composer = new Composer("Snomed Transformer Composer");
        //Process all the text files inside the directory.
        if(inputFileOrDirectory.isDirectory()){
            Arrays.stream(inputFileOrDirectory.listFiles()).filter(p -> p.getName().endsWith(".txt")).forEach(file ->
                    transformFile(file, composer)
            );
        }else if(inputFileOrDirectory.isFile()
                && inputFileOrDirectory.getName().endsWith(".txt")){
            transformFile(inputFileOrDirectory, composer);
        }
        composer.commitAllSessions();
        runAxiomSyntaxTransformer();
        PrimitiveData.stop();
        LOG.info("########## Snomed Transformer Finishing...");
    }

    /**
     * Checks files for matching keywords and uses appropriate transformer
     *
     * @param file File to transform
     */
    private static void transformFile(File file, Composer composer) {
        String fileName = file.getName();
        Boolean success = true;
        LOG.info("### Transformer Starting for file :"+ fileName);
        if(fileName.contains("Concept")){
            ConceptTransformer ctc = new ConceptTransformer();
            ctc.transform(file, composer);
        }
        else if(fileName.contains("Definition")){
            DefinitionTransformer dtc = new DefinitionTransformer();
            dtc.transform(file, composer);
        }
        else if(fileName.contains("Description")){
            DescriptionTransformer dtc = new DescriptionTransformer();
            dtc.transform(file, composer);
        }
        else if(fileName.contains("Language")){
            LanguageTransformer ltc = new LanguageTransformer();
            ltc.transformLanguageSemantics(file, composer);
        }
        else if(fileName.contains("OWLExpression")){
            AxiomSyntaxTransformer astc = new AxiomSyntaxTransformer();
            astc.transform(file, composer);
        }
        else{
            LOG.info("This file cannot be processed at the moment : " + file.getName());
            success = false;
        }
        if(success){
            LOG.info("### Transformer Finishing for file : " + fileName);
        }
    }

    private static void runAxiomSyntaxTransformer() {
        LOG.info("########## Transforming OWL Axioms...");
        Transaction owlTransformationTransaction = Transaction.make();
        try {
            new Rf2OwlToLogicAxiomTransformer(
                    owlTransformationTransaction,
                    TinkarTerm.OWL_AXIOM_SYNTAX_PATTERN,
                    TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN).call();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
