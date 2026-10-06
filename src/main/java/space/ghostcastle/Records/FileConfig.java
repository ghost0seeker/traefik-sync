package space.ghostcastle.Records;

import java.util.List;
import java.util.Map;

public record FileConfig(HTTPConfig http) {

    public record HTTPConfig(

        Map<String, Router> routers, Map<String, Service> services) {

            public record Router(
                String entryPoints,
                String rule,
                TLS tls,
                String service
            ) {
                public record TLS(){};
            }

            public record Service(
                LoadBalancer loadBalancer
            ) {
                public record LoadBalancer(List<Server> servers) {
                    public record Server(String url) {}
                }
            }

    }
}