package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.elk.snomed.SnomedOntology;
import dev.ikm.elk.snomed.SnomedOntologyReasoner;
import dev.ikm.elk.snomed.model.Concept;
import dev.ikm.maven.SnomedUtility;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PluggableService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
import dev.ikm.tinkar.coordinate.view.calculator.ViewCalculator;
import dev.ikm.tinkar.reasoner.elksnomed.ElkSnomedData;
import dev.ikm.tinkar.reasoner.elksnomed.ElkSnomedDataBuilder;
import dev.ikm.tinkar.reasoner.elksnomed.ElkSnomedReasonerService;
import dev.ikm.tinkar.reasoner.service.ReasonerService;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

public abstract class AbstractIntegrationTest {
    Logger LOG = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    protected static int stated_count = -1;
    protected static int active_count = -1;
    protected static int inactive_count = -1;
    protected static int expected_supercs_cnt = -1;
    protected static int expected_non_snomed_cnt = 300;
    protected static int expected_miss_cnt = 0;
    protected static int expected_pharma_miss_cnt = 0;
    protected static int expected_other_miss_cnt = 0;

    @AfterAll
    public static void shutdown() {
        PrimitiveData.stop();
    }

    @BeforeAll
    public static void setup() throws IOException {
        CachingService.clearAll();
        //Note. Dataset needed to be generated within repo, with command 'mvn clean install'
        File datastore = new File(System.getProperty("datastorePath")); // property set in pom.xml
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastore);
        PrimitiveData.selectControllerByName("Open SpinedArrayStore");
        PrimitiveData.start();
        String sourceFilePath = "../snomed-ct-origin/target/origin-sources";
        String absolutePath = findFilePath(sourceFilePath, "Concept");
        if (absolutePath.contains("INT_")) {
            if (absolutePath.contains("20241201")) {
                stated_count = 397849;
                active_count = 370186;
                inactive_count = 27663;
                expected_supercs_cnt = 604718;
            } else if (absolutePath.contains("20250101")) {
                stated_count = 399052;
                active_count = 371232;
                inactive_count = 27820;
                expected_supercs_cnt = 606769;
            } else if (absolutePath.contains("20250301")) {
                stated_count = 400647;
                active_count = 372706;
                inactive_count = 27941;
                expected_supercs_cnt = 610428;
            }
        } else if (absolutePath.contains("US")) {
            if (absolutePath.contains("20250301")) {
                stated_count = 407087;
                active_count = 378585;
                inactive_count = 28502;
                expected_supercs_cnt = 620168;
            }
        }
    }

    /**
     * Find FilePath
     *
     * @param baseDir
     * @param fileKeyword
     * @return absolutePath
     * @throws IOException
     */
    protected static String findFilePath(String baseDir, String fileKeyword) throws IOException {

        try (Stream<Path> dirStream = Files.walk(Paths.get(baseDir))) {
            Path targetDir = dirStream.filter(Files::isDirectory)
//                    .filter(path -> path.toFile().getAbsoluteFile().toString().toLowerCase().contains(dirKeyword.toLowerCase()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Target DIRECTORY not found"));

            try (Stream<Path> fileStream = Files.walk(targetDir)) {
                Path targetFile = fileStream.filter(Files::isRegularFile)
                        .filter(path -> path.toFile().getAbsoluteFile().toString().toLowerCase().contains(fileKeyword.toLowerCase()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Target FILE not found for: " + fileKeyword));

                return targetFile.toAbsolutePath().toString();
            }
        }

    }

    /**
     * Process sourceFilePath
     *
     * @param sourceFilePath
     * @param errorFile
     * @return File status, either Found/NotFound
     * @throws IOException
     */
    protected int processFile(String sourceFilePath, String errorFile) throws IOException {
        int notFound = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(sourceFilePath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(errorFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("id") || line.startsWith("alternateIdentifier")) continue;
                if (!assertLine(line.split("\\t"))) {
                    notFound++;
                    bw.write(line);
                }
            }
        }
        LOG.info("We found file: " + sourceFilePath);
        return notFound;
    }

    protected UUID uuid(String id) {
        return SnomedUtility.generateUUID(UuidUtil.SNOMED_NAMESPACE, id);
    }


    /**
     * ****************************************************************************************************
     * CODE OBTAINED FROM ElkSnomedTestBase.java / reasoner-elk-snomed module / tinkar-core repo
     */
    protected Path getWritePath(String filePart) throws IOException {
        Path path = Paths.get("target", filePart + ".txt");
        LOG.info("Write path: " + path);
        Files.createDirectories(path.getParent());
        return path;
    }

    public ElkSnomedData buildSnomedData() throws Exception {
        LOG.info("buildSnomedData");
        ViewCalculator viewCalculator = PrimitiveDataTestUtil.getViewCalculator();
        ElkSnomedData data = new ElkSnomedData();
        ElkSnomedDataBuilder builder = new ElkSnomedDataBuilder(viewCalculator,
                TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN, data);
        builder.build();
        return data;
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

    public ArrayList<String> runSnomedReasoner() throws Exception {
        LOG.info("runSnomedReasoner");
        ElkSnomedData data = buildSnomedData();
        LOG.info("Create ontology");
        SnomedOntology ontology = new SnomedOntology(data.getConcepts(), data.getRoleTypes(),
                data.getConcreteRoleTypes());
        LOG.info("Create reasoner");
        SnomedOntologyReasoner reasoner = SnomedOntologyReasoner.create(ontology);
        Files.createDirectories(getWritePath("supercs").getParent());
        Path path = getWritePath("supercs");
        ArrayList<String> lines = getSupercs(data, reasoner);
        Files.write(path, lines);
        return lines;
    }

	public ReasonerService initReasonerService() {
		ReasonerService rs = PluggableService.load(ReasonerService.class).stream()
				.filter(x -> x.type().getSimpleName().equals(ElkSnomedReasonerService.class.getSimpleName())) //
				.findFirst().get().get();
		rs.init(PrimitiveDataTestUtil.getViewCalculator(), TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN,
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

	public ReasonerService runReasonerServiceNNF() throws Exception {
		LOG.info("runReasonerServiceNNF");
		ReasonerService rs = initReasonerService();
		rs.extractData();
		rs.loadData();
		rs.computeInferences();
		rs.buildNecessaryNormalForm();
		return rs;
	}
    /**
     * ****************************************************************************************************
     * ****************************************************************************************************
     */

    protected abstract boolean assertLine(String[] columns);
}
