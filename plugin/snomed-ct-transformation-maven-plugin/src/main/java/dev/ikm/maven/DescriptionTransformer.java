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

public class DescriptionTransformer extends AbstractTransformer{
    private static final Logger LOG = LoggerFactory.getLogger(DescriptionTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int CONCEPT_ID = 4;
    private static final int LANGUAGE_CODE = 5;
    private static final int TYPE_ID = 6;
    private static final int TERM = 7;
    private static final int CASE_SIGNIFICANCE = 8;
    private String previousRowId;

    DescriptionTransformer(UUID namespace) {
        super(namespace);
    }

    /**
     * This method uses a description file and transforms it into a list of entities
     * @param descriptionFile
     * @Returns void
     */
    @Override
    public void transform(File descriptionFile, Composer composer){
        EntityProxy.Concept author = SnomedUtility.getUserConcept(namespace);
        EntityProxy.Concept path = SnomedUtility.getPathConcept();

        try (Stream<String> lines = Files.lines(descriptionFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split("\t"))
                    .forEach(data -> {
                        State status = Integer.parseInt(data[ACTIVE]) == 1 ? State.ACTIVE : State.INACTIVE;
                        long time = SnomedUtility.snomedTimestampToEpochSeconds(data[EFFECTIVE_TIME]);
                        EntityProxy.Concept moduleId = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[MODULE_ID])));

                        EntityProxy.Concept descriptionTypeConcept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[TYPE_ID])));
                        EntityProxy.Concept languageTypeConcept = TransformationHelper.getLanguageConcept(data[LANGUAGE_CODE]);
                        EntityProxy.Concept caseSensitivityConcept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[CASE_SIGNIFICANCE])));

                        EntityProxy.Concept concept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[CONCEPT_ID])));
                        EntityProxy.Semantic definitionSemantic = EntityProxy.Semantic.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[ID])));

                        Session session = composer.open(status, time, author, moduleId, path);
                        session.compose((SemanticAssembler semanticAssembler) -> semanticAssembler
                                .semantic(definitionSemantic)
                                .pattern(TinkarTerm.DESCRIPTION_PATTERN)
                                .reference(concept)
                                .fieldValues(fieldValues -> fieldValues
                                        .with(languageTypeConcept)
                                        .with(data[TERM])
                                        .with(caseSensitivityConcept)
                                        .with(descriptionTypeConcept)
                                ));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
