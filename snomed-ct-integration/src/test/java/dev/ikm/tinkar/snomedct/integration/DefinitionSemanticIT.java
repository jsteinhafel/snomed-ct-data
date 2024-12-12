package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.component.Component;
import dev.ikm.tinkar.coordinate.Calculators;
import dev.ikm.tinkar.coordinate.stamp.StampCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.StampPositionRecord;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticEntityVersion;
import dev.ikm.tinkar.entity.SemanticRecord;
import dev.ikm.tinkar.entity.SemanticVersionRecord;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefinitionSemanticIT extends BaseIntegrationTest {

    @Test
    public void testDefinitionSemantics() throws IOException {
        executeTestLogic();
    }

    /**
     * Test Definition Semantics.
     *
     * @result Reads content from file and validates Definition of Semantics by calling private method assertDefinition().
     */
    @Override
    public void executeTestLogic() throws IOException {
        // Given
        String sourceFilePath = System.getProperty("user.home") + "/data/SnomedCT_InternationalRF2_PRODUCTION_20241001T120000Z/Full/Terminology/sct2_TextDefinition_Full-en_INT_20241001.txt";
        String errorFile = "target/failsafe-reports/descriptions_definitions_not_found.txt";
        int notFound = 0;
        // When
        try (BufferedReader br = new BufferedReader(new FileReader(sourceFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("id")) continue;
                String[] columns = line.split("\\t");

                //pass these args in assertion method
                UUID id = UuidUtil.fromSNOMED(columns[0]);
                long effectiveTime = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
                StateSet descriptionStatus = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;
                EntityProxy.Concept moduleId = EntityProxy.Concept.make(PublicIds.of(UuidUtil.fromSNOMED(columns[3])));
                PublicId publicId = PublicIds.of(UuidUtil.fromSNOMED(columns[4]));
                EntityProxy.Concept languageType = SnomedUtility.getLanguageConcept(columns[5]);
                EntityProxy.Concept descriptionType = SnomedUtility.getDescriptionType(columns[6]);
                String term = columns[7];
                EntityProxy.Concept caseSensitivityConcept = SnomedUtility.getDescriptionCaseSignificanceConcept(columns[8]);

                if (!assertDefinition(id, term, descriptionType, caseSensitivityConcept, effectiveTime, descriptionStatus)) {
                    notFound++;
                    logError(errorFile, "Definition not found: " + id);
                }
            }
        }
        //assertEquals(0, notFound, "Unable to find " + notFound + " description definition semantics. Details written to " + errorFile);
    }

    /**
     * Test Definition Term value.
     *
     * @result Term is validated against a valid DataSet.
     */
    @Test
    public void testDefinition() {
        // Given
        String expectedTerm = "Domestic goat";
        String actualTerm = "";
        String actualUUID = "6e4469d1-c972-3031-9455-c18e7b377fbd";

        UUID diseaseId = UUID.fromString(actualUUID);
        Entity<EntityVersion> entity = EntityService.get().getEntityFast(diseaseId);
        StampCalculator stampCalc = Calculators.Stamp.DevelopmentLatestActiveOnly();

        // When
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();
        AtomicReference<SemanticEntityVersion> matchFound = new AtomicReference<>();
        EntityService.get().forEachSemanticForComponentOfPattern(entity.nid(), TinkarTerm.DESCRIPTION_PATTERN.nid(), (descriptionSemantic) -> {
            Latest<SemanticEntityVersion> latestDescriptionSemantic = stampCalc.latest(descriptionSemantic);
            Component definitionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latestDescriptionSemantic.get());
            if (PublicId.equals(definitionType.publicId(), TinkarTerm.DEFINITION_DESCRIPTION_TYPE)) {
                matchFound.set(latestDescriptionSemantic.get());
            }
        });

        actualTerm = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, matchFound.get());

        // Then
        assertEquals(expectedTerm, actualTerm, "Message: Assert Term Values");
    }

    private boolean assertDefinition(UUID id, String term, EntityProxy.Concept nameType, EntityProxy.Concept caseSensitive, long effectiveDate, StateSet activeFlag) {
        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveDate, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(activeFlag, stampPosition).stampCalculator();
        SemanticRecord entity = EntityService.get().getEntityFast(id);

        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();
        Latest<SemanticVersionRecord> latest = stampCalc.latest(entity);
        if (latest.isPresent()) {
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latest.get());
            Component caseSensitivity = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_CASE_SIGNIFICANCE, latest.get());
            String text = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, latest.get());

            if (PublicId.equals(descriptionType.publicId(), TinkarTerm.DEFINITION_DESCRIPTION_TYPE)) {
                return descriptionType.equals(nameType) && caseSensitivity.equals(caseSensitive) && text.equals(term);
            }

        }
        return false;
    }

}


