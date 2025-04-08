package dev.ikm.maven;

import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.SemanticAssembler;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class LanguageTransformer extends AbstractTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(LanguageTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int REFSET_ID = 4;
    private static final int REFERENCED_COMPONENT_ID = 5;
    private static final int ACCEPTABILITY_ID = 6;

    LanguageTransformer(UUID namespace) {
        super(namespace);
    }

    /**
     * This method uses a language file and transforms it into a list of entities
     * @param languageFile
     * @Returns EntityList
     */
    @Override
    public void transform(File languageFile, Composer composer) {
        List<Entity<? extends EntityVersion>> semantics = new ArrayList<>();
        EntityProxy.Concept author = SnomedUtility.getUserConcept(namespace);
        EntityProxy.Concept path = SnomedUtility.getPathConcept();

        try (Stream<String> lines = Files.lines(languageFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split("\t"))
                    .forEach((data) -> {
                        State status = Integer.parseInt(data[ACTIVE]) == 1 ? State.ACTIVE : State.INACTIVE;
                        long epochTime = SnomedUtility.snomedTimestampToEpochSeconds(data[EFFECTIVE_TIME]);

                        EntityProxy.Concept moduleId = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[MODULE_ID])));
                        EntityProxy.Concept referencedComponent = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[REFERENCED_COMPONENT_ID])));

                        Session session = composer.open(status, epochTime, author, moduleId, path);
                        session.compose((SemanticAssembler semanticAssembler) -> semanticAssembler
                                .pattern(TransformationHelper.getDialectPattern(data[REFSET_ID]))
                                .reference(referencedComponent)
                                .fieldValues(fieldValues -> fieldValues
                                        .with(EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[ACCEPTABILITY_ID])))
                                )));
                    });
        } catch(IOException e) {
            LOG.warn("Error parsing language file");
        }
    }
}
