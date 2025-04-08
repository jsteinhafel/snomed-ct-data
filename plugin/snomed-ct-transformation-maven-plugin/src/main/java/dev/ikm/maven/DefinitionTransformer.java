package dev.ikm.maven;

import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.SemanticAssembler;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.stream.Stream;

public class DefinitionTransformer extends AbstractTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(DefinitionTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int CONCEPT_ID = 4;
    private static final int LANGUAGE_CODE = 5;
    private static final int TYPE_ID = 6;
    private static final int TERM = 7;
    private static final int CASE_SIGNIFICANCE_ID = 8;
    private static String previousRowId;
    private static EntityProxy.Concept previousReferencedConcept;
    private static EntityProxy.Semantic previousDefinitionSemantic;

    DefinitionTransformer(UUID namespace) {
        super(namespace);
    }


    /**
     * Parses Definition file and creates Definition Semantics for each line
     *
     * @param definitionFile input file to parse
     */
    @Override
    public void transform(File definitionFile, Composer composer) {
        EntityProxy.Concept author = SnomedUtility.getUserConcept(namespace);
        EntityProxy.Concept path = SnomedUtility.getPathConcept();

        try (Stream<String> lines = Files.lines(definitionFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split("\t"))
                    .forEach(data -> {
                        State status = Integer.parseInt(data[ACTIVE]) == 1 ? State.ACTIVE : State.INACTIVE;
                        long epochTime = SnomedUtility.snomedTimestampToEpochSeconds(data[EFFECTIVE_TIME]);
                        EntityProxy.Concept moduleConcept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[MODULE_ID])));

                        Session session = composer.open(status, epochTime, author, moduleConcept, path);

                        if (!data[ID].equals(previousRowId)) {
                            previousRowId = data[ID];
                            previousReferencedConcept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[CONCEPT_ID])));
                            previousDefinitionSemantic = EntityProxy.Semantic.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[ID])));
                        }

                        EntityProxy.Concept languageConcept = TransformationHelper.getLanguageConcept(data[LANGUAGE_CODE]);
                        EntityProxy.Concept caseSignificanceConcept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[CASE_SIGNIFICANCE_ID])));
                        EntityProxy.Concept descriptionTypeConcept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[TYPE_ID])));

                        session.compose((SemanticAssembler semanticAssembler) -> semanticAssembler
                                .semantic(previousDefinitionSemantic)
                                .pattern(TinkarTerm.DESCRIPTION_PATTERN)
                                .reference(previousReferencedConcept)
                                .fieldValues(fieldValues -> fieldValues
                                        .with(languageConcept)
                                        .with(data[TERM])
                                        .with(caseSignificanceConcept)
                                        .with(descriptionTypeConcept)
                                ));
                    });
        } catch (IOException | SecurityException ex) {
            LOG.info(ex.toString());
        }
    }

}
