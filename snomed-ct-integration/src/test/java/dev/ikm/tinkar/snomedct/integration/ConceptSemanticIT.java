package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.coordinate.stamp.StampCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.StampPositionRecord;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.ConceptRecord;
import dev.ikm.tinkar.entity.ConceptVersionRecord;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.terms.EntityProxy;
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

public class ConceptSemanticIT {
    @BeforeAll
    public static void setup() {
        CachingService.clearAll();
        File datastore = new File(System.getProperty("user.home") + "/Solor/September2024_ConnectathonDataset_v1");
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastore);
        PrimitiveData.selectControllerByName("Open SpinedArrayStore");
        PrimitiveData.start();
    }

    @AfterAll
    public static void shutdown() {
        PrimitiveData.stop();
    }

    /**
     * Test Concepts Semantics.
     *
     * @result Reads content from file and validates Concept of Semantics by calling private method assertConcept().
     */
    @Test
    public void testConceptSemantics() throws IOException {
        String sourceFilePath = System.getProperty("user.home") + "/data/SnomedCT_InternationalRF2_PRODUCTION_20241001T120000Z/Full/Terminology/sct2_Concept_Full_INT_20241001.txt";
        String errorFile = "target/failsafe-reports/concepts_not_found.txt";
        int notFound = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(sourceFilePath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(errorFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("id")) continue;
                String[] columns = line.split("\\t");

                //pass these args in assertion method
                UUID id = UuidUtil.fromSNOMED(columns[0]);
                long effectiveTime = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
                StateSet conceptStatus = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;
                EntityProxy.Concept moduleId = EntityProxy.Concept.make(PublicIds.of(UuidUtil.fromSNOMED(columns[3])));

                if (!assertConcept(id, effectiveTime, conceptStatus)) {
                    notFound++;
                    bw.write(id + "\t" + columns[0] +
                            "\t" + (conceptStatus.equals(StateSet.ACTIVE) ? "Active" : "Inactive"));
//                            "\t" + descriptionType.description() +
//                            "\t" + caseSensitivityConcept.description() + "\n");
                }

            }
        }
    }

    private boolean assertConcept(UUID id, long effectiveDate, StateSet activeFlag) {
        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveDate, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(activeFlag, stampPosition).stampCalculator();
        ConceptRecord entity = EntityService.get().getEntityFast(id);

        ConceptVersionRecord latestDescriptionPattern = (ConceptVersionRecord) stampCalc.latest(TinkarTerm.CONCEPT_TYPE).get();
        Latest<ConceptVersionRecord> latest = stampCalc.latest(entity);
        return latest.isPresent();
    }

}
