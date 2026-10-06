package space.ghostcastle;

import space.ghostcastle.Exceptions.*;
import space.ghostcastle.Records.FileConfig.HTTPConfig;
import io.javalin.Javalin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;


public class App {
    private static ArrayNode containers;
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/", ctx -> ctx.result("Hello to traefik sync app"));
            config.routes.post("/sync", ctx -> {
                DockerProxies dP = DockerProxies.create();
                containers = dP.getContainers();
                System.out.println();

                for (JsonNode node : containers) {
                    try {
                        TraefikFileConfig.add(node.path("Labels"));
                    } catch (InvalidJsonNodeException e) {
                        System.err.println(e);
                        // e.printStackTrace();
                    }
                }

                List<HTTPConfig> configs = TraefikFileConfig.configs;
                System.lineSeparator();
            });
        }).start(7000);

    }



}
