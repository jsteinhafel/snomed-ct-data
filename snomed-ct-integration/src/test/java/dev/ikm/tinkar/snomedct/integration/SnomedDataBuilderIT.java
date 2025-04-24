package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.coordinate.logic.LogicCoordinateRecord;
import dev.ikm.tinkar.coordinate.view.calculator.ViewCalculator;
import dev.ikm.tinkar.reasoner.elksnomed.ElkSnomedData;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnomedDataBuilderIT extends AbstractIntegrationTest{

    private static final Logger LOG = LoggerFactory.getLogger(SnomedDataBuilderIT.class);

    @Test
    public void statedPattern() throws Exception {
        ViewCalculator viewCalculator = getViewCalculator();
        LogicCoordinateRecord logicCoordinateRecord = viewCalculator.logicCalculator().logicCoordinateRecord();
        assertEquals(TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid(),
                logicCoordinateRecord.statedAxiomsPatternNid());
    }

    @Test
    public void count() throws Exception {
        ViewCalculator viewCalculator = getViewCalculator();
        AtomicInteger cnt = new AtomicInteger();
        AtomicInteger active_cnt = new AtomicInteger();
        AtomicInteger inactive_cnt = new AtomicInteger();

        // should we filter out the starter data? maybe look at if the concept has an SCTID ?
        viewCalculator.forEachSemanticVersionOfPatternParallel(TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid(),
                (semanticEntityVersion, patternEntityVersion) -> {
                    int conceptNid = semanticEntityVersion.referencedComponentNid();
                    if (viewCalculator.latestIsActive(conceptNid)) {
                        active_cnt.incrementAndGet();
                    } else {
                        inactive_cnt.incrementAndGet();
                    }
                    cnt.incrementAndGet();
                });
        LOG.info("Cnt: " + cnt.intValue());
        LOG.info("Active Cnt: " + active_cnt.intValue());
        LOG.info("Inactive Cnt: " + inactive_cnt.intValue());
        assertEquals(stated_count, cnt.intValue());
        assertEquals(active_count, active_cnt.intValue());
        assertEquals(inactive_count, inactive_cnt.intValue());
    }

    @Test
    public void build() throws Exception {
        ElkSnomedData data = buildSnomedData();
        assertEquals(active_count, data.getActiveConceptCount());
        assertEquals(inactive_count, data.getInactiveConceptCount());
        assertEquals(data.getReasonerConceptSet().size(), data.getConcepts().size());
        // TODO get these to work again
//		Files.createDirectories(getWritePath("concepts").getParent());
//		data.writeConcepts(getWritePath("concepts"));
//		data.writeRoleTypes(getWritePath("roles"));
//		compare("concepts");
//		compare("roles");
    }

    @Override
    protected boolean assertLine(String[] columns) {
        return false;
    }
}
