package dev.ikm.tinkar.snomedct.integration;

import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

public abstract class BaseIntegrationTest {

    @AfterAll
    public static void shutdown() {
        PrimitiveData.stop();
    }

    @BeforeAll
    public static void setup() {
        CachingService.clearAll();
        File datastore = new File(System.getProperty("user.home") + "/Solor/generated-data"); //Note. Dataset needed to be generated within repo, with command 'mvn clean install'
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastore);
        PrimitiveData.selectControllerByName("Open SpinedArrayStore");
        PrimitiveData.start();
    }

    //Helper method to handle logging
    protected void logError(String errorFilePath, String message) {
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter(errorFilePath, true));
            bw.write(message + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void processFileLines(String sourceFilePath, Consumer<String[]> lineProcessor) {
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(sourceFilePath))) {
            while ((line = br.readLine()) != null) {
                if (line.startsWith("id")) continue;
                String[] columns = line.split("\\t");
                lineProcessor.accept(columns);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
