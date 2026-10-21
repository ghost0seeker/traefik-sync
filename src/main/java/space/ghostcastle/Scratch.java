package space.ghostcastle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.http.HttpClient;

import static space.ghostcastle.DockerProxies.makeHttpGetRequest;
class Scratch {
    private static ObjectMapper mapper = new ObjectMapper();
    public static void main(String[] args) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            JsonNode json = makeHttpGetRequest(client, "http://localhost:2375", "/containers/json");
            File containers = new File("containers.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(containers, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}