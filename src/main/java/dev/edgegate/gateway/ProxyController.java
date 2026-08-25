package dev.edgegate.gateway;

import dev.edgegate.domain.BlockedIpRepository;
import dev.edgegate.domain.GatewayRoute;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
class ProxyController {
    private static final Set<String> HOP_BY_HOP = Set.of("connection", "keep-alive", "proxy-authenticate", "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade", "host", "content-length");
    private final RouteResolver routeResolver;
    private final BlockedIpRepository blockedIps;
    private final ApiKeyService apiKeys;
    private final RateLimiter rateLimiter;
    private final WebClient client;

    ProxyController(RouteResolver routeResolver, BlockedIpRepository blockedIps, ApiKeyService apiKeys, RateLimiter rateLimiter, WebClient.Builder client) {
        this.routeResolver = routeResolver; this.blockedIps = blockedIps; this.apiKeys = apiKeys; this.rateLimiter = rateLimiter; this.client = client.build();
    }

    @RequestMapping("/**")
    Mono<ResponseEntity<byte[]>> proxy(ServerHttpRequest request) {
        String clientIp = clientIp(request);
        String host = request.getHeaders().getHost() == null ? "" : request.getHeaders().getHost().getHostString();
        String path = request.getURI().getRawPath();
        return Mono.fromCallable(() -> blockedIps.existsByAddress(clientIp))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(blocked -> blocked ? Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(bytes("IP is blocked"))) :
                        Mono.fromCallable(() -> routeResolver.resolve(host, path)).subscribeOn(Schedulers.boundedElastic()).flatMap(route -> route
                                .<Mono<ResponseEntity<byte[]>>>map(value -> forward(request, value, clientIp))
                                .orElseGet(() -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(bytes("No gateway route matches this request"))))));
    }

    private Mono<ResponseEntity<byte[]>> forward(ServerHttpRequest request, GatewayRoute route, String clientIp) {
        if (route.isApiKeyRequired() && !apiKeys.valid(request.getHeaders().getFirst("X-EdgeGate-Key"))) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(bytes("A valid X-EdgeGate-Key is required")));
        }
        if (!rateLimiter.allowed(route.getId() + ":" + clientIp, route.getRateLimitPerMinute())) {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "60").body(bytes("Rate limit exceeded")));
        }
        URI target = target(route, request);
        return client.method(request.getMethod()).uri(target)
                .headers(headers -> copyRequestHeaders(request.getHeaders(), headers, clientIp))
                .body(request.getBody(), org.springframework.core.io.buffer.DataBuffer.class)
                .exchangeToMono(response -> response.bodyToMono(byte[].class).defaultIfEmpty(new byte[0]).map(body -> {
                    HttpHeaders headers = new HttpHeaders();
                    response.headers().asHttpHeaders().forEach((name, values) -> { if (!HOP_BY_HOP.contains(name.toLowerCase())) headers.put(name, List.copyOf(values)); });
                    headers.set("X-EdgeGate-Origin", route.getOrigin().getName());
                    return new ResponseEntity<>(body, headers, response.statusCode());
                }))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(bytes("Origin is unavailable"))));
    }

    private URI target(GatewayRoute route, ServerHttpRequest request) {
        String base = route.getOrigin().getBaseUrl().replaceAll("/$", "");
        String query = request.getURI().getRawQuery();
        return URI.create(base + request.getURI().getRawPath() + (query == null ? "" : "?" + query));
    }
    private void copyRequestHeaders(HttpHeaders source, HttpHeaders target, String clientIp) {
        source.forEach((name, values) -> { if (!HOP_BY_HOP.contains(name.toLowerCase()) && !name.equalsIgnoreCase("X-Forwarded-For")) target.put(name, List.copyOf(values)); });
        target.set("X-Forwarded-For", clientIp);
    }
    private String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddress().getAddress().getHostAddress() : forwarded.split(",")[0].trim();
    }
    private static byte[] bytes(String message) { return message.getBytes(java.nio.charset.StandardCharsets.UTF_8); }
}
