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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import space.ghostcastle.Exceptions.InvalidJsonNodeException;
import space.ghostcastle.Records.DockerProxyRecord;
import space.ghostcastle.Records.FileConfig;
import space.ghostcastle.Records.FileConfig.HTTPConfig;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Router;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Router.TLS;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service.LoadBalancer;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service.LoadBalancer.Server;
import space.ghostcastle.Records.MandatoryLabels;


public class TraefikFileConfig {

    private final Map<String, FileConfig> fileConfigs = new HashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private final Pattern routerNameRegex = Pattern.compile("traefik\\.http\\.routers\\.([a-zA-Z0-9-]+)\\..*");

    private final Pattern serviceNameRegex = Pattern.compile("traefik\\.http\\.services\\.([a-zA-Z0-9-]+)\\..*");

    private final Pattern labelRegex = Pattern.compile("(?i)traefik\\.http\\.(?:routers|services)\\.[\\w\\d-]+\\.(?:(?<entrypoints>entrypoints)|(?<rule>rule)|(?<tls>tls))$");

    private final List<String> nullKeys = new ArrayList<>();

    public void createConfig(DockerProxyRecord proxyRecord) throws InvalidJsonNodeException {

        String proxyName = proxyRecord.name();
        ArrayNode containers = proxyRecord.containers();

        HTTPConfig httpConfig = new HTTPConfig(
            new HashMap<>(),
            new HashMap<>()
        );

        int failedContainers = 0;

        for (JsonNode container : containers) {

            List<String> entrypoints = new ArrayList<>();
            List<String> url = new ArrayList<>();
            StringBuilder rule = new StringBuilder();
            StringBuilder tls = new StringBuilder();
            StringBuilder service = new StringBuilder();
            StringBuilder containerName = new StringBuilder();

            containerName.append(container.path("Name"));

            StringBuilder routerNameBuilder = new StringBuilder();
            StringBuilder serviceNameBuilder = new StringBuilder();

            Matcher routerNameMatcher;
            Matcher serviceNameMatcher;

            JsonNode labels = container.path("Labels");
            Map<String, String> labelMap = mapper.convertValue(
                labels,
                new TypeReference<Map<String, String>>() {}
            );

            try {

                List<String> routerKeySet = labelMap.keySet().stream().filter(k -> k.startsWith("traefik")).toList();

                if (routerKeySet.size() < 4) {
                    throw new InvalidJsonNodeException(proxyName + " does not have valid traefik labels");
                }

                for (String key : routerKeySet) {
                    routerNameMatcher = routerNameRegex.matcher(key);
                    serviceNameMatcher = serviceNameRegex.matcher(key);

                    if (routerNameMatcher.matches()) {
                        if (routerNameBuilder.isEmpty()) {
                            routerNameBuilder.append(routerNameMatcher.group(1));
                        }

                        Matcher labelMatcher = labelRegex.matcher(key);

                        if (labelMatcher.find()) {
                            if (labelMatcher.group("entrypoints") != null) {
                                List<String> ePList = getListFromString(labelMap.get(key));
                                entrypoints.addAll(ePList);
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
                        url.addAll(urlList);
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
                        mandatoryLabels.containerName().append(containerName.toString());
                        Router routers = getNewRouterRecord(mandatoryLabels);
                        Service services = getServiceRecord(mandatoryLabels);

                        httpConfig.routers().put(routerNameBuilder.toString(), routers);
                        httpConfig.services().put(serviceNameBuilder.toString(), services);
                    }

                } else {
                    throw new InvalidJsonNodeException("JsonNode validation failed: Router or Service Names not found for " + containerName.toString() );
                }

            } catch (InvalidJsonNodeException e) {
                System.out.println(e.getMessage());
                failedContainers++;
            }
        }

        System.out.println(proxyName + ": No of Containers: " + containers.size() + ((failedContainers != 0) ? "\nFailed Containers: " + failedContainers : ""));
        fileConfigs.put(proxyName, new FileConfig(httpConfig));
    }

    private Router getNewRouterRecord(MandatoryLabels mandatoryLabels) {
        boolean tls = Boolean.parseBoolean(mandatoryLabels.tls().toString());

        return new Router (
            mandatoryLabels.entrypoints(),
            mandatoryLabels.rule().toString(),
            tls ? new TLS() : null,
            mandatoryLabels.service().toString()
        );
    }

    private Service getServiceRecord(MandatoryLabels mandatoryLabels) {

        return new Service(
            new LoadBalancer(List.of(new Server(mandatoryLabels.urls())))
        );
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
        
        if (entrypoints.isEmpty()) {
            nullKeys.add("entrypoints");
        } else if (rule.isEmpty()) {
            nullKeys.add("rule");
        } else if (tls.isEmpty()) {
            nullKeys.add("tls");
        } else if (url.isEmpty()) {
            nullKeys.add("url");
        }

        return nullKeys.isEmpty();

    }

    private static MandatoryLabels getMandatoryLabelsRecord () {
        return new MandatoryLabels(
            new ArrayList<>(),
            new StringBuilder(),
            new StringBuilder(),
            new StringBuilder(),
            new ArrayList<>(),
            new StringBuilder()
        );
    }
}