package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.maven.SnomedUtility;
import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.coordinate.Calculators;
import dev.ikm.tinkar.coordinate.stamp.StampCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.StampPositionRecord;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.ConceptRecord;
import dev.ikm.tinkar.entity.ConceptVersionRecord;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.PublicIdentifierRecord;
import dev.ikm.tinkar.entity.SemanticRecord;
import dev.ikm.tinkar.entity.SemanticVersionRecord;
import dev.ikm.tinkar.terms.EntityFacade;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IdentifierSemanticIT extends AbstractIntegrationTest {
    /**
     * Test Snomed Loinc Identifier Semantics.
     *
     * @result Reads content from file and validates Identifier of Semantics by calling protected method assertLine().
     */
    @Test
    public void testIdentifierSemantics() throws IOException {
        String sourceFilePath = "../snomed-ct-loinc-origin/target/origin-sources";
        String errorFile = "target/failsafe-reports/identifiers_not_found.txt";

        String absolutePath = findFilePath(sourceFilePath, "sct2_identifier");
        int notFound = processFile(absolutePath, errorFile);

        assertEquals(0, notFound, "Unable to find " + notFound + " identifiers. Details written to " + errorFile);
    }

    @Override
    protected boolean assertLine(String[] columns) {
        long effectiveDate = SnomedUtility.snomedTimestampToEpochSeconds(columns[1]);
        StateSet active = Integer.parseInt(columns[2]) == 1 ? StateSet.ACTIVE : StateSet.INACTIVE;
        String identifierSchemeId = columns[4];
        UUID uuid = UuidUtil.fromSNOMED(identifierSchemeId);

        StampPositionRecord stampPosition = StampPositionRecord.make(effectiveDate, TinkarTerm.DEVELOPMENT_PATH.nid());
        StampCalculator stampCalc = StampCoordinateRecord.make(active, stampPosition).stampCalculator();
        ConceptRecord entity = EntityService.get().getEntityFast(uuid);
        Latest<EntityVersion> latest = stampCalc.latest((EntityFacade) entity);

        return latest.isPresent();
    }
}
