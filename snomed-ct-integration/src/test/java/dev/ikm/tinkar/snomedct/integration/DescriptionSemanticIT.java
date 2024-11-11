package dev.ikm.tinkar.snomedct.integration;


import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.component.Component;
import dev.ikm.tinkar.component.PatternVersion;
import dev.ikm.tinkar.coordinate.Calculators;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.ConceptEntity;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.entity.Field;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticEntityVersion;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DescriptionSemanticIT {
    private static final Logger LOG = LoggerFactory.getLogger(DescriptionSemanticIT.class);

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
        String expectedSynonym = "Neoplasm of pancreas";
        final String[] descriptionSemanticString = new String[1];
        UUID regularNameDescriptionId = UUID.fromString("d7da4d59-8bdf-38cf-b863-c657c35a284e");


        Entity<EntityVersion> cldEntity = EntityService.get().getEntityFast(regularNameDescriptionId);

        StampCalculator stampCalc = Calculators.Stamp.DevelopmentLatestActiveOnly();
        PatternEntityVersion latestDescriptionPattern = (PatternEntityVersion) stampCalc.latest(TinkarTerm.DESCRIPTION_PATTERN).get();

        AtomicReference<SemanticEntityVersion> synonymVersion = new AtomicReference<>();
        EntityService.get().forEachSemanticForComponentOfPattern(cldEntity.nid(), TinkarTerm.DESCRIPTION_PATTERN.nid(), (descriptionSemantic) -> {
            descriptionSemanticString[0] =descriptionSemantic.toString();
            Latest<SemanticEntityVersion> latestDescriptionSemantic = stampCalc.latest(descriptionSemantic);
            Component descriptionType = latestDescriptionPattern.getFieldWithMeaning(TinkarTerm.DESCRIPTION_TYPE, latestDescriptionSemantic.get());
            if (PublicId.equals(descriptionType.publicId(), TinkarTerm.REGULAR_NAME_DESCRIPTION_TYPE)) {
                synonymVersion.set(latestDescriptionSemantic.get());
            }
        });
        boolean matchFound = false;
        if (descriptionSemanticString[0].contains(expectedSynonym)){
            matchFound = true;
            assertTrue(matchFound, "found a match for "+expectedSynonym);
        }else {
            fail("no match found, we found: " + descriptionSemanticString[0]);
        }


    }
}
