package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnomedAxiomSemanticIT {

    @BeforeAll
    public static void setup() {
        CachingService.clearAll();
        File datastore = new File(System.getProperty("user.home") + "/Solor/generated-data"); //Note. Dataset needed to be generated within repo, with command 'mvn clean install'
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastore);
        PrimitiveData.selectControllerByName("Open SpinedArrayStore");
        PrimitiveData.start();
    }

    @AfterAll
    public static void shutdown() {
        PrimitiveData.stop();
    }

    /**
     * Test Snomed Axiom Semantics.
     *
     * @result Reads content from file and validates Snomed Axiom of Semantics by calling private method assertSnomedAxioms().
     */
    @Test
    public void testSnomedAxiomSemantics() throws IOException {
        // Given
        String sourceFilePath = System.getProperty("user.home") + "/data/SnomedCT_InternationalRF2_PRODUCTION_20241001T120000Z/Full/Terminology/sct2_sRefset_OWLExpressionFull_INT_20241001.txt";
        String errorFile = "target/failsafe-reports/snomedaxioms_not_found.txt";
        int notFound = 0;
        // When
        try (BufferedReader br = new BufferedReader(new FileReader(sourceFilePath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(errorFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("id")) continue;
                String[] columns = line.split("\\t");

                //pass these args in assertion method
                long effectiveTime = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
                StateSet snomedAxiomStatus = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;
                String owlAxiomStr = SnomedUtility.owlAxiomIdsToPublicIds(columns[6]);
                UUID id = UuidT5Generator.get(UUID.fromString("3094dbd1-60cf-44a6-92e3-0bb32ca4d3de"), columns[0]); //Need hardcode ID on namespace for Snomed

                if (!assertSnomedAxioms(id, effectiveTime, snomedAxiomStatus, owlAxiomStr)) {
                    notFound++;
                    bw.write(id + "\t" + columns[1] +
                            "\t" + (snomedAxiomStatus.equals(StateSet.ACTIVE) ? "Active" : "Inactive") +
                            "\t" + owlAxiomStr + "\n");
                }
            }
        }
        assertEquals(0, notFound, "Unable to find " + notFound + " snomed axiom semantics. Details written to " + errorFile);
    }

    public boolean assertSnomedAxioms(UUID id, long effectiveDate, StateSet active, String axiomStr) {
        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveDate, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(active, stampPosition).stampCalculator();
        SemanticRecord entity = EntityService.get().getEntityFast(id);

        if (entity != null) {
            PatternEntityVersion pattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.OWL_AXIOM_SYNTAX_PATTERN).get();
            Latest<SemanticVersionRecord> latest = stampCalc.latest(entity);
            String fieldValue = pattern.getFieldWithMeaning(TinkarTerm.AXIOM_SYNTAX, latest.get());
            return latest.isPresent() && fieldValue.equals(axiomStr);
        }

        return false;
    }

}
