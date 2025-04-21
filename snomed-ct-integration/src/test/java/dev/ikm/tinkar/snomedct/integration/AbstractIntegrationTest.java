package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.maven.SnomedUtility;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.uuid.UuidUtil;
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
import java.util.UUID;
import java.util.stream.Stream;

public abstract class AbstractIntegrationTest {
    Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    protected static int stated_count = -1;
    protected static int active_count = -1;
    protected static int inactive_count = -1;
    protected static int expected_supercs_cnt = -1;
    protected static int expected_non_snomed_cnt = 299;
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
                stated_count = 397848;
                active_count = 370185;
                inactive_count = 27663;
                expected_supercs_cnt = 604717;
            } else if (absolutePath.contains("20250101")) {
                stated_count = 399051;
                active_count = 371231;
                inactive_count = 27820;
                expected_supercs_cnt = 606768;
            } else if (absolutePath.contains("20250301")) {
                stated_count = 400646;
                active_count = 372705;
                inactive_count = 27941;
                expected_supercs_cnt = 610427;
            }
        } else if (absolutePath.contains("US")) {
            if (absolutePath.contains("20250301")) {
                stated_count = 407086;
                active_count = 378584;
                inactive_count = 28502;
                expected_supercs_cnt = 620167;
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
        log.info("We found file: " + sourceFilePath);
        return notFound;
    }

    protected UUID uuid(String id) {
        return SnomedUtility.generateUUID(UuidUtil.SNOMED_NAMESPACE, id);
    }

    protected abstract boolean assertLine(String[] columns);
}
