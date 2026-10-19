package space.ghostcastle;

import java.io.File;
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

import space.ghostcastle.Exceptions.InvalidJsonNodeException;
import space.ghostcastle.Records.DockerProxyRecord;

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
            List<String> dockerProxies = lines.toList();
            if (dockerProxies.size() == 0) {
                throw new IllegalStateException(".socket-proxies is empty");
            }
            this.dockerProxies = dockerProxies;
        } catch (IOException e) {
            System.err.println("Could not find .socket-proxies,\nplease create a .socket-proxies file to declare your docker socket proxies to use");
            System.exit(1);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
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
            String str = response.body();
            if (str.contains("html")) {
                throw new InvalidJsonNodeException("Server returned html for proxy host " + url);
            }
            return map.readTree(response.body());

        } catch(IOException | InterruptedException e ) {
            e.printStackTrace();
            return null;
        }
    }

    private String getHostname(String proxy) {
            
        JsonNode rootNode = makeHttpGetRequest(proxy, infoPath);
            
        if (rootNode == null) {
            throw new NullPointerException("Something went wrong with calling proxy " + proxy + " on path " + infoPath); 
        }

        String a = rootNode.get("Name").toString();
        String b = a.replaceAll("\"", "");
        return b;
    }

    public List<DockerProxyRecord> getProxyRecords() {
        
        List<DockerProxyRecord> records = new ArrayList<>();

        for (String proxy : this.dockerProxies) {

            JsonNode rootNode = makeHttpGetRequest(proxy, containerPath);

            if (rootNode == null) {
                throw new NullPointerException("Something went wrong with calling proxy " + proxy + " on path " + containerPath);
            }

            if (!rootNode.isArray()) {
                throw new  InvalidJsonNodeException("GET " + proxy + "/containers/json did not return a json ArrayNode.");
            }

            ArrayNode arrayNode = map.createArrayNode();
            String proxyName = getHostname(proxy);

            for (JsonNode node : rootNode) {
                ObjectNode objNode = map.createObjectNode();
                
                objNode.put("Id", node.path("Id").asText());
                objNode.set("Names", node.path("Names"));
                objNode.set("Ports", node.path("Ports"));
                objNode.set("Labels", node.path("Labels"));
                
                arrayNode.add(objNode);
            }
            records.add(new DockerProxyRecord(proxyName, arrayNode));
        }
        
        
        return records;
    }
}
