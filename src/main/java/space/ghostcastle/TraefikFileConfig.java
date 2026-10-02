package space.ghostcastle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import space.ghostcastle.Exceptions.InvalidJsonNodeException;
import space.ghostcastle.Records.FileConfig.*;
import space.ghostcastle.Records.FileConfig.HTTPConfig.*;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service.*;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Service.LoadBalancer.*;
import space.ghostcastle.Records.FileConfig.HTTPConfig.Router.*;

public class TraefikFileConfig {

    private List<Router> routers = new ArrayList<>();

    private static ObjectMapper mapper = new ObjectMapper();

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
        boolean isEmpty = routerKeys.isEmpty();
        if (!isEmpty) {
           List<String> entryPoints = new ArrayList<>();
           StringBuilder rule = new StringBuilder();
           StringBuilder serviceStr = new StringBuilder();
           StringBuilder routerName = new StringBuilder();
           StringBuilder serviceName = new StringBuilder();
           StringBuilder tlsStr = new StringBuilder();
           List<String> servers = new ArrayList<>();

           for ( String key : routerKeys) {
            
           }

            Server server = new Server(servers.get(0));
            LoadBalancer loadBalancer = new LoadBalancer(List.of(server));
            Service service = new Service(loadBalancer);
            Router router;
            if (Boolean.parseBoolean(tlsStr.toString())) {
                TLS tls = new TLS();
                router = new Router(entryPoints, rule.toString(), tls, service);
                routers.add(router);
            }
        }
    }
}
