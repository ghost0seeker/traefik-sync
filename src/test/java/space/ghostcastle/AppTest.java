package space.ghostcastle;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import space.ghostcastle.Records.DockerProxyRecord;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest 
public class AppTest {

    private final Path socketProxiesFile = Path.of(".socket-proxies");
    private final Path socketProxiesBackup = Path.of(".socket-proxies.bak");

    private String baseUrl;
    private HttpClient client;
    private DockerProxies dp;
    private ObjectMapper map = new ObjectMapper();
    private DockerProxies dockerProxies;
    

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        client = HttpClient.newHttpClient();
        this.backupProxyFile();
        dockerProxies = DockerProxies.create();
        
    }

    @AfterEach
    void cleanUp() {
        this.restoreProxyFile();
    }

    @Test
    void testDockerProxies () throws IllegalStateException {
        List<DockerProxyRecord> records = dockerProxies.getProxyRecords();
        for (DockerProxyRecord record : records) {
            TraefikFileConfig tfConfig = new TraefikFileConfig();
            tfConfig.createConfig(record);
        }

    }

    private void backupProxyFile() {
        try {
            if (Files.exists(this.socketProxiesFile)) {
                Files.move(socketProxiesFile, socketProxiesBackup, StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(socketProxiesFile, this.baseUrl);
            }
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private void restoreProxyFile() {
        try {
            if (Files.exists(socketProxiesBackup)) {
                Files.move(socketProxiesBackup, socketProxiesFile, StandardCopyOption.REPLACE_EXISTING);
            }           
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

}
