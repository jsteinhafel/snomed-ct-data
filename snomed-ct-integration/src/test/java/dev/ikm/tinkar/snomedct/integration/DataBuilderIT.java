package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.elk.snomed.OwlElTransformer;
import dev.ikm.elk.snomed.SnomedDescriptions;
import dev.ikm.elk.snomed.SnomedIds;
import dev.ikm.elk.snomed.SnomedIsa;
import dev.ikm.elk.snomed.SnomedOntology;
import dev.ikm.elk.snomed.SnomedOntologyReasoner;
import dev.ikm.elk.snomed.model.Concept;
import dev.ikm.elk.snomed.model.ConcreteRoleType;
import dev.ikm.elk.snomed.model.Definition;
import dev.ikm.elk.snomed.model.DefinitionType;
import dev.ikm.elk.snomed.model.RoleType;
import dev.ikm.elk.snomed.owlel.OwlElOntology;
import dev.ikm.tinkar.common.service.PluggableService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.coordinate.Coordinates;
import dev.ikm.tinkar.coordinate.logic.LogicCoordinateRecord;
import dev.ikm.tinkar.coordinate.view.ViewCoordinateRecord;
import dev.ikm.tinkar.coordinate.view.calculator.ViewCalculator;
import dev.ikm.tinkar.coordinate.view.calculator.ViewCalculatorWithCache;
import dev.ikm.tinkar.reasoner.elksnomed.ElkSnomedData;
import dev.ikm.tinkar.reasoner.elksnomed.ElkSnomedDataBuilder;
import dev.ikm.tinkar.reasoner.elksnomed.ElkSnomedReasonerService;
import dev.ikm.tinkar.reasoner.service.ReasonerService;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class DataBuilderIT extends AbstractIntegrationTest{

    private static final Logger LOG = LoggerFactory.getLogger(DataBuilderIT.class);

    @Test
    public void builderTests() throws Exception {
        statedPattern();
        count();
        build();
    }

    @Test
    public void compareTests() throws Exception {

        String sourceFilePath = "../snomed-ct-origin/target/origin-sources";
        Path axioms_file = Paths.get(findFilePath(sourceFilePath, "sct2_sRefset_OWLExpressionSnapshot_"));
        Path rels_file = Paths.get(findFilePath(sourceFilePath, "sct2_Relationship_Snapshot_"));
        String descriptionFile = findFilePath(sourceFilePath, "sct2_Description_Snapshot-en");
        Path descriptions_file = Paths.get(descriptionFile);

        final String edition;
        if (descriptionFile.contains("en_US")) {
            edition = "US";
        } else {
            edition = "INT";
        }

        assumeTrue(Files.exists(axioms_file), "No file: " + axioms_file);
        assumeTrue(Files.exists(rels_file), "No file: " + rels_file);
        LOG.info("Files exist");
        LOG.info("\t" + axioms_file);
        LOG.info("\t" + rels_file);

        ElkSnomedData data = buildSnomedData();

        {
            Concept us_con = data.getConcept(ElkSnomedData.getNid(SnomedIds.us_nlm_module));
            if (edition.equals("US")) {
                assertNotNull(us_con);
            } else {
                assertNull(us_con);
            }
        }

        OwlElOntology ontology = new OwlElOntology();
        ontology.load(axioms_file);
        SnomedOntology snomedOntology = new OwlElTransformer().transform(ontology);
        snomedOntology.setDescriptions(SnomedDescriptions.init(descriptions_file));
        {
            Concept us_con = snomedOntology.getConcept(SnomedIds.us_nlm_module);
            if (edition.equals("US")) {
                assertNotNull(us_con);
            } else {
                assertNull(us_con);
            }
        }

        for (RoleType role : snomedOntology.getRoleTypes()) {
            role.setName(snomedOntology.getFsn(role.getId()));
            int nid = ElkSnomedData.getNid(role.getId());
            RoleType data_role = data.getRoleType(nid);
            if (data_role == null)
                continue;
            data_role.setId(role.getId());
            data_role.setName(role.getName());
        }

        for (ConcreteRoleType role : snomedOntology.getConcreteRoleTypes()) {
            role.setName(snomedOntology.getFsn(role.getId()));
            int nid = ElkSnomedData.getNid(role.getId());
            ConcreteRoleType data_role = data.getConcreteRoleType(nid);
            if (data_role == null)
                continue;
            data_role.setId(role.getId());
            data_role.setName(role.getName());
        }

        for (Concept con : snomedOntology.getConcepts()) {
            con.setName(snomedOntology.getFsn(con.getId()));
            int nid = ElkSnomedData.getNid(con.getId());
            Concept data_con = data.getConcept(nid);
            if (data_con == null)
                continue;
            data_con.setId(con.getId());
            data_con.setName(con.getName());
        }
        int missing_role_cnt = 0;
        int missing_concrete_role_cnt = 0;
        int missing_concept_cnt = 0;
        int compare_role_cnt = 0;
        int compare_concrete_role_cnt = 0;
        int compare_concept_cnt = 0;
        for (RoleType role : snomedOntology.getRoleTypes()) {
            int nid = ElkSnomedData.getNid(role.getId());
            RoleType data_role = data.getRoleType(nid);
            if (data_role == null) {
                LOG.error("No role: " + role);
                missing_role_cnt++;
                continue;
            }
            // Role group is in tinkar starter data as a concept
            // Concept model data attribute is a concept but has a definition
//			if (data.getConcept(nid) != null)
//				LOG.warn("Role type is also a concept: " + role + " " + data.getConcept(nid));
            if (!compare(role, data_role, snomedOntology))
                compare_role_cnt++;
        }
        for (RoleType data_role : data.getRoleTypes()) {
            RoleType role = snomedOntology.getRoleType(data_role.getId());
            if (role == null) {
                LOG.error("Extra role: " + data_role);
                continue;
            }
        }
        for (ConcreteRoleType role : snomedOntology.getConcreteRoleTypes()) {
            int nid = ElkSnomedData.getNid(role.getId());
            ConcreteRoleType data_role = data.getConcreteRoleType(nid);
            if (data_role == null) {
                LOG.error("No role: " + role);
                missing_concrete_role_cnt++;
                continue;
            }
            // Concept model object attribute is a concept but has a definition
//			if (data.getConcept(nid) != null)
//				LOG.warn("Concrete role type is also a concept: " + role + " " + data.getConcept(nid));
            if (!compare(role, data_role, snomedOntology))
                compare_concrete_role_cnt++;
        }
        for (ConcreteRoleType data_role : data.getConcreteRoleTypes()) {
            ConcreteRoleType role = snomedOntology.getConcreteRoleType(data_role.getId());
            if (role == null) {
                LOG.error("Extra concrete role: " + data_role);
                continue;
            }
        }

        for (Concept con : snomedOntology.getConcepts()) {
            int nid = ElkSnomedData.getNid(con.getId());
            Concept data_con = data.getConcept(nid);
            if (data_con == null) {
                LOG.error("No concept: " + con);
                missing_concept_cnt++;
                continue;
            }
            // This merges multiple subConcept definition into one
            List<Definition> sc_defs = con.getDefinitions().stream()
                    .filter(def -> def.getDefinitionType() == DefinitionType.SubConcept).toList();
            if (sc_defs.size() > 1) {
                Definition merged_def = new Definition();
                merged_def.setDefinitionType(DefinitionType.SubConcept);
                sc_defs.forEach(sc_def -> {
                    sc_def.getSuperConcepts().forEach(merged_def::addSuperConcept);
                    sc_def.getUngroupedRoles().forEach(merged_def::addUngroupedRole);
                    sc_def.getUngroupedConcreteRoles().forEach(merged_def::addUngroupedConcreteRole);
                    sc_def.getRoleGroups().forEach(merged_def::addRoleGroup);
                });
                con.getDefinitions().removeAll(sc_defs);
                con.addDefinition(merged_def);
            }
            if (!compare(con, data_con, snomedOntology) && con.getId() != SnomedIds.root)
                compare_concept_cnt++;
        }

        assertEquals(0, missing_role_cnt);
        assertEquals(0, missing_concrete_role_cnt);
        assertEquals(0, missing_concept_cnt);
        assertEquals(0, compare_role_cnt);
        assertEquals(0, compare_concrete_role_cnt);
        assertEquals(0, compare_concept_cnt);
    }
    @Test
    public void supercs() throws Exception {
        ArrayList<String> lines = runSnomedReasoner();
        assertEquals(expected_supercs_cnt, lines.size());
    }

    @Test
    public void supercsService() throws Exception {
        ArrayList<String> lines = runSnomedReasonerService();
        assertEquals(expected_supercs_cnt, lines.size());
    }

    @Test
    public void nnfService() throws Exception {
        ReasonerService rs = runReasonerServiceNNF();
        rs.writeInferredResults();
    }

    private HashMap<Integer, Long> nid_sctid_map;

    private Set<Long> toSctids(Set<Long> nids) {
        return nids.stream().map(x -> nid_sctid_map.get(x.intValue())).collect(Collectors.toSet());
    }

    @Test
    public void isas() throws Exception {
        String sourceFilePath = "../snomed-ct-origin/target/origin-sources";
        Path axioms_file = Paths.get(findFilePath(sourceFilePath, "sct2_sRefset_OWLExpressionSnapshot_"));
        Path rels_file = Paths.get(findFilePath(sourceFilePath, "sct2_Relationship_Snapshot_"));
        String descriptionFile = findFilePath(sourceFilePath, "sct2_Description_Snapshot-en");
        Path descriptions_file = Paths.get(descriptionFile);

        LOG.info("runSnomedReasoner");
        ElkSnomedData data = buildSnomedData();
        LOG.info("Create ontology");
        SnomedOntology ontology = new SnomedOntology(data.getConcepts(), data.getRoleTypes(), List.of());
        LOG.info("Create reasoner");
        SnomedOntologyReasoner reasoner = SnomedOntologyReasoner.create(ontology);
        TreeSet<Long> misses = new TreeSet<>();
        TreeSet<Long> other_misses = new TreeSet<>();
        int non_snomed_cnt = 0;
        int miss_cnt = 0;
        int pharma_miss_cnt = 0;
        int other_miss_cnt = 0;
        SnomedIsa isas = SnomedIsa.init(rels_file);
        SnomedDescriptions descr = SnomedDescriptions.init(descriptions_file);
        nid_sctid_map = new HashMap<>();
        for (long sctid : isas.getOrderedConcepts()) {
            int nid = ElkSnomedData.getNid(sctid);
            nid_sctid_map.put(nid, sctid);
            if (ontology.getConcept(nid) == null)
                LOG.info("No concept for: " + sctid + " " + descr.getFsn(sctid));
        }
        for (Concept con : ontology.getConcepts()) {
            long nid = con.getId();
            Set<Long> sups = toSctids(reasoner.getSuperConcepts(nid));
            Long sctid = nid_sctid_map.get((int) nid);
            if (sctid == null) {
                non_snomed_cnt++;
                continue;
            }
            Set<Long> parents = isas.getParents(sctid);
            if (sctid == SnomedIds.root) {
                assertTrue(parents.isEmpty());
                // has a parent in the db
                assertEquals(1, sups.size());
                assertEquals(TinkarTerm.ROOT_VERTEX.nid(), reasoner.getSuperConcepts(nid).iterator().next());
                continue;
            } else {
                assertNotNull(parents);
            }
            if (!parents.equals(sups)) {
                misses.add(sctid);
                miss_cnt++;
                if (isas.hasAncestor(sctid, 373873005)) {
                    // 373873005 |Pharmaceutical / biologic product (product)|
                    pharma_miss_cnt++;
                } else if (isas.hasAncestor(sctid, 127785005)) {
                    // 127785005 |Administration of substance to produce immunity, either active or
                    // passive (procedure)|
                } else if (isas.hasAncestor(sctid, 713404003)) {
                    // 713404003 |Vaccination given (situation)|
                } else if (isas.hasAncestor(sctid, 591000119102l)) {
                    // 591000119102 |Vaccine declined by patient (situation)|
                } else if (isas.hasAncestor(sctid, 90351000119108l)) {
                    // 90351000119108 |Vaccination not done (situation)|
                } else if (isas.hasAncestor(sctid, 293104008)) {
                    // 293104008 |Adverse reaction to component of vaccine product (disorder)|
                } else if (isas.hasAncestor(sctid, 266758009)) {
                    // 266758009 |Immunization contraindicated (situation)|
                } else {
                    other_misses.add(sctid);
                    other_miss_cnt++;
                }
            }
        }
        isas.getOrderedConcepts().stream().filter(other_misses::contains) //
                .limit(10) //
                .forEach((sctid) -> {
                    UUID uuid = UuidUtil.fromSNOMED("" + sctid);
                    int nid = PrimitiveData.nid(uuid);
                    LOG.error("Miss: " + sctid + " " + PrimitiveData.text(nid));
                    Set<Long> sups = toSctids(reasoner.getSuperConcepts(nid));
                    Set<Long> parents = isas.getParents(sctid);
                    HashSet<Long> par = new HashSet<>(parents);
                    par.removeAll(sups);
                    HashSet<Long> sup = new HashSet<>(sups);
                    sup.removeAll(parents);
                    LOG.error("Sno:  " + par);
                    LOG.error("Elk:  " + sup);
                    if (sups.contains(null)) {
                        reasoner.getSuperConcepts(nid)
                                .forEach(sup_nid -> LOG.error("   :  " + PrimitiveData.text((sup_nid.intValue()))));
                    }
                });
        LOG.error("Miss cnt: " + miss_cnt);
        LOG.error("Pharma cnt: " + pharma_miss_cnt);
        LOG.error("Other cnt: " + other_miss_cnt);
        assertEquals(expected_non_snomed_cnt, non_snomed_cnt);
        assertEquals(expected_miss_cnt, miss_cnt);
        assertEquals(expected_pharma_miss_cnt, pharma_miss_cnt);
        assertEquals(expected_other_miss_cnt, other_miss_cnt);
    }

    public boolean compareEquals(Object expect, Object actual, String msg) {
        if (Objects.equals(expect, actual))
            return true;
        LOG.error(msg);
        LOG.error("\tExpect: " + expect);
        LOG.error("\tActual: " + actual);
        return false;
    }

    // In these compare methods need to create new Sets and copy Concepts since we
    // updated the ids. This update causes the hash codes to change and equals would
    // fail otherwise.

    public boolean compare(RoleType expect, RoleType actual, SnomedOntology snomedOntology) {
        String con_msg = expect.getId() + " " + snomedOntology.getFsn(expect.getId());
        return compareEquals(new HashSet<>(expect.getSuperRoleTypes()), new HashSet<>(actual.getSuperRoleTypes()),
                "Super Role Types " + con_msg)
                & compareEquals(expect.isTransitive(), actual.isTransitive(), "Transitive " + con_msg)
                & compareEquals(expect.getChained(), actual.getChained(), "Chained " + con_msg)
                & compareEquals(expect.isReflexive(), actual.isReflexive(), "Reflexive " + con_msg);
    }

    public boolean compare(ConcreteRoleType expect, ConcreteRoleType actual, SnomedOntology snomedOntology) {
        String con_msg = expect.getId() + " " + snomedOntology.getFsn(expect.getId());
        return compareEquals(new HashSet<>(expect.getSuperConcreteRoleTypes()),
                new HashSet<>(actual.getSuperConcreteRoleTypes()), "Super Concrete Role Types " + con_msg);
    }

    public boolean compare(Concept expect, Concept actual, SnomedOntology snomedOntology) {
//		compareDefinitions(expect.getDefinitions(), actual.getDefinitions(), "Definitions ", expect, snomedOntology);
//		compareDefinitions(expect.getGciDefinitions(), actual.getGciDefinitions(), "Gci Definitions ", expect,
//				snomedOntology);
        String con_msg = expect.getId() + " " + snomedOntology.getFsn(expect.getId());
        return compareEquals(new HashSet<>(expect.getDefinitions()),
                new HashSet<>(actual.getDefinitions().stream().map(x -> x.copy()).toList()), "Definitions " + con_msg)
                & compareEquals(new HashSet<>(expect.getGciDefinitions()),
                new HashSet<>(actual.getGciDefinitions().stream().map(x -> x.copy()).toList()),
                "Gci Definitions " + con_msg);
    }

    public void statedPattern() throws Exception {
        ViewCalculator viewCalculator = getViewCalculator();
        LogicCoordinateRecord logicCoordinateRecord = viewCalculator.logicCalculator().logicCoordinateRecord();
        assertEquals(TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid(),
                logicCoordinateRecord.statedAxiomsPatternNid());
    }

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
    public ArrayList<String> getSupercs(ElkSnomedData data, SnomedOntologyReasoner reasoner) {
        ArrayList<String> lines = new ArrayList<>();
        for (Concept con : data.getConcepts()) {
            int con_id = (int) con.getId();
            String con_str = PrimitiveData.publicId(con_id).asUuidArray()[0] + "\t" + PrimitiveData.text(con_id);
            for (Concept sup : reasoner.getSuperConcepts(con)) {
                int sup_id = (int) sup.getId();
                String sup_str = PrimitiveData.publicId(sup_id).asUuidArray()[0] + "\t" + PrimitiveData.text(sup_id);
                lines.add(con_str + "\t" + sup_str);
            }
        }
        Collections.sort(lines);
        return lines;
    }

    protected ElkSnomedData buildSnomedData() throws Exception {
        ViewCalculator viewCalculator = getViewCalculator();
        ElkSnomedData data = new ElkSnomedData();
        ElkSnomedDataBuilder builder = new ElkSnomedDataBuilder(viewCalculator,
                TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN, data);
        builder.build();
        return data;
    }

    public ArrayList<String> runSnomedReasoner() throws Exception {
        ElkSnomedData data = buildSnomedData();
        SnomedOntology ontology = new SnomedOntology(data.getConcepts(), data.getRoleTypes(), List.of());
        SnomedOntologyReasoner reasoner = SnomedOntologyReasoner.create(ontology);
        Files.createDirectories(getWritePath("supercs").getParent());
        Path path = getWritePath("supercs");
        ArrayList<String> lines = getSupercs(data, reasoner);
        Files.write(path, lines);
        return lines;
    }

    protected Path getWritePath(String filePart) throws IOException {
        Path path = Paths.get("target", filePart + ".txt");
        Files.createDirectories(path.getParent());
        return path;
    }

    protected ViewCalculator getViewCalculator() {
        ViewCoordinateRecord vcr = Coordinates.View.DefaultView();

        return ViewCalculatorWithCache.getCalculator(vcr);
    }

    public ArrayList<String> runSnomedReasonerService() throws Exception {
        LOG.info("runSnomedReasonerService");
        ReasonerService rs = initReasonerService();
        rs.extractData();
        rs.loadData();
        rs.computeInferences();
        Files.createDirectories(getWritePath("supercs").getParent());
        Path path = getWritePath("supercs");
        ArrayList<String> lines = getSupercs(rs);
        Files.write(path, lines);
        return lines;
    }

    public ReasonerService initReasonerService() {
        ReasonerService rs = PluggableService.load(ReasonerService.class).stream()
                .filter(x -> x.type().getSimpleName().equals(ElkSnomedReasonerService.class.getSimpleName())) //
                .findFirst().get().get();
        rs.init(getViewCalculator(), TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN,
                TinkarTerm.EL_PLUS_PLUS_INFERRED_AXIOMS_PATTERN);
        rs.setProgressUpdater(null);
        return rs;
    }

    public ArrayList<String> getSupercs(ReasonerService rs) {
        ArrayList<String> lines = new ArrayList<>();
        for (int con_id : rs.getReasonerConceptSet().toArray()) {
            String con_str = PrimitiveData.publicId(con_id).asUuidArray()[0] + "\t" + PrimitiveData.text(con_id);
            for (int sup_id : rs.getParents(con_id).toArray()) {
                String sup_str = PrimitiveData.publicId(sup_id).asUuidArray()[0] + "\t" + PrimitiveData.text(sup_id);
                lines.add(con_str + "\t" + sup_str);
            }
        }
        Collections.sort(lines);
        return lines;
    }

    public ReasonerService runReasonerServiceNNF() throws Exception {
        LOG.info("runReasonerServiceNNF");
        ReasonerService rs = initReasonerService();
        rs.extractData();
        rs.loadData();
        rs.computeInferences();
        rs.buildNecessaryNormalForm();
        return rs;
    }

    @Override
    protected boolean assertLine(String[] columns) {
        return false;
    }
}
