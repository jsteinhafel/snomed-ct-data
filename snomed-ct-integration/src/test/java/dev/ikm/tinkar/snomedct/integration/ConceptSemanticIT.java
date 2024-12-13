package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConceptSemanticIT extends BaseIntegrationTest {

    @Test
    protected void testConceptSemantics() throws Exception {
        executeTestLogic();
    }

    /**
     * Test Concepts Semantics.
     *
     * @result Reads content from file and validates Concept of Semantics by calling private method assertConcept().
     */
    @Override
    public void executeTestLogic() throws IOException {
        // Given
        String sourceFilePath = "../snomed-ct-origin/target/origin-sources/SnomedCT_ManagedServiceUS_PRODUCTION_US1000124_20240901T120000Z/Full/Terminology/sct2_Concept_Full_US1000124_20240901.txt";
        String errorFile = "target/failsafe-reports/concepts_not_found.txt";
        int notFound = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(sourceFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("id")) continue;
                String[] columns = line.split("\\t");

                //pass these args in assertion method
                UUID id = UuidT5Generator.get(UUID.fromString("3094dbd1-60cf-44a6-92e3-0bb32ca4d3de"), columns[0]); //Need hardcode ID on namespace for Snomed
                long effectiveTime = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
                StateSet conceptStatus = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;

                if (!assertConcept(id, effectiveTime, conceptStatus)) {
                    notFound++;
                    logError(errorFile, "Concepts not found: " + notFound);
                }
            }
        }
        assertEquals(0, notFound, "Unable to find " + notFound + " concepts. Details written to " + errorFile);
    }

    private boolean assertConcept(UUID id, long effectiveDate, StateSet activeFlag) {
        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveDate, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(activeFlag, stampPosition).stampCalculator();
        ConceptRecord entity = EntityService.get().getEntityFast(id);

        Latest<ConceptVersionRecord> latest = stampCalc.latest(entity);
        return latest.isPresent();
    }

}
