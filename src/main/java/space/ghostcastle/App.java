package space.ghostcastle;

import space.ghostcastle.Exceptions.*;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;


public class App {
    private ArrayNode containers;
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/", ctx -> ctx.result("Hello to traefik sync app"));
            config.routes.post("/sync", ctx -> {
                DockerProxies dP = DockerProxies.create();
                ArrayNode containers = dP.getContainers();
                System.out.println();

                for (JsonNode node : containers) {
                    TraefikRouteConfig traefikRouteConfig = new TraefikRouteConfig();
                    try {
                        traefikRouteConfig.validateJsonNode(node.path("Labels"));
                        traefikRouteConfig.declare();
                    } catch (InvalidJsonNodeException e) {
                        System.err.println(e);
                        // e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }).start(7000);
    }


}
