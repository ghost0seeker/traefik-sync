package space.ghostcastle;

import io.javalin.Javalin;

import java.util.List;
import space.ghostcastle.DockerProxies;

public class App {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/", ctx -> ctx.result("Hello to traefik sync app"));
            config.routes.post("/sync", ctx -> {
                DockerProxies dP = DockerProxies.create();
                dP.getContainers();
            });
        }).start(7000);
    }

}
