package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.coordinate.stamp.StampCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.StampPositionRecord;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticRecord;
import dev.ikm.tinkar.entity.SemanticVersionRecord;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AxiomSemanticIT extends AbstractIntegrationTest {

    /**
     * Test Snomed Axiom Semantics.
     *
     * @result Reads content from file and validates Snomed Axiom of Semantics by calling private method assertSnomedAxioms().
     */
    @Test
    public void testAxiomSemantics() throws IOException {
        String baseDir = "../snomed-ct-origin/target/origin-sources";
        String errorFile = "target/failsafe-reports/axioms_not_found.txt";

        String absolutePath = findFilePath(baseDir, "sct2_srefset_owl");
        int notFound = processFile(absolutePath, errorFile);

        assertEquals(0, notFound, "Unable to find " + notFound + " snomed axiom semantics. Details written to " + errorFile);
    }

    @Override
    protected boolean assertLine(String[] columns) {
        long effectiveTime = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
        StateSet snomedAxiomStatus = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;
        String owlAxiomStr = SnomedUtility.owlAxiomIdsToPublicIds(columns[6]);
        UUID id = uuid(columns[0]);

        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveTime, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(snomedAxiomStatus, stampPosition).stampCalculator();
        SemanticRecord entity = EntityService.get().getEntityFast(id);

        if (entity != null) {
            PatternEntityVersion pattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.OWL_AXIOM_SYNTAX_PATTERN).get();
            Latest<SemanticVersionRecord> latest = stampCalc.latest(entity);
            String fieldValue = pattern.getFieldWithMeaning(TinkarTerm.AXIOM_SYNTAX, latest.get());
            return latest.isPresent() && fieldValue.equals(owlAxiomStr);
        }
        return false;
    }

}
