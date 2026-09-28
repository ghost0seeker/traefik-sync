package space.ghostcastle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;

import space.ghostcastle.Exceptions.InvalidJsonNodeException;

public class TraefikFileConfig {

    record FileConfig(HTTPConfig http) {

        record HTTPConfig(

            Map<String, Router> routers, Map<String, Service> services) {

                record Router(
                    List<String> entryPoints,
                    String rule,
                    TLS tls,
                    Service service
                ) {
                    record TLS(){};
                }

                record Service(
                    LoadBalancer loadBalancer
                ) {
                    record LoadBalancer(List<Server> servers) {
                        record Server(String url) {}
                    }
                }

        }
    }

    public static ObjectMapper mapper = new ObjectMapper();

    public void valideKeys(JsonNode node) throws InvalidJsonNodeException {
        Map<String, String> map = mapper.convertValue(
            node,
            new TypeReference<Map<String, String>>() {}
        );
        Set<String> routerKeySet = map.keySet();
        List<String> routerKeys = routerKeySet.stream()
                                                .filter(s -> s.matches("traefik\\.http\\.routers\\.[^.]+\\.entryPoints"))
                                                .filter(s -> s.matches("traefik\\.http\\.routers\\.[^.]+\\.rule"))
                                                .filter(s -> s.matches("traefik\\.http\\.routers\\.[^.]+\\.tls"))
                                                .filter(s -> s.matches("traefik\\.http\\.routers\\.[^.]+\\.service"))
                                                .filter(s -> s.matches("traefik\\.http\\.services\\.[^.]+\\.loadBalancer"))
                                                .toList();
        Boolean isEmpty = routerKeys.isEmpty();
        if (!isEmpty) {
            /*
                create the record and save it
            */

            
        }


    }
}
