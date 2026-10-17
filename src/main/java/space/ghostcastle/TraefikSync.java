package space.ghostcastle;

import space.ghostcastle.Exceptions.*;
import space.ghostcastle.Records.FileConfig;
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
        
        //FIXME: This will add containers of all proxies into one single file which we dont want, get proxies and create TraefikFileConfig for each from DockerProxies to here instead or create a Proxy to Cotnainer Map.
        ArrayNode containers = this.dockerProxies.getContainers();
        TraefikFileConfig traefikFileConfig = new TraefikFileConfig();

        for (JsonNode node : containers) {
            try {
                traefikFileConfig.createConfig(node);
            } catch (InvalidJsonNodeException e) {
                ctx.status(400).result(e.getMessage());
            }
        }
        FileConfig fileConfig = traefikFileConfig.getConfig("localhost");
        System.out.println(fileConfig);
        return ctx.status(200).result("Success");
    }
}
