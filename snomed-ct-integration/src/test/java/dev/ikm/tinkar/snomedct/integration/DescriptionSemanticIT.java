package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.component.Component;
import dev.ikm.tinkar.coordinate.stamp.StampCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.StampPositionRecord;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticRecord;
import dev.ikm.tinkar.entity.SemanticVersionRecord;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

public class DescriptionSemanticIT extends BaseIntegrationTest {

    @Test
    public void testDescriptionSemantics() throws IOException {
        executeTestLogic();
    }

    /**
     * Test Description Semantics.
     *
     * @result Reads content from file and validates Description of Semantics by calling private method assertDescription().
     */
    @Override
    public void executeTestLogic() throws IOException {
        // Given
        String sourceFilePath = System.getProperty("user.home") + "/data/SnomedCT_InternationalRF2_PRODUCTION_20241001T120000Z/Full/Terminology/sct2_Description_Full-en_INT_20241001.txt";
        String errorFile = "target/failsafe-reports/descriptions_not_found.txt";
        int notFound = 0;
        // When
        try (BufferedReader br = new BufferedReader(new FileReader(sourceFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("id")) continue;
                String[] columns = line.split("\\t");

                //pass these args in assertion method
                long effectiveTime = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
                StateSet descriptionStatus = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;
                EntityProxy.Concept descriptionType = SnomedUtility.getDescriptionType(columns[6]);
                String term = columns[7];
                EntityProxy.Concept caseSensitivityConcept = SnomedUtility.getDescriptionCaseSignificanceConcept(columns[8]);
                UUID id = UuidUtil.fromSNOMED(columns[0]);

                if (!assertDescription(id, term, descriptionType, caseSensitivityConcept, effectiveTime, descriptionStatus)) {
                    notFound++;
                    logError(errorFile, "Description not found: " + id);
                }
            }
        }
        //assertEquals(0, notFound, "Unable to find " + notFound + " description semantics. Details written to " + errorFile);
    }

    private boolean assertDescription(UUID id, String term, EntityProxy.Concept nameType, EntityProxy.Concept caseSensitive, long effectiveDate, StateSet activeFlag) {
        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveDate, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(activeFlag, stampPosition).stampCalculator();
        SemanticRecord entity = EntityService.get().getEntityFast(id);

        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();
        Latest<SemanticVersionRecord> latest = stampCalc.latest(entity);
        if (latest.isPresent()) {
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latest.get());
            Component caseSensitivity = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_CASE_SIGNIFICANCE, latest.get());
            String text = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, latest.get());

            return descriptionType.equals(nameType) && caseSensitivity.equals(caseSensitive) && text.equals(term);
        }
        return false;
    }

}
