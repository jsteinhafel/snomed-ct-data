package dev.ikm.maven;

import dev.ikm.tinkar.common.id.IntIds;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.ConceptAssembler;
import dev.ikm.tinkar.composer.assembler.SemanticAssembler;
import dev.ikm.tinkar.composer.template.Definition;
import dev.ikm.tinkar.composer.template.FullyQualifiedName;
import dev.ikm.tinkar.composer.template.Identifier;
import dev.ikm.tinkar.composer.template.StatedAxiom;
import dev.ikm.tinkar.composer.template.Synonym;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.UUID;

import static dev.ikm.tinkar.terms.TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE;
import static dev.ikm.tinkar.terms.TinkarTerm.ENGLISH_LANGUAGE;

@Mojo(name = "run-snomed-starterdata", defaultPhase = LifecyclePhase.INSTALL)
public class SnomedStarterDataMojo extends AbstractMojo
{
    @Parameter(property = "origin.namespace", required = true)
    String namespaceString;
    @Parameter(property = "datastorePath", required = true)
    private String datastorePath;
    @Parameter(property = "controllerName", defaultValue = "Open SpinedArrayStore")
    private String controllerName;

    public void execute() throws MojoExecutionException
    {
        try {
            UUID namespace = UUID.fromString(namespaceString);
            File datastore = new File(datastorePath);

            CachingService.clearAll();
            ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastore);
            PrimitiveData.selectControllerByName(controllerName);
            PrimitiveData.start();

            Composer composer = new Composer("Snomed Starter Data Composer");

            Session session = composer.open(State.ACTIVE,
                    TinkarTerm.USER,
                    TinkarTerm.PRIMORDIAL_MODULE,
                    TinkarTerm.PRIMORDIAL_PATH);

            EntityProxy.Concept snomedAuthor = EntityProxy.Concept.make("IHTSDO SNOMED CT Author", UuidT5Generator.get(namespace, "IHTSDO SNOMED CT Author"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(snomedAuthor)
                    .attach((FullyQualifiedName fqn) -> fqn
                            .language(ENGLISH_LANGUAGE)
                            .text("IHTSDO SNOMED CT Author")
                            .caseSignificance(DESCRIPTION_NOT_CASE_SENSITIVE)
                    )
                    .attach((Synonym synonym)-> synonym
                            .language(ENGLISH_LANGUAGE)
                            .text("SNOMED CT Author")
                            .caseSignificance(DESCRIPTION_NOT_CASE_SENSITIVE)
                    )
                    .attach((Definition definition) -> definition
                            .language(ENGLISH_LANGUAGE)
                            .text("International Health Terminology Standards Development Organisation (IHTSDO) SNOMED CT Author")
                            .caseSignificance(DESCRIPTION_NOT_CASE_SENSITIVE)
                    )
                    .attach((Identifier identifier) -> identifier
                            .source(TinkarTerm.UNIVERSALLY_UNIQUE_IDENTIFIER)
                            .identifier(snomedAuthor.asUuidArray()[0].toString())
                    )
                    .attach((StatedAxiom statedAxiom) -> statedAxiom
                            .isA(TinkarTerm.USER)
                    )
            );

            EntityProxy.Concept snomedIdentifier = EntityProxy.Concept.make("SNOMED CT Identifier",  UuidT5Generator.get(namespace,"900000000000294009"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(snomedIdentifier)
                    .attach((FullyQualifiedName fqn) -> fqn
                            .language(ENGLISH_LANGUAGE)
                            .text("SNOMED CT Identifier")
                            .caseSignificance(DESCRIPTION_NOT_CASE_SENSITIVE)
                    )
                    .attach((Synonym synonym)-> synonym
                            .language(ENGLISH_LANGUAGE)
                            .text("SCTID")
                            .caseSignificance(DESCRIPTION_NOT_CASE_SENSITIVE)
                    )
                    .attach((Definition definition) -> definition
                            .language(ENGLISH_LANGUAGE)
                            .text("Unique point of origin for identifier")
                            .caseSignificance(DESCRIPTION_NOT_CASE_SENSITIVE)
                    )
                    .attach((Identifier identifier) -> identifier
                            .source(TinkarTerm.UNIVERSALLY_UNIQUE_IDENTIFIER)
                            .identifier(snomedIdentifier.asUuidArray()[0].toString())
                    )
                    .attach((StatedAxiom statedAxiom) -> statedAxiom
                            .isA(TinkarTerm.IDENTIFIER_SOURCE)
                    )
            );

            EntityProxy.Concept descriptionType = EntityProxy.Concept.make("Description Type", TinkarTerm.DESCRIPTION_TYPE.uuids()[0], UuidT5Generator.get(namespace,"900000000000446008"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(descriptionType));

            EntityProxy.Concept fullySpecifiedName = EntityProxy.Concept.make("Fully Specified Name", TinkarTerm.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE.uuids()[0], TinkarTerm.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE.uuids()[1], UuidT5Generator.get(namespace,"900000000000003001"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(fullySpecifiedName));

            EntityProxy.Concept synonym = EntityProxy.Concept.make("Synonym", TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE.uuids()[0], TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE.uuids()[1], UuidT5Generator.get(namespace,"900000000000013009"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(synonym));

            EntityProxy.Concept definition = EntityProxy.Concept.make("Definition", TinkarTerm.DEFINITION_DESCRIPTION_TYPE.uuids()[0], UuidT5Generator.get(namespace,"900000000000550004"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(definition));

            EntityProxy.Concept caseSignificance = EntityProxy.Concept.make("Case significance", TinkarTerm.DESCRIPTION_CASE_SIGNIFICANCE.uuids()[0], TinkarTerm.DESCRIPTION_CASE_SIGNIFICANCE.uuids()[1], UuidT5Generator.get(namespace,"900000000000447004"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(caseSignificance));

            EntityProxy.Concept entireTermCaseInsensitive = EntityProxy.Concept.make("Entire term case insensitive", TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE.uuids()[0], UuidT5Generator.get(namespace,"900000000000448009"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(entireTermCaseInsensitive));

            EntityProxy.Concept entireTermCaseSensitive = EntityProxy.Concept.make("Entire term case sensitive", TinkarTerm.DESCRIPTION_CASE_SENSITIVE.uuids()[0], UuidT5Generator.get(namespace,"900000000000017005"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(entireTermCaseSensitive));

            EntityProxy.Concept initialCharCaseSensitive = EntityProxy.Concept.make("Initial character case sensitive", TinkarTerm.DESCRIPTION_INITIAL_CHARACTER_CASE_SENSITIVE.uuids()[0], UuidT5Generator.get(namespace,"900000000000020002"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(initialCharCaseSensitive));

            EntityProxy.Concept acceptability = EntityProxy.Concept.make("Acceptability", TinkarTerm.DESCRIPTION_ACCEPTABILITY.uuids()[0], UuidT5Generator.get(namespace,"900000000000511003"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(acceptability));

            EntityProxy.Concept acceptable = EntityProxy.Concept.make("Acceptable", TinkarTerm.ACCEPTABLE.uuids()[0], UuidT5Generator.get(namespace,"900000000000549004"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(acceptable));

            EntityProxy.Concept preferred = EntityProxy.Concept.make("Preferred", TinkarTerm.PREFERRED.uuids()[0], UuidT5Generator.get(namespace,"900000000000548007"));
            session.compose((ConceptAssembler concept) -> concept
                    .concept(preferred));

            // needed for US editions to function properly
            EntityProxy.Concept snomedCoreModule = EntityProxy.Concept.make("SNOMED CT Core Module", UuidT5Generator.get(namespace, "900000000000207008"));
            EntityProxy.Concept snomedUsModule = EntityProxy.Concept.make("SNOMED CT US Module",  UuidT5Generator.get(namespace, "731000124108"));
            session.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                    .concept(snomedCoreModule));
            session.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                    .concept(snomedUsModule));
            session.compose((SemanticAssembler semanticAssembler) -> semanticAssembler
                    .reference(snomedUsModule)
                    .pattern(TinkarTerm.MODULE_ORIGINS_PATTERN)
                    .fieldValues(fieldVals -> fieldVals
                            .with(IntIds.set.of(snomedCoreModule.nid()))));

            composer.commitSession(session);
            PrimitiveData.stop();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to execute class", e);
        }

    }
}
