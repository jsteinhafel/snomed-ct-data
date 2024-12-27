package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.id.PublicId;
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

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefinitionSemanticIT extends AbstractIntegrationTest {

    /**
     * Test Definition Semantics.
     *
     * @result Reads content from file and validates Definition of Semantics by calling private method assertDefinition().
     */
    @Test
    public void testDefinitionSemantics() throws IOException {
        String sourceFilePath = "../snomed-ct-origin/target/origin-sources";
        //"/SnomedCT_InternationalRF2_PRODUCTION_20241201T120000Z/Full/Terminology/sct2_TextDefinition_Full-en_INT_20241201.txt";
        String errorFile = "target/failsafe-reports/descriptions_definitions_not_found.txt";

        String absolutePath = findFilePath(sourceFilePath, "uS", "definition");
        int notFound = processFile(absolutePath, errorFile);
        assertEquals(0, notFound, "Unable to find " + notFound + " description definition semantics. Details written to " + errorFile);

        log.info("We found file: " + absolutePath);
    }

    @Override
    protected boolean assertLine(String[] columns) {
        UUID id = uuid(columns[0]);
        long effectiveTime = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
        StateSet descriptionStatus = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;
        EntityProxy.Concept descType = SnomedUtility.getDescriptionType(columns[6]);
        String term = columns[7];
        EntityProxy.Concept caseSensitivityConcept = SnomedUtility.getDescriptionCaseSignificanceConcept(columns[8]);

        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveTime, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(descriptionStatus, stampPosition).stampCalculator();
        SemanticRecord entity = EntityService.get().getEntityFast(id);

        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();
        Latest<SemanticVersionRecord> latest = stampCalc.latest(entity);
        if (latest.isPresent()) {
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latest.get());
            Component caseSensitivity = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_CASE_SIGNIFICANCE, latest.get());
            String text = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, latest.get());
            if (PublicId.equals(descriptionType.publicId(), TinkarTerm.DEFINITION_DESCRIPTION_TYPE)) {
                return descriptionType.equals(descType) && caseSensitivity.equals(caseSensitivityConcept) && text.equals(term);
            }
        }
        return false;
    }

}


