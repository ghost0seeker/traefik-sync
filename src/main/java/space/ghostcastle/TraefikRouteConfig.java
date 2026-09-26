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
    private Router router;

    private static List<String> runRegex(String regex, String input) {
        List<String> matchedStrings = new ArrayList<>();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            for (int a = 1; a <= matcher.groupCount(); a++){
                matchedStrings.add(matcher.group(a));
            } 
        }

        return matchedStrings;
    }

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

                    this.name = label.split(".")[3];

                    if (label.contains("entrypoints")) {
                        this.entryPoints = mandatoryKeys.get(label);
                    } else if (label.contains("rule")) {
                        this.rule = mandatoryKeys.get(label);
                    } else if (label.contains("tls")) {
                        this.tls = mandatoryKeys.get(label);
                    }
                } else if (label.contains("traefik.http.services")) {
                    this.service = new Service();
                    this.service.name = label.split(".")[3];
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

        
        if (!routerKeys.isEmpty()) {
            for (String routerKey : routerKeys) {
                if (!routerKeys.contains("traefik")) continue;
                String[] splitByDot = routerKey.split("\\.");
                switch (splitByDot[1]) {
                    case "enable":
                        Router.mandatoryKeys.put(routerKey, node.path(routerKey).asText());
                        break;
                    case "http":
                        switch (splitByDot[2]) {
                            case "routers":
                                switch (splitByDot[4]) {
                                    case "entrypoints":
                                    case "rule":
                                    case "tls":
                                        Router.mandatoryKeys.put(routerKey, node.path(routerKey).asText());
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case "services":
                                switch (splitByDot[4]) {
                                    case "loadbalancer":
                                        Router.mandatoryKeys.put(routerKey, node.path(routerKey).asText());
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
        }

        HashSet<String> routerKeyState = new HashSet<>(Router.mandatoryKeys.values());
        
        if (routerKeyState.contains(null)) {
            isRouterValid = false;
        } else {
            jsonMap = map;
            isRouterValid = true;
        }

        if (!isRouterValid) {
            throw new InvalidJsonNodeException("JsonNode is not valid structure to build router configuration.");
        }

        }

    }

    public void declare() throws InvalidJsonNodeException {

        if (this.isRouterValid.equals(false)) {
            throw new InvalidJsonNodeException("JsonNode is not valid structure to build router configuration.");
        }

        this.router = new Router();

        
    }
}