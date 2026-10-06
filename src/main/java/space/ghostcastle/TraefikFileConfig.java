package space.ghostcastle;
/**
 * TODO: javadoc
 *
 *
 * @param node 
 * @return 
 * @throws InvalidJsonNodeException if all required router config keys are missing
 */


import java.io.File;
import java.io.IOException;
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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import space.ghostcastle.Exceptions.InvalidJsonNodeException;
import space.ghostcastle.Records.FileConfig;
import space.ghostcastle.Records.FileConfig.HTTPConfig;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Router;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Router.TLS;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service.LoadBalancer;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service.LoadBalancer.Server;


public class TraefikFileConfig {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private final Pattern routerNameRegex = Pattern.compile("traefik\\.http\\.routers\\.([a-zA-Z0-9-]+)\\..+");
    private Matcher routerNameMatcher;
    private StringBuilder routerNameBuilder = new StringBuilder();

    private final Pattern serviceNameRegex = Pattern.compile("traefik\\.http\\.services\\.([a-zA-Z0-9-]+)\\..+");
    private Matcher serviceNameMatcher;
    private StringBuilder serviceNameBuilder = new StringBuilder();

    private final Pattern labelRegex = Pattern.compile("entryp?P?oints|rule|tls");
    private Matcher labelMatcher;

    private List<String> nullKeys = new ArrayList<>();

    private Map<String, StringBuilder> mandatoryLabels = new HashMap<>(Map.of(
        "entrypoints", new StringBuilder(),
        "rule", new StringBuilder(),
        "tls", new StringBuilder(),
        "service", new StringBuilder(),
        "url", new StringBuilder()
    ));

    // private List<HTTPConfig> httpConfigs = new ArrayList<>();
    private Map<String, Router> routers = new HashMap<>();
    private Map<String, Service> services = new HashMap<>();

    private StringBuilder fileNameBuilder = new StringBuilder();

    public void add(JsonNode node) {
        Map<String, String> map = mapper.convertValue(
            node,
            new TypeReference<Map<String, String>>() {}
        );
        Set<String> routerKeySet = map.keySet();

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

        if (!routerNameBuilder.isEmpty() || !serviceNameBuilder.isEmpty()) {
            if (!areLabelsValid()) {
                throw new InvalidJsonNodeException("JsonNode validation failed: Following labels " + nullKeys.toString().replaceAll("[\\[\\]]", "") + " were not found");
            } else {
                buildRoutersServicesRecord(routerNameBuilder.toString(), serviceNameBuilder.toString());
            }
        } else {
            StringBuilder messageBuilder = new StringBuilder();
            if (routerNameBuilder.isEmpty()) {
                messageBuilder.append("JsonNode validation failed: Missing router name in labels");
            } else if (serviceNameBuilder.isEmpty()) {
                messageBuilder.append("JsonNode validation failed: Missing service name in labels");
            }
            throw new InvalidJsonNodeException(messageBuilder.toString());
        }

        fileNameBuilder.append(map.get("Hostname"));
        
    }

    private void buildRoutersServicesRecord(String routerName, String serviceName) {
            
            routers.put(routerName, new Router (
                mandatoryLabels.get("entrypoints").toString(),
                mandatoryLabels.get("rule").toString(),
                new TLS(),
                mandatoryLabels.get("service").toString()
            ));

            services.put(serviceName, new Service(
                new LoadBalancer(List.of(new Server(mandatoryLabels.get("url").toString())))
            ));

    }

    public void createFileConfig() {
        
        if (nullKeys.isEmpty()) {
            throw new IllegalStateException("This TraefikFileObject does not has valid FileConfig record");
        }

        FileConfig fileConfig = new FileConfig(
            new HTTPConfig(routers, services)
        );

        File yamlFile = new File(fileNameBuilder.toString() + ".yaml");
        
        try {
            yamlMapper.writeValue(yamlFile, fileConfig);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private Boolean areLabelsValid() {
        
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