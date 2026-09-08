package space.ghostcastle;

import io.javalin.Javalin;

public class App {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/", ctx -> ctx.result("Hello from Javalin"));
        }).start(7000);
    }

}
