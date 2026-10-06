package space.ghostcastle;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public class DockerProxies {

    private List<String> dockerProxies = new ArrayList<>();
    private final HttpClient client = HttpClient.newHttpClient(); 
    private static DockerProxies instance;
    private static final Path socketProxiesFile = Paths.get(".socket-proxies");
    private static final String containerPath = "/containers/json";
    private static final String infoPath = "/info";
    private final ObjectMapper map = new ObjectMapper();

    private DockerProxies() {
        try (Stream<String> lines = Files.lines(socketProxiesFile)) {
            lines.forEach(this.dockerProxies::add);
        } catch (IOException e) {
            System.err.println("Could not find .socket-proxies,\nplease create a .socket-proxies file to declare your docker socket proxies to use");
            System.exit(1);
        }
    }

    public static DockerProxies create() {
        if (instance == null) {
            instance = new DockerProxies();
        }
        return instance;
    }

    private JsonNode makeHttpGetRequest(String url, String path) {
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url + path))
            .GET()
            .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return map.readTree(response.body());

        } catch(IOException | InterruptedException e ) {
            e.printStackTrace();
            return null;
        }
    }

    private String getHostname(String proxy) {
            
        JsonNode rootNode = makeHttpGetRequest(infoPath, containerPath);
            
        if (rootNode == null) {
            throw new NullPointerException("Something went wrong with calling proxy " + proxy + " on path " + infoPath); 
        }

        return rootNode.get("Name").toString();
    }

    public ArrayNode getContainers() {
        ObjectMapper yamlMapper = new ObjectMapper(
            new YAMLFactory()
        );
        ArrayNode containers = yamlMapper.createArrayNode();

        for (String proxy : this.dockerProxies) {

            JsonNode rootNode = makeHttpGetRequest(proxy, containerPath);
            if (rootNode == null) {
                throw new NullPointerException("Something went wrong with calling proxy " + proxy + " on path " + containerPath);
            }

            String proxyName = getHostname(proxy);
            
            for (JsonNode container : rootNode) {
                ObjectNode selectedKeys = yamlMapper.createObjectNode();
                selectedKeys.put("Hostname", proxyName);
                selectedKeys.put("Id", container.path("Id").asText());
                selectedKeys.set("Names", container.path("Names"));
                selectedKeys.set("Ports", container.path("Ports"));
                selectedKeys.set("Labels", container.path("Labels"));
                containers.add(selectedKeys);
            }
        }
        return containers;
    }
}
