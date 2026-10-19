package space.ghostcastle;

import space.ghostcastle.Exceptions.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class TraefikSync {

    private Javalin app;
    private DockerProxies dockerProxies;

    private TraefikSync() {
        this.dockerProxies = DockerProxies.create();
    }

    private void start() {
        Javalin app = Javalin.create(config -> {
            
            config.routes.get("/", ctx -> ctx.result("Hello from traefik sync app"));
            
            config.routes.post("/sync", ctx -> this.buildHTTPConfig(ctx));
        
        }).start(7000);

        this.app = app;
    }

    private void stop() {
        this.app.stop();
    }
    public static void main(String[] args) {
        TraefikSync ts = new TraefikSync();
        ts.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ts::stop));
    }

    private Context buildHTTPConfig(Context ctx) {
        int failedContainers = 0;
        int netContainers = 0;
        TraefikFileConfig traefikFileConfig = new TraefikFileConfig();
        
        //FIXME: This will add containers of all proxies into one single file which we dont want, get proxies and create TraefikFileConfig for each from DockerProxies to here instead or create a Proxy to Cotnainer Map.
        
        try {
            ArrayNode containers = this.dockerProxies.getContainers();

            for (JsonNode node : containers) {
                traefikFileConfig.createConfig(node);
                netContainers++;
            }
            
        } catch (InvalidJsonNodeException e) {
            failedContainers++;
            // return ctx.status(400).result(e.getMessage());
        }

        traefikFileConfig.createFileConfig("envy");
        traefikFileConfig.createFileConfig("vultr-one");

        return ctx.status(200).result("Success\n" + "No of Containers: " + netContainers + (failedContainers != 0 ? ("\nFailed Containers: " + failedContainers) : "" ));
    }
}
