package space.ghostcastle;

import java.util.HashMap;
import java.util.List;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;
import java.util.Map;

public class MakeRequest {
    private List<String> dockerSocketProxies;
    private final HttpClient client = HttpClient.newHttpClient(); 
    private static MakeRequest instance;
    private static Path socketProxiesFile = Paths.get(".socket-proxy");

    private MakeRequest() {
        try (Stream<String> lines = Files.lines(socketProxiesFile)) {
            lines.forEach(dockerSocketProxies::add);
        } catch (IOException e) {
            System.err.println("Could not find .socket-proxies,\nplease create a .socket-proxies to mention all proxies to use");
        }
    }

    public static MakeRequest createInstance() {
        if (instance == null) {
            instance = new MakeRequest();
        }
        return instance;
    }

    public Map<String, String> getContainers() {
        Map<String, String> labels = new HashMap<>();

        /*
            use http client to get labels from all socket proxies
            get container name, service name, ports, labels
        */

        return labels;
    }
}
