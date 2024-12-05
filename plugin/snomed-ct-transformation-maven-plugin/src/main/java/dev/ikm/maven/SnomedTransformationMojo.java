package dev.ikm.maven;

import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.entity.EntityService;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Mojo(name = "run-snomed-transformation", defaultPhase = LifecyclePhase.INSTALL)
public class SnomedTransformationMojo extends AbstractMojo {
    private static final Logger LOG = LoggerFactory.getLogger(SnomedTransformationMojo.class.getSimpleName());

    @Parameter(property = "origin.namespace", required = true)
    String namespaceString;
    @Parameter(property = "datastorePath", required = true)
    private String datastorePath;
    @Parameter(property = "inputDirectoryPath", required = true)
    private String inputDirectoryPath;
    @Parameter(property = "dataOutputPath", required = true)
    private String dataOutputPath;
    @Parameter(property = "controllerName", defaultValue = "Open SpinedArrayStore")
    private String controllerName;

    private UUID namespace;

    public void execute() throws MojoExecutionException {
        try {
            this.namespace = UUID.fromString(namespaceString);

            File datastore = new File(datastorePath);
            String unzippedData = unzipRawData(inputDirectoryPath);
            File inputFileOrDirectory = new File(unzippedData);
            validateInputDirectory(inputFileOrDirectory);

            transformFile(datastore, inputFileOrDirectory);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Invalid namespace for UUID formatting");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String unzipRawData(String zipFilePath) throws IOException {
        File outputDirectory = new File(dataOutputPath);
        try(ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                File newFile = new File(outputDirectory, zipEntry.getName());
                if(zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try(FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer,0,len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        File terminologyFolder = searchTerminologyFolder(outputDirectory);

        if (terminologyFolder != null) {
            return terminologyFolder.getAbsolutePath();
        } else {
            throw new FileNotFoundException("The 'Terminology' folder could not be found...");
        }
    }

    private static File searchTerminologyFolder(File dir) {
        if (dir.isDirectory()){
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if(file.isDirectory() && file.getName().equals("Terminology") &&
                        file.getParentFile().getName().equals("Full")) {
                        return file;
                    }
                    File found = searchTerminologyFolder(file);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private void validateInputDirectory(File inputFileOrDirectory) throws MojoExecutionException {
        if(!inputFileOrDirectory.exists()){
            throw new RuntimeException("Invalid input directory or file. Directory or file does not exist");
        }
    }

    /**
     * Transforms each snomed file in a directory based on filename
     *
     * @param datastore location of datastore to write entities to
     * @param inputFileOrDirectory directory containing snomed files
     */
    public void transformFile(File datastore, File inputFileOrDirectory){
        LOG.info("########## Snomed Transformer Starting...");
        initializeDatastore(datastore);

        EntityService.get().beginLoadPhase();
        try {
            Composer composer = new Composer("Snomed Transformer Composer");
            processFilesFromInput(inputFileOrDirectory, composer);
            composer.commitAllSessions();
        } finally {
            EntityService.get().endLoadPhase();
            PrimitiveData.stop();
            LOG.info("########## Snomed Transformer Finishing...");
        }
    }

    private void initializeDatastore(File datastore){
        CachingService.clearAll();
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastore);
        PrimitiveData.selectControllerByName(controllerName);
        PrimitiveData.start();
    }

    private void processFilesFromInput(File inputFileOrDirectory, Composer composer){
        if(inputFileOrDirectory.isDirectory()){
            Arrays.stream(inputFileOrDirectory.listFiles())
                    .filter(file -> file.getName().endsWith(".txt"))
                    .forEach(file -> processIndividualFile(file, composer));
        } else if (inputFileOrDirectory.isFile() && inputFileOrDirectory.getName().endsWith(".txt")) {
            processIndividualFile(inputFileOrDirectory, composer);
        }
    }

    private void processIndividualFile(File file, Composer composer) {
        String fileName = file.getName();
        Transformer transformer = getTransformer(fileName);

        if (transformer != null) {
            LOG.info("### Transformer Starting for file: " + fileName);
            transformer.transform(file, composer);
            LOG.info("### Transformer Finishing for file : " + fileName);
        } else {
            LOG.info("This file cannot be processed at the moment : " + file.getName());
        }
    }

    /**
     * Checks files for matching keywords and uses appropriate transformer
     *
     * @param fileName File for Transformer match
     */
    private Transformer getTransformer(String fileName) {
        if(fileName.contains("Concept")){
            return new ConceptTransformer(namespace);
        } else if(fileName.contains("Definition")){
            return new DefinitionTransformer(namespace);
        } else if(fileName.contains("Description")){
            return new DescriptionTransformer(namespace);
        } else if(fileName.contains("Language")){
            return new LanguageTransformer(namespace);
        } else if(fileName.contains("OWLExpression")){
            return new AxiomSyntaxTransformer(namespace);
        }
        return null;
    }
}
