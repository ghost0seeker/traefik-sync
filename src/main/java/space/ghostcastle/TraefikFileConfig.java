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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import space.ghostcastle.Records.MandatoryLabels;


public class TraefikFileConfig {

    private Map<String, FileConfig> fileConfigs = new HashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private final Pattern routerNameRegex = Pattern.compile("traefik\\.http\\.routers\\.([a-zA-Z0-9-]+)\\..*");
    private Matcher routerNameMatcher;
    private StringBuilder routerNameBuilder = new StringBuilder();

    private final Pattern serviceNameRegex = Pattern.compile("traefik\\.http\\.services\\.([a-zA-Z0-9-]+)\\..*");
    private Matcher serviceNameMatcher;
    private StringBuilder serviceNameBuilder = new StringBuilder();

    private final Pattern hostNameRegex = Pattern.compile("\\W*(\\w+)\\W*");
    private final Pattern labelRegex = Pattern.compile("(?i)traefik\\.http\\.(?:routers|services)\\.[\\w\\d-]+\\.(?:(?<entrypoints>entrypoints)|(?<rule>rule)|(?<tls>tls))$");

    private Matcher labelMatcher;

    private List<String> nullKeys = new ArrayList<>();

    public void createConfig(JsonNode node) {
        List<String> entrypoints = new ArrayList<>();
        List<String> url = new ArrayList<>();
        StringBuilder rule = new StringBuilder();
        StringBuilder tls = new StringBuilder();
        StringBuilder service = new StringBuilder();

        JsonNode labels = node.path("Labels");
        Map<String, String> labelMap = mapper.convertValue(
            labels,
            new TypeReference<Map<String, String>>() {}
        );

        List<String> routerKeySet = labelMap.keySet().stream().filter(k -> k.startsWith("traefik")).toList();
        
        for (String key : routerKeySet) {
            routerNameMatcher = routerNameRegex.matcher(key);
            serviceNameMatcher = serviceNameRegex.matcher(key);
            
            if (routerNameMatcher.matches()) {
                if (routerNameBuilder.isEmpty()) {
                    routerNameBuilder.append(routerNameMatcher.group(1));
                }
                
                labelMatcher = labelRegex.matcher(key); 
                
                if (labelMatcher.find()) {  
                    if (labelMatcher.group("entrypoints") != null) {
                         List<String> ePList = getListFromString(labelMap.get(key));
                         if (ePList != null) {
                            entrypoints.addAll(ePList);
                         }
                    } else if (labelMatcher.group("rule") != null) {
                        rule.append(labelMap.get(key));
                    } else if (labelMatcher.group("tls") != null) {
                        tls.append(labelMap.get(key));
                    }
                }
            }
            
            if (serviceNameMatcher.find()) {
                if (serviceNameBuilder.isEmpty()) {
                    serviceNameBuilder.append(serviceNameMatcher.group(1).trim());
                }
                service.append(serviceNameBuilder.toString());
                List<String> urlList = getListFromString(labelMap.get(key));
                if (urlList != null) {
                    url.addAll(urlList);
                }
                labelMatcher = labelRegex.matcher(key); 
                if (labelMatcher.find()) {  
                }
            }
        }
        
        if (!routerNameBuilder.isEmpty() || !serviceNameBuilder.isEmpty()) {
            if (!areLabelsValid(entrypoints, rule, tls, service, url)) {
                throw new InvalidJsonNodeException("JsonNode validation failed: Following labels " + nullKeys.toString().replaceAll("[\\[\\]]", "") + " were not found");
            } else {
                
                MandatoryLabels mandatoryLabels = getMandatoryLabelsRecord();
                
                mandatoryLabels.entrypoints().addAll(entrypoints);
                mandatoryLabels.urls().addAll(url);
                mandatoryLabels.rule().append(rule.toString());
                mandatoryLabels.tls().append(tls.toString());
                mandatoryLabels.service().append(service.toString());

                HTTPConfig httpConfig = buildHTTPConfigRecord(routerNameBuilder.toString(), serviceNameBuilder.toString(), mandatoryLabels);
                String proxyHostName = node.path("Hostname").toPrettyString();
                Matcher proxyNameMatcher = hostNameRegex.matcher(proxyHostName);
                
                boolean didNameMatch = proxyNameMatcher.find();
                if (didNameMatch) {
                    proxyHostName = proxyNameMatcher.group(1);
                }

                FileConfig fileConfig = new FileConfig(httpConfig);
                fileConfigs.put(proxyHostName, fileConfig);
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
    }

    private HTTPConfig buildHTTPConfigRecord(String routerName, String serviceName, MandatoryLabels mandatoryLabels) {
        boolean tls = false;

        if (Boolean.valueOf(mandatoryLabels.tls().toString())) {
            tls = true;
        }

        Router router = new Router (
            mandatoryLabels.entrypoints(),
            mandatoryLabels.rule().toString(),
            tls ? new TLS() : null,
            mandatoryLabels.service().toString()
        );

        Service service =  new Service(
            new LoadBalancer(List.of(new Server(mandatoryLabels.urls())))
        );

        return new HTTPConfig(Map.of(routerName, router), Map.of(serviceName, service));
    }

    public FileConfig getConfig(String proxyHostName) {
        return fileConfigs.get(proxyHostName);
    }

    private static List<String> getListFromString(String str) {
        String[] strArr = str.trim().split(",");
        List<String> strList = List.of(strArr);
        return strList.stream().map(String::trim).toList();
    }

    public void createFileConfig(String proxyHostName) {
        
        if (!nullKeys.isEmpty()) {
            throw new IllegalStateException("This TraefikFileObject does not has valid FileConfig record");
        }

        FileConfig fileConfig = getConfig(proxyHostName);

        File yamlFile = new File(proxyHostName + ".yaml");
        
        try {
            yamlMapper.writeValue(yamlFile, fileConfig);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private Boolean areLabelsValid(List<String> entrypoints, StringBuilder rule, StringBuilder tls, StringBuilder service, List<String> url) {
        
        if (
            entrypoints.isEmpty() ||
            rule.isEmpty() ||
            service.isEmpty() ||
            url.isEmpty()
        ) {
            return false;
        } else {
            return true;
        }
    }

    private static MandatoryLabels getMandatoryLabelsRecord () {
        return new MandatoryLabels(
            new ArrayList<>(),
            new StringBuilder(),
            new StringBuilder(),
            new StringBuilder(),
            new ArrayList<>()
        );
    }
}