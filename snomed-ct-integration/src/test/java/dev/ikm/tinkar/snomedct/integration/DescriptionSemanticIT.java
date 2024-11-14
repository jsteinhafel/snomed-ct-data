package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.component.Component;
import dev.ikm.tinkar.coordinate.Calculators;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticEntityVersion;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static dev.ikm.tinkar.terms.TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;



public class DescriptionSemanticIT {
private static final int BATCH_SIZE = 10;
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
    public void testDescriptionSemantics()throws IOException {
        String sourceFilePath ="C:\\Users\\patrichards\\Solor\\SnomedCT_InternationalRF2_PRODUCTION_20240201T120000Z\\Full\\Terminology\\sct2_Description_Full-en_INT_20240201.txt";

        try(BufferedReader br = new BufferedReader(new FileReader(sourceFilePath))){
            String line;
            while((line = br.readLine()) != null){
                if (line.startsWith("id")) continue;
                String[] columns = line.split("\\t");

                //pass these args in assertion method
                String effectiveTime =columns[1];
                String typeId = columns[6];
                String term =columns[7];
                String caseSignificanceId =columns[8];

                long effectiveTimeToLong=dateStringToEpochMillis(effectiveTime);
                boolean isRegularNameType= typeId.equals(REGULAR_NAME_DESCRIPTION_TYPE.publicId());

                boolean foundInDb = checkDescriptionInDatabase(term);
                assertTrue(foundInDb, "Description not found in Database: " + term);
            }
        }
    }


    public boolean checkDescriptionInDatabase(String expectedTerm) {
        StampCalculator stampCalc = Calculators.Stamp.DevelopmentLatestActiveOnly();
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();

        AtomicReference<Boolean> matchFound = new AtomicReference<>(false);
        EntityService.get().forEachSemanticOfPattern(latestDescriptionPattern.nid(), (semanticVersion) -> {
            Latest<SemanticEntityVersion> latestDescriptionSemantic = stampCalc.latest(semanticVersion);
            if (latestDescriptionSemantic.isPresent()) {
                latestDescriptionSemantic.get().fieldValues().forEach(field -> {
                    if (field instanceof String){
                        String actualTerm = (String) field;
                        if (actualTerm.contains(expectedTerm)){
                            matchFound.set(true);
                        }
                    }
                });
            }
        });
        return matchFound.get();
    }

    private long dateStringToEpochMillis(String dateString) {
        long epochMillis;
        try {
            epochMillis = new SimpleDateFormat("yyyyMMdd").parse(dateString).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return epochMillis;
    }

    @Test
    public void testSingleSynonym() {
        String expectedSynonym = "Tumor of pancreas";
        UUID regularNameDescriptionId = UUID.fromString("d7da4d59-8bdf-38cf-b863-c657c35a284e");

        Entity<EntityVersion> entity = EntityService.get().getEntityFast(regularNameDescriptionId);
        StampCalculator stampCalc = Calculators.Stamp.DevelopmentLatestActiveOnly();
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();

        AtomicBoolean matchFound = new AtomicBoolean(false);
        EntityService.get().forEachSemanticForComponentOfPattern(entity.nid(), TinkarTerm.DESCRIPTION_PATTERN.nid(), (descriptionSemantic) -> {

            Latest<SemanticEntityVersion> latestDescriptionSemantic = stampCalc.latest(descriptionSemantic);
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latestDescriptionSemantic.get());

            if (PublicId.equals(descriptionType.publicId(), REGULAR_NAME_DESCRIPTION_TYPE)) {
                String actualSynonym = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, latestDescriptionSemantic.get());
                if (actualSynonym.equals(expectedSynonym)) {
                    matchFound.set(true);
                }
            }
        });
        assertTrue(matchFound.get(), "No synonym found: " + expectedSynonym);
    }

    /**
     * Test FQN Term value.
     * @result Term is validated against a valid DataSet.
     */
    @Test
    public void testFQN() {
        // Given
        String expectedTermFqn = "Erythema gyratum repens (disorder)";
        String actualTermFqn = "";
        String actualUUID = "ba8a3539-31fa-307a-895c-37401f0aea78";

        UUID diseaseId = UUID.fromString(actualUUID);
        Entity<EntityVersion> entity = EntityService.get().getEntityFast(diseaseId);
        StampCalculator stampCalc = Calculators.Stamp.DevelopmentLatestActiveOnly();

        // When
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();
        AtomicReference<SemanticEntityVersion> fqnVersion = new AtomicReference<>();
        EntityService.get().forEachSemanticForComponentOfPattern(entity.nid(), TinkarTerm.DESCRIPTION_PATTERN.nid(), (descriptionSemantic) -> {
            Latest<SemanticEntityVersion> latestDescriptionSemantic = stampCalc.latest(descriptionSemantic);
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latestDescriptionSemantic.get());
            if (PublicId.equals(descriptionType.publicId(), TinkarTerm.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE)) {
                fqnVersion.set(latestDescriptionSemantic.get());
            }
        });

        actualTermFqn = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, fqnVersion.get());

        // Then
        assertEquals(expectedTermFqn, actualTermFqn, "Message: Assert Term Values");
    }

}
