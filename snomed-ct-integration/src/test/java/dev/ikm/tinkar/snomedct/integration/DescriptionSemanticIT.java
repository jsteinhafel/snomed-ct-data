package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.maven.SnomedUtility;
import dev.ikm.tinkar.component.Component;
import dev.ikm.tinkar.coordinate.Calculators;
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

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DescriptionSemanticIT extends AbstractIntegrationTest {

    /**
     * Test Description Semantics.
     *
     * @result Reads content from file and validates Description of Semantics by calling private method assertDescription().
     */
    @Test
    public void testDescriptionSemantics() throws IOException {
        String baseDir = "../snomed-ct-origin/target/origin-sources";
        String errorFile = "target/failsafe-reports/descriptions_not_found.txt";

        String absolutePath = findFilePath(baseDir, "sct2_description");
        int notFound = processFile(absolutePath, errorFile);

        assertEquals(0, notFound, "Unable to find " + notFound + " description semantics. Details written to " + errorFile);
    }


    @Override
    protected boolean assertLine(String[] columns) {
        long effectiveTime = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
        StateSet descriptionStatus = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;
        EntityProxy.Concept descrType = SnomedUtility.getDescriptionType(columns[6]);
        String term = columns[7];
        EntityProxy.Concept caseSensitivityConcept = SnomedUtility.getDescriptionCaseSignificanceConcept(columns[8]);
        UUID id = uuid(columns[0]);


        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveTime, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(descriptionStatus, stampPosition).stampCalculator();
        SemanticRecord entity = EntityService.get().getEntityFast(id);

        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) Calculators.Stamp.DevelopmentLatest().latest(TinkarTerm.DESCRIPTION_PATTERN).get();
        Latest<SemanticVersionRecord> latest = stampCalc.latest(entity);
        if (latest.isPresent()) {
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latest.get());
            Component descCaseSensitivity = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_CASE_SIGNIFICANCE, latest.get());
            String text = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, latest.get());

            return descriptionType.equals(descrType) && descCaseSensitivity.equals(caseSensitivityConcept) && text.equals(term);
        }
        return false;
    }
}
