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
    private static Path socketProxiesFile = Paths.get(".socket-proxies");
    private static String path = "/containers/json";
    private ObjectMapper map = new ObjectMapper();

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

    public ArrayNode getContainers() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(
            new YAMLFactory()
        );
        ArrayNode containers = yamlMapper.createArrayNode();

        for (String proxy : this.dockerProxies) {

            try {
                
                HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(proxy + path))
                .GET()
                .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    continue;
                }

                JsonNode rootNode = map.readTree(response.body());

                // Path filePath = Paths.get("response.json");

                // try {
                //     Files.writeString(filePath, rootNode.toPrettyString());
                //     return 
                // }
                // for (JsonNode container : rootNode) {
                //     Boolean isTraefikEnabled = container.path("Labels").path("traefik.enable").asBoolean();
                //     System.out.println(isTraefikEnabled);
                // }
                
                for (JsonNode container : rootNode) {
                    ObjectNode selectedKeys = yamlMapper.createObjectNode();
                    selectedKeys.put("Id", container.path("Id").asText());
                    selectedKeys.set("Names", container.path("Names"));
                    selectedKeys.set("Ports", container.path("Ports"));
                    selectedKeys.set("Labels", container.path("Labels"));
                    containers.add(selectedKeys);
                }
                // String yamlString = yamlMapper.writeValueAsString(containers);
                // Files.writeString(Path.of("container" + ".yaml"), yamlString);
                return containers;
                
            } catch (Exception e) {
                e.printStackTrace();
                return containers;
            }
        }

        return  containers;

    }
}
