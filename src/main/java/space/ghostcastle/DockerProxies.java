package space.ghostcastle;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.net.URI;
import java.lang.Boolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DockerProxies {
    private List<String> dockerProxies = new ArrayList<>();
    private final HttpClient client = HttpClient.newHttpClient(); 
    private static DockerProxies instance;
    private static Path socketProxiesFile = Paths.get(".socket-proxies");
    private static String path = "/containers/json";
    private ObjectMapper map = new ObjectMapper();

    private DockerProxies() {
        try (Stream<String> lines = Files.lines(socketProxiesFile)) {
            lines.forEach(this.dockerProxies::add);
        } catch (IOException e) {
            System.err.println("Could not find .socket-proxies,\nplease create a .socket-proxies to declare your docker socket proxies to use");
            System.exit(1);
        }
    }

    public static DockerProxies create() {
        if (instance == null) {
            instance = new DockerProxies();
        }
        return instance;
    }

    public List<Map<String, Object>> getContainers() throws Exception {
        List<Map<String, Object>> containers = new ArrayList<>();

        for (String proxy : dockerProxies) {
            HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(proxy + path))
            .GET()
            .build();

            String responseBody = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

            JsonNode rootNode = map.readTree(responseBody);

            for (JsonNode container : rootNode) {
                String isTraefikEnabled = container.path("Labels").path("traefik.enable").asText();
                System.out.println(Boolean.parseBoolean(isTraefikEnabled));
            }

        }


        return containers;
    }
}
