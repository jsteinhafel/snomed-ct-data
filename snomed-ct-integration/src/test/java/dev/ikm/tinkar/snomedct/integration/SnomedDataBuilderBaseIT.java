/*
 * Copyright © 2015 Integrated Knowledge Management (support@ikm.dev)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.elk.snomed.SnomedConcepts;
import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.coordinate.logic.LogicCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.view.calculator.ViewCalculator;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticEntityVersion;
import dev.ikm.tinkar.reasoner.elksnomed.ElkSnomedData;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnomedDataBuilderBaseIT extends AbstractIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(SnomedDataBuilderBaseIT.class);

    @Test
    public void statedPattern() throws Exception {
        ViewCalculator viewCalculator = PrimitiveDataTestUtil.getViewCalculator();
        LogicCoordinateRecord logicCoordinateRecord = viewCalculator.logicCalculator().logicCoordinateRecord();
        assertEquals(TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid(),
                logicCoordinateRecord.statedAxiomsPatternNid());
    }

    @Test
    public void count() throws Exception {
        String sourceFilePath = "../snomed-ct-origin/target/origin-sources";
        Path concepts_file = Paths.get(findFilePath(sourceFilePath, "sct2_Concept_Snapshot_INT_"));

        ViewCalculator viewCalculator = PrimitiveDataTestUtil.getViewCalculator();
        AtomicInteger cnt = new AtomicInteger();
        AtomicInteger active_cnt = new AtomicInteger();
        AtomicInteger inactive_cnt = new AtomicInteger();
        viewCalculator.forEachSemanticVersionOfPatternParallel(TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid(),
                (semanticEntityVersion, _) -> {
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
        SnomedConcepts snomed_concepts = SnomedConcepts.init(concepts_file);
        LOG.info("Snomed active cnt: " + snomed_concepts.getActiveCount());
        LOG.info("Snomed inactive cnt: " + snomed_concepts.getInactiveCount());
        int primordial_cnt = getPrimordialCount();
        LOG.info("Primordial Cnt: " + primordial_cnt);
        int primordial_sctid_cnt = getPrimordialSctidCount();
        assertEquals(snomed_concepts.getActiveCount(), active_cnt.intValue() - primordial_cnt + primordial_sctid_cnt);
        // TODO why is the inactive count so low
//		assertEquals(snomed_concepts.getInactiveCount(), inactive_cnt.intValue());
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

    public int getPrimordialCount() throws Exception {
        ViewCalculator primordial_vc = PrimitiveDataTestUtil.getViewCalculatorPrimordial();
        AtomicInteger cnt = new AtomicInteger();
        AtomicInteger active_cnt = new AtomicInteger();
        AtomicInteger inactive_cnt = new AtomicInteger();
        primordial_vc.forEachSemanticVersionOfPattern(TinkarTerm.IDENTIFIER_PATTERN.nid(),
                (semanticEntityVersion, _) -> {
                    int conceptNid = semanticEntityVersion.referencedComponentNid();
                    if (primordial_vc.latestIsActive(conceptNid)) {
                        active_cnt.incrementAndGet();
                    } else {
                        inactive_cnt.incrementAndGet();
                    }
                    cnt.incrementAndGet();
                });
        LOG.info("Primordial:");
        LOG.info("\tCnt: " + cnt.intValue());
        LOG.info("\tActive Cnt: " + active_cnt.intValue());
        LOG.info("\tInactive Cnt: " + inactive_cnt.intValue());
        assertEquals(0, inactive_cnt.intValue());
        return cnt.intValue();
    }

    public int getPrimordialSctidCount() throws Exception {
        ViewCalculator primordial_vc = PrimitiveDataTestUtil.getViewCalculatorPrimordial();
        AtomicInteger cnt = new AtomicInteger();
        primordial_vc.forEachSemanticVersionOfPattern(TinkarTerm.IDENTIFIER_PATTERN.nid(),
                (semanticEntityVersion, _) -> {
                    int conceptNid = semanticEntityVersion.referencedComponentNid();
                    ViewCalculator vc = PrimitiveDataTestUtil.getViewCalculator();
                    Latest<PatternEntityVersion> latestIdPattern = vc
                            .latestPatternEntityVersion(TinkarTerm.IDENTIFIER_PATTERN);
                    EntityService.get().forEachSemanticForComponentOfPattern(conceptNid,
                            TinkarTerm.IDENTIFIER_PATTERN.nid(), (semanticEntity) -> {
                                if (vc.latest(semanticEntity).isPresent()) {
                                    SemanticEntityVersion latestSemanticVersion = vc.latest(semanticEntity).get();
                                    EntityProxy identifierSource = latestIdPattern.get()
                                            .getFieldWithMeaning(TinkarTerm.IDENTIFIER_SOURCE, latestSemanticVersion);
                                    boolean has_sctid = false;
                                    if (PublicId.equals(identifierSource, TinkarTerm.SCTID)) {
                                        // Just in case it has more than one sctid
                                        if (!has_sctid)
                                            cnt.incrementAndGet();
                                        has_sctid = true;
                                        String idSourceName = vc
                                                .getPreferredDescriptionTextWithFallbackOrNid(identifierSource);
                                        String idValue = latestIdPattern.get().getFieldWithMeaning(
                                                TinkarTerm.IDENTIFIER_VALUE, latestSemanticVersion);
                                        LOG.info("Primordial: " + conceptNid + " " + PrimitiveData.text(conceptNid));
                                        LOG.info("ID: " + idSourceName + " " + idValue);
                                    }
                                } else {
                                    throw new RuntimeException(
                                            "No latest for " + conceptNid + " " + PrimitiveData.text(conceptNid));
                                }
                            });
                });
        return cnt.intValue();
    }

    @Override
    protected boolean assertLine(String[] columns) {
        return false;
    }

}
