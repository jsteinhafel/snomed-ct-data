package dev.ikm.maven;

import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.composer.Composer;
import dev.ikm.tinkar.composer.Session;
import dev.ikm.tinkar.composer.assembler.ConceptAssembler;
import dev.ikm.tinkar.composer.template.Identifier;
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

public class ConceptTransformer extends AbstractTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(ConceptTransformer.class.getSimpleName());
    private static final int ID = 0;
    private static final int EFFECTIVE_TIME = 1;
    private static final int ACTIVE = 2;
    private static final int MODULE_ID = 3;
    private static final int DEFINITION_STATUS_ID = 4;
    private String previousRowId;

    ConceptTransformer(UUID namespace) {
        super(namespace);
    }

    /**
     * transforms concept file into entity
     * @param inputFile concept input txt file
     */
    @Override
    public void transform(File inputFile, Composer composer){
        if(inputFile == null || !inputFile.exists() || !inputFile.isFile()){
            throw new RuntimeException("Concept input file is either null or invalid.");
        }
        EntityProxy.Concept author = SnomedUtility.getUserConcept(namespace);
        EntityProxy.Concept path = SnomedUtility.getPathConcept();

        try (Stream<String> lines = Files.lines(inputFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split("\t"))
                    .forEach(data -> {
                        State status = Integer.parseInt(data[ACTIVE]) == 1 ? State.ACTIVE : State.INACTIVE;
                        long time = SnomedUtility.snomedTimestampToEpochSeconds(data[EFFECTIVE_TIME]);
                        EntityProxy.Concept moduleIdConcept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace, data[MODULE_ID])));
                        EntityProxy.Concept concept = EntityProxy.Concept.make(PublicIds.of(SnomedUtility.generateUUID(namespace,data[ID])));

                        Session session = composer.open(status, time, author, moduleIdConcept, path);
                        if(!data[ID].equals(previousRowId)) {
                            session.compose((ConceptAssembler conceptAssembler) -> conceptAssembler
                                    .concept(concept)
                                    .attach((Identifier identifier) -> identifier
                                            .source(TinkarTerm.UNIVERSALLY_UNIQUE_IDENTIFIER)
                                            .identifier(concept.asUuidArray()[0].toString())
                                    )
                                    .attach((Identifier identifier) -> identifier
                                            .source(TinkarTerm.SCTID)
                                            .identifier(data[ID])
                                    )
                            );
                        } else {
                            session.compose((ConceptAssembler conceptAssembler) -> conceptAssembler.concept(concept));
                        }
                        previousRowId = data[ID];
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
