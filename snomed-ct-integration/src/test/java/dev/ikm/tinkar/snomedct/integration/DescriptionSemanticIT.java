package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
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
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertTrue;



public class DescriptionSemanticIT {

    /**
     * This method converts dateString into milliseconds
     *
     * @param dateString
     * @return Long value of dateString.
     */
    private long dateStringToEpochMillis(String dateString) {
        long epochMillis;
        try {
            epochMillis = new SimpleDateFormat("yyyyMMdd").parse(dateString).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return epochMillis;
    }

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
    public void testSingleSynonym() {
        String expectedSynonym = "Tumor of pancreas";
        UUID regularNameDescriptionId = UUID.fromString("d7da4d59-8bdf-38cf-b863-c657c35a284e");


        Entity<EntityVersion> cldEntity = EntityService.get().getEntityFast(regularNameDescriptionId);
        StampCalculator stampCalc = Calculators.Stamp.DevelopmentLatestActiveOnly();
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();

        AtomicBoolean matchFound = new AtomicBoolean(false);
        EntityService.get().forEachSemanticForComponentOfPattern(cldEntity.nid(), TinkarTerm.DESCRIPTION_PATTERN.nid(), (descriptionSemantic) -> {

            Latest<SemanticEntityVersion> latestDescriptionSemantic = stampCalc.latest(descriptionSemantic);
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latestDescriptionSemantic.get());

            if (PublicId.equals(descriptionType.publicId(), TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE)) {
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
     * @result Term is validated agains a valid DataSet,
     *
     */
    @Test
    public void testFQN() {
        // Given
        String expectedTermFqn = "Erythema gyratum repens (disorder)";
        String actualTermFqn = "";
        String actualUUID = "ba8a3539-31fa-307a-895c-37401f0aea78"; //SNOMED CT integer ID: 77300003

        UUID diseaseId = UUID.fromString(actualUUID);
        Entity<EntityVersion> cldEntity = EntityService.get().getEntityFast(diseaseId);

        long timestamp = dateStringToEpochMillis("20250931");
        StampPositionRecord stampPosition = StampPositionRecord.make(timestamp, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(StateSet.ACTIVE, stampPosition).stampCalculator();

        // When
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();

        AtomicReference<SemanticEntityVersion> fqnVersion = new AtomicReference<>();
        EntityService.get().forEachSemanticForComponentOfPattern(cldEntity.nid(), TinkarTerm.DESCRIPTION_PATTERN.nid(), (descriptionSemantic) -> {
            Latest<SemanticEntityVersion> latestDescriptionSemantic = stampCalc.latest(descriptionSemantic);
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latestDescriptionSemantic.get());
            if (PublicId.equals(descriptionType.publicId(), TinkarTerm.FULLY_QUALIFIED_NAME_DESCRIPTION_TYPE)) {
                fqnVersion.set(latestDescriptionSemantic.get());
            }
        });

        actualTermFqn = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, fqnVersion.get());
//        System.out.println("actualTermFqn: " + actualTermFqn);

        // Then
        assertEquals(expectedTermFqn, actualTermFqn, "Message: Assert Term Values");
    }

}
