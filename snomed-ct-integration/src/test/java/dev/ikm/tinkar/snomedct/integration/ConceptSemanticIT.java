package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.coordinate.stamp.StampCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.StampPositionRecord;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.ConceptRecord;
import dev.ikm.tinkar.entity.ConceptVersionRecord;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConceptSemanticIT extends AbstractIntegrationTest {

    /**
     * Test Concepts Semantics.
     *
     * @result Reads content from file and validates Concept of Semantics by calling private method assertConcept().
     */
    @Test
    public void testConceptSemantics() throws IOException {
        String sourceFilePath = "../snomed-ct-origin/target/origin-sources";
        String errorFile = "target/failsafe-reports/concepts_not_found.txt";

        String absolutePath = findFilePath(sourceFilePath, "sct2_concept");
        int notFound = processFile(absolutePath, errorFile);

        assertEquals(0, notFound, "Unable to find " + notFound + " concepts. Details written to " + errorFile);
    }

    @Override
    protected boolean assertLine(String[] columns) {
        UUID id = uuid(columns[0]);
        long effectiveDate = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
        StateSet active = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;

        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveDate, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(active, stampPosition).stampCalculator();
        ConceptRecord entity = EntityService.get().getEntityFast(id);
        Latest<ConceptVersionRecord> latest = stampCalc.latest(entity);

        return latest.isPresent();
    }
}
