package space.ghostcastle;

import space.ghostcastle.Exceptions.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class App {

    private static ArrayNode containers;
    public static void main(String[] args) {
        
        Javalin app = Javalin.create(config -> {
            
            config.routes.get("/", ctx -> ctx.result("Hello from traefik sync app"));
            
            config.routes.post("/sync", ctx -> buildHTTPConfig(ctx));
        
        }).start(7000);

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    private static void buildHTTPConfig(Context ctx) {
        
        DockerProxies dP = DockerProxies.create();
        //FIXME: This will add containers of all proxies into one single file which we dont want, get proxies and create TraefikFileConfig for each from DockerProxies to here instead or create a Proxy to Cotnainer Map.
        containers = dP.getContainers();
        TraefikFileConfig traefikFileConfig = new TraefikFileConfig();

        for (JsonNode node : containers) {
            try {
                traefikFileConfig.add(node);
            } catch (InvalidJsonNodeException e) {
                ctx.status(400).result(e.getMessage());
            }
        }
    }
}
