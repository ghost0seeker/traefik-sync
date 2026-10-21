package space.ghostcastle;

import static space.ghostcastle.DockerProxies.makeHttpGetRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import space.ghostcastle.Records.DockerProxyRecord;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest 
public class AppTest {

    private String baseUrl;
    private HttpClient client;
    private DockerProxies dp;
    private ObjectMapper map = new ObjectMapper();
    private DockerProxies dockerProxies;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        dockerProxies = DockerProxies.create();
        client = HttpClient.newHttpClient();
    }

    @Test
    void testDockerProxies () throws IllegalStateException {
        List<DockerProxyRecord> records = dockerProxies.getProxyRecords();
        for (DockerProxyRecord record : records) {
            TraefikFileConfig tfConfig = new TraefikFileConfig();
            tfConfig.createConfig(record);
        }

    }


}
