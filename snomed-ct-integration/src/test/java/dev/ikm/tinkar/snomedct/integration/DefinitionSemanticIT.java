package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.component.Component;
import dev.ikm.tinkar.coordinate.Calculators;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticEntityVersion;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefinitionSemanticIT {

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

    @Test
    public void testDefinitionSemantics() throws IOException {
        String sourceFilePath = System.getProperty("user.home") + "/Solor/SnomedCT_InternationalRF2_PRODUCTION_20240201T120000Z/Full/Terminology/sct2_TextDefinition_Full-en_INT_20240201.txt";

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

            }
        }
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
}


