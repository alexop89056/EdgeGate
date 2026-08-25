package dev.edgegate.gateway;

import dev.edgegate.domain.GatewayRoute;
import dev.edgegate.domain.GatewayRouteRepository;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RouteResolver {
    private final GatewayRouteRepository routes;
    public RouteResolver(GatewayRouteRepository routes) { this.routes = routes; }

    public Optional<GatewayRoute> resolve(String host, String path) {
        return routes.findAll().stream()
                .filter(GatewayRoute::isEnabled)
                .filter(route -> route.getOrigin().isEnabled())
                .filter(route -> hostnameMatches(route.getHostname(), host))
                .filter(route -> pathMatches(route.getPathPrefix(), path))
                .max(Comparator.comparingInt(route -> route.getPathPrefix().length()));
    }

    static boolean hostnameMatches(String configuredHost, String requestHost) {
        return configuredHost == null || configuredHost.isBlank() || configuredHost.equalsIgnoreCase(requestHost);
    }

    static boolean pathMatches(String prefix, String path) {
        if ("/".equals(prefix)) return true;
        return path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/");
    }
}
