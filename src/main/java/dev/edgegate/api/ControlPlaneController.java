package dev.edgegate.api;

import dev.edgegate.domain.*;
import dev.edgegate.gateway.ApiKeyService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ControlPlaneController {
    private final OriginRepository origins;
    private final GatewayRouteRepository routes;
    private final ApiKeyRepository keys;
    private final ApiKeyService apiKeyService;
    private final BlockedIpRepository blockedIps;

    public ControlPlaneController(OriginRepository origins, GatewayRouteRepository routes, ApiKeyRepository keys, ApiKeyService apiKeyService, BlockedIpRepository blockedIps) {
        this.origins = origins; this.routes = routes; this.keys = keys; this.apiKeyService = apiKeyService; this.blockedIps = blockedIps;
    }

    @GetMapping("/origins") public List<Origin> origins() { return origins.findAll(); }
    @PostMapping("/origins") @ResponseStatus(HttpStatus.CREATED)
    public Origin createOrigin(@Valid @RequestBody CreateOrigin request) {
        URI uri;
        try { uri = URI.create(request.baseUrl()); } catch (IllegalArgumentException e) { throw badRequest("baseUrl must be a valid URL"); }
        if (uri.getScheme() == null || uri.getHost() == null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) throw badRequest("baseUrl must be an http(s) URL");
        return origins.save(new Origin(request.name(), request.baseUrl().replaceAll("/$", "")));
    }
    @PatchMapping("/origins/{id}/enabled") @Transactional
    public Origin setOriginEnabled(@PathVariable UUID id, @RequestBody Enabled request) {
        Origin origin = origins.findById(id).orElseThrow(() -> notFound("Origin")); origin.setEnabled(request.enabled()); return origin;
    }

    @GetMapping("/routes") public List<GatewayRoute> routes() { return routes.findAll(); }
    @PostMapping("/routes") @ResponseStatus(HttpStatus.CREATED)
    public GatewayRoute createRoute(@Valid @RequestBody CreateRoute request) {
        Origin origin = origins.findById(request.originId()).orElseThrow(() -> notFound("Origin"));
        return routes.save(new GatewayRoute(request.hostname(), normalizedPrefix(request.pathPrefix()), origin, request.apiKeyRequired(), request.rateLimitPerMinute()));
    }
    @PatchMapping("/routes/{id}/enabled") @Transactional
    public GatewayRoute setRouteEnabled(@PathVariable UUID id, @RequestBody Enabled request) {
        GatewayRoute route = routes.findById(id).orElseThrow(() -> notFound("Route")); route.setEnabled(request.enabled()); return route;
    }

    @GetMapping("/api-keys") public List<KeyView> apiKeys() { return keys.findAll().stream().map(key -> new KeyView(key.getId(), key.getName(), key.getPrefix(), key.isActive())).toList(); }
    @PostMapping("/api-keys") @ResponseStatus(HttpStatus.CREATED)
    public CreatedKey createKey(@Valid @RequestBody CreateKey request) {
        ApiKeyService.CreatedKey created = apiKeyService.create(request.name());
        return new CreatedKey(created.key().getId(), created.key().getName(), created.token());
    }
    @DeleteMapping("/api-keys/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void revokeKey(@PathVariable UUID id) { keys.findById(id).orElseThrow(() -> notFound("API key")).revoke(); }

    @GetMapping("/security/blocked-ips") public List<BlockedIp> blockedIps() { return blockedIps.findAll(); }
    @PostMapping("/security/blocked-ips") @ResponseStatus(HttpStatus.CREATED)
    public BlockedIp blockIp(@Valid @RequestBody BlockIp request) { return blockedIps.save(new BlockedIp(request.address(), request.reason())); }
    @DeleteMapping("/security/blocked-ips/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblockIp(@PathVariable UUID id) { blockedIps.deleteById(id); }

    @GetMapping("/dashboard/summary")
    public Summary summary() { return new Summary(origins.count(), routes.count(), keys.count(), blockedIps.count()); }

    private static String normalizedPrefix(String prefix) { return prefix.length() > 1 && prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix; }
    private static ResponseStatusException notFound(String resource) { return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found"); }
    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    public record CreateOrigin(@NotBlank String name, @NotBlank String baseUrl) { }
    public record CreateRoute(String hostname, @NotBlank @Pattern(regexp = "/.*") String pathPrefix, @NotNull UUID originId, boolean apiKeyRequired, @Min(0) @Max(1_000_000) int rateLimitPerMinute) { }
    public record CreateKey(@NotBlank String name) { }
    public record CreatedKey(UUID id, String name, String token) { }
    public record KeyView(UUID id, String name, String prefix, boolean active) { }
    public record BlockIp(@NotBlank String address, @NotBlank String reason) { }
    public record Enabled(boolean enabled) { }
    public record Summary(long origins, long routes, long apiKeys, long blockedIps) { }
}
