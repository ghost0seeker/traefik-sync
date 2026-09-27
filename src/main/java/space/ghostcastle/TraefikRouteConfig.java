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

import space.ghostcastle.Exceptions.InvalidJsonNodeException;


public class TraefikRouteConfig {

    private ObjectMapper mapper;
    private Boolean isRouterValid = false;
    private Map<String, String> jsonMap;
    public Router router;

    public Boolean isValid() {
        return this.isRouterValid;
    }

    private class Service {
        String name;
        String loadBalancer;

        public Service() {}
    }

    private class MiddleWares {
        String name;
    }

    public class Router {
        private Boolean enabled = false;
        private String name;
        private String entryPoints;
        private String rule;
        private String tls;
        private Service service;
        private MiddleWares middleWares;
        private static Map<String, String> mandatoryKeys = new HashMap<>();

/*
    private static Map<String, String> mandatoryKeys = new HashMap<>(Map.ofEntries(
        Map.entry("traefik.enable", null),
        Map.entry("traefik.http.routers.<name>.entryPoints", null),
        Map.entry("traefik.http.routers.<name>.rule", null),
        Map.entry("traefik.http.routers.<name>.tls", null),
        Map.entry("traefik.http.services.<name>.loadBalancer.server.port", null)
    ));
*/
        
        public Router() {
            Set<String> labels = Router.mandatoryKeys.keySet();

            labels.forEach(label -> {
                if (label.contains("traefik.enable")) {
                    this.enabled = Boolean.parseBoolean(mandatoryKeys.get(label));
                } else if (label.contains("traefik.http.routers")) {
                    String[] splitLabel = label.split("\\.");

                    this.name = splitLabel[3];

                    if (label.contains("entrypoints")) {
                        this.entryPoints = mandatoryKeys.get(label);
                    } else if (label.contains("rule")) {
                        this.rule = mandatoryKeys.get(label);
                    } else if (label.contains("tls")) {
                        this.tls = mandatoryKeys.get(label);
                    }
                } else if (label.contains("traefik.http.services")) {
                    this.service = new Service();
                    String[] splitLabel = label.split("\\.");
                    this.service.name = splitLabel[3];
                    this.service.loadBalancer = mandatoryKeys.get(label);
                }
            });
        }
        
    }

    public TraefikRouteConfig () {
        this.mapper = new ObjectMapper();
    }


    public void validateJsonNode(JsonNode node) throws InvalidJsonNodeException {
        Map<String, String> map = mapper.convertValue(node, new TypeReference<Map<String, String>>() {});
        Set<String> routerKeySet = map.keySet();
        List<String> routerKeys = routerKeySet.stream()
                                                .filter(s -> s.startsWith("traefik"))
                                                .toList();
        Boolean isTrue = routerKeys.isEmpty();
        if (!routerKeys.isEmpty()) {
            routerKeys.forEach(k -> Router.mandatoryKeys.put(k, map.get(k)));
            this.isRouterValid = true;
        }
    }

    public void declare() throws InvalidJsonNodeException {

        if (this.isRouterValid.equals(false)) {
            throw new InvalidJsonNodeException("JsonNode is not valid structure to build router configuration.");
        }

        this.router = new Router();
    }

    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();

        String defaultMap = "traefik.http.";
        String routerMap = defaultMap + "routers." + this.router.name;
        String servicesMap = defaultMap + "services." + this.router.service.name;

        config.put(routerMap + ".entrypoints", this.router.entryPoints);
        config.put(routerMap + ".rule", this.router.rule);
        config.put(routerMap + ".tls", this.router.tls);
        config.put(servicesMap + ".loadBalancer.servers[0].url", this.router.service.loadBalancer);

        return config;
    }

    @Override
    public String toString() {
        return this.router.name;
    }
}