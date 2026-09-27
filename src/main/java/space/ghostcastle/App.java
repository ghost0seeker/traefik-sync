package space.ghostcastle;

import space.ghostcastle.Exceptions.*;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class App {
    private static ArrayNode containers;
    private static List<Map<String, String>> routerConfigs = new ArrayList<>();
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/", ctx -> ctx.result("Hello to traefik sync app"));
            config.routes.post("/sync", ctx -> {
                DockerProxies dP = DockerProxies.create();
                containers = dP.getContainers();
                System.out.println();

                for (JsonNode node : containers) {
                    TraefikRouteConfig traefikRouteConfig = new TraefikRouteConfig();
                    try {
                        traefikRouteConfig.validateJsonNode(node.path("Labels"));
                        traefikRouteConfig.declare();
                        routerConfigs.add(traefikRouteConfig.getConfig());
                    } catch (InvalidJsonNodeException e) {
                        System.err.println(e);
                        // e.printStackTrace();
                    }
                }
            });
        }).start(7000);

        String str = "str";
    }


}
