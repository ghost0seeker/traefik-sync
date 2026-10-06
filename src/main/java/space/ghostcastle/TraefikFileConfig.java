package space.ghostcastle;
/**
 * TODO: javadoc
 *
 *
 * @param node 
 * @return 
 * @throws InvalidJsonNodeException if all required router config keys are missing
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import space.ghostcastle.Exceptions.InvalidJsonNodeException;
import space.ghostcastle.Records.FileConfig.HTTPConfig;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Router;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Router.TLS;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service.LoadBalancer;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service.LoadBalancer.Server;


public class TraefikFileConfig {

    public static List<HTTPConfig> configs = new ArrayList<>();

    private static final ObjectMapper mapper = new ObjectMapper();

    private static List<String> nullKeys;

    private static Map<String, StringBuilder> mandatoryLabels = new HashMap<>(Map.of(
        "entrypoints", new StringBuilder(),
        "rule", new StringBuilder(),
        "tls", new StringBuilder(),
        "service", new StringBuilder(),
        "url", new StringBuilder()
    ));

    public static void add(JsonNode node) {
        Map<String, String> map = mapper.convertValue(
            node,
            new TypeReference<Map<String, String>>() {}
        );
        Set<String> routerKeySet = map.keySet();

        Pattern routerNameRegex = Pattern.compile("traefik\\.http\\.routers\\.([a-zA-Z0-9-]+)\\..+");
        Matcher routerNameMatcher;
        StringBuilder routerNameBuilder = new StringBuilder();

        Pattern serviceNameRegex = Pattern.compile("traefik\\.http\\.services\\.([a-zA-Z0-9-]+)\\..+");
        Matcher serviceNameMatcher;
        StringBuilder serviceNameBuilder = new StringBuilder();

        Pattern labelRegex = Pattern.compile("entryp?P?oints|rule|tls");
        Matcher labelMatcher;

        for (String key : routerKeySet) {
            routerNameMatcher = routerNameRegex.matcher(key);
            serviceNameMatcher = serviceNameRegex.matcher(key);
            
            if (routerNameMatcher.matches()) {
                if (routerNameBuilder.isEmpty()) {
                    routerNameBuilder.append(routerNameMatcher.group(1));
                }
                labelMatcher = labelRegex.matcher(key); 
                while(labelMatcher.find()) {
                    mandatoryLabels.get(labelMatcher.group().toLowerCase()).append(map.get(key).trim());
                }
            }
            
            if (serviceNameMatcher.matches()) {
                if (serviceNameBuilder.isEmpty()) {
                    serviceNameBuilder.append(serviceNameMatcher.group(1).trim());
                }
                mandatoryLabels.get("service").append(serviceNameBuilder.toString());
                mandatoryLabels.get("url").append(map.get(key));
            }
            
        }

        if (!routerNameBuilder.isEmpty() && !serviceNameBuilder.isEmpty()) {
            if (areLabelsValid()) {
                buildConfigRecord(routerNameBuilder.toString(), serviceNameBuilder.toString());
            } else {
                throw new InvalidJsonNodeException("JsonNode validation failed: Following labels " + nullKeys.toString().replaceAll("[\\[\\]]", "") + " were not found");
            }
        }

        
    }

    private static void buildConfigRecord(String routerName, String serviceName) {
            
        HTTPConfig config = new HTTPConfig(
            Map.of(routerName, new Router(
                mandatoryLabels.get("entrypoints").toString(),
                mandatoryLabels.get("rule").toString(),
                new TLS(),
                mandatoryLabels.get("service").toString()

            )),

            Map.of(serviceName, new Service(
                new LoadBalancer(List.of(new Server(mandatoryLabels.get("url").toString())))
            ))
        );

        configs.add(config);
    }

    private static Boolean areLabelsValid() {
        nullKeys = new ArrayList<>();
        
        for (Map.Entry<String, StringBuilder> entry : mandatoryLabels.entrySet()) {
            if (entry.getValue().toString().isBlank()) {
                nullKeys.add(entry.getKey());
            }
        }

        if (!nullKeys.isEmpty()) {
            return false;
        } else {
            return true;
        }

    }
}