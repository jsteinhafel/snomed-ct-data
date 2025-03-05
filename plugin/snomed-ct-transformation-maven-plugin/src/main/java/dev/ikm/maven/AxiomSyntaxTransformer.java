package dev.ikm.maven;

import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.template.AxiomSyntax;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.stream.Stream;

public class AxiomSyntaxTransformer extends AbstractTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(AxiomSyntaxTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int REFSET_ID = 4;
    private static final int REFERENCED_COMPONENT_ID = 5;
    private static final int OWL_EXPRESSION = 6;
    String previousRowId;

    AxiomSyntaxTransformer(UUID namespace) {
        super(namespace);
    }

    /**
     * Parses OWL Expression file and creates Axiom Semantics for each line
     *
     * @param axiomFile input file Path
     */
    @Override
    public void transform(File axiomFile, Composer composer) {
        EntityProxy.Concept author = SnomedUtility.getUserConcept(namespace);
        EntityProxy.Concept path = SnomedUtility.getPathConcept();

        try (Stream<String> lines = Files.lines(axiomFile.toPath())) {
            lines.skip(1)
                    .map(row -> row.split("\t"))
                    .forEach(data -> {
                        State status = data[ACTIVE].equals("1") ? State.ACTIVE : State.INACTIVE;
                        long time = SnomedUtility.snomedTimestampToEpochSeconds(data[EFFECTIVE_TIME]);
                        EntityProxy.Concept module = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[MODULE_ID])));

                        Session session = composer.open(status, time, author, module, path);
                        configureSemanticsForConcept(session, data);
                    });
        } catch(IOException | SecurityException ex) {
            LOG.info(ex.toString());
        }
    }

    /**
     * Checks previous row for duplicate id and creates a version instead of a new semantic
     *
     * @param session initialized Session from the Composer API
     * @param columns line from OWL expression file split on tabs
     */
    private void configureSemanticsForConcept(Session session, String[] columns) {
        String owlExpressionWithPublicIds = SnomedUtility.owlAxiomIdsToPublicIds(namespace, columns[OWL_EXPRESSION]);

        EntityProxy.Concept concept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, columns[REFERENCED_COMPONENT_ID])));
        EntityProxy.Semantic axiomSemantic = EntityProxy.Semantic.make(PublicIds.of(SnomedUtility.generateUUID(namespace, columns[ID])));
        previousRowId = columns[ID];

        session.compose(new AxiomSyntax()
                        .semantic(axiomSemantic)
                        .text(owlExpressionWithPublicIds),
                concept);
    }

}
