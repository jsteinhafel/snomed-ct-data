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
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DescriptionSemanticIT {

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

        Entity<EntityVersion> entity = EntityService.get().getEntityFast(regularNameDescriptionId);
        StampCalculator stampCalc = Calculators.Stamp.DevelopmentLatestActiveOnly();
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();

        AtomicBoolean matchFound = new AtomicBoolean(false);
        EntityService.get().forEachSemanticForComponentOfPattern(entity.nid(), TinkarTerm.DESCRIPTION_PATTERN.nid(), (descriptionSemantic) -> {

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
     *
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

    /**
     * Assert Description
     *
     * @param term
     * @param nameType
     * @param caseSensitive
     * @param effectiveDate
     * @param activeFlag
     */
    private void assertDescription(String term, EntityProxy.Concept nameType, EntityProxy.Concept caseSensitive, long effectiveDate, StateSet activeFlag) {
        // Given
        String actualTermFqn = "";
        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveDate, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(activeFlag, stampPosition).stampCalculator();

        // When
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();
        AtomicReference<String> actualFqn = new AtomicReference<>();
        EntityService.get().forEachSemanticOfPattern(latestDescriptionPattern.nid(), (semanticVersion) -> {
            Latest<SemanticEntityVersion> latestDescriptionSemantic = stampCalc.latest(semanticVersion);
            if (latestDescriptionSemantic.isPresent()) {
                Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latestDescriptionSemantic.get());
                if (PublicId.equals(descriptionType.publicId(), nameType)) {
                    String text = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.TEXT_FOR_DESCRIPTION, latestDescriptionSemantic.get());
                    if (text.contains(term)) {
                        actualFqn.set(text);
                    }
                }
            }
        });

    }

}
