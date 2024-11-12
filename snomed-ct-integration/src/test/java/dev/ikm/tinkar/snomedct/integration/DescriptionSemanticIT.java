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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        String expectedSynonym = "Neoplasm of lesser curvature of stomach";
        UUID regularNameDescriptionId = UUID.fromString("3cf8a41e-6f46-306b-a5d5-2d49c42c048b");


        Entity<EntityVersion> cldEntity = EntityService.get().getEntityFast(regularNameDescriptionId);
        StampCalculator stampCalc = Calculators.Stamp.DevelopmentLatestActiveOnly();
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();

        AtomicReference<SemanticEntityVersion> synonymVersion = new AtomicReference<>();
        AtomicBoolean matchFound = new AtomicBoolean(false);
        EntityService.get().forEachSemanticForComponentOfPattern(cldEntity.nid(), TinkarTerm.DESCRIPTION_PATTERN.nid(), (descriptionSemantic) -> {

            Latest<SemanticEntityVersion> latestDescriptionSemantic = stampCalc.latest(descriptionSemantic);
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latestDescriptionSemantic.get());

            if (PublicId.equals(descriptionType.publicId(), TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE)) {
                synonymVersion.set(latestDescriptionSemantic.get());
                String actualSynonym=synonymVersion.get().fieldValues().get(1).toString();
                if (matchFound.get()){
                    return;
                }
                if (actualSynonym.equals(expectedSynonym)) {
                    matchFound.set(true);
                    assertTrue(true, "found a match for " + expectedSynonym);
                } else {
                    fail("no match found, we found: " + actualSynonym);
                }
            }
        });
    }
}
