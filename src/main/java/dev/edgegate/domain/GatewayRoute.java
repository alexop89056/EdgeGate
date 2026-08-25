package dev.edgegate.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "gateway_routes")
public class GatewayRoute {
    @Id @GeneratedValue private UUID id;
    private String hostname;
    @Column(nullable = false) private String pathPrefix;
    @ManyToOne(optional = false) private Origin origin;
    @Column(nullable = false) private boolean enabled = true;
    @Column(nullable = false) private boolean apiKeyRequired;
    @Column(nullable = false) private int rateLimitPerMinute;

    protected GatewayRoute() { }
    public GatewayRoute(String hostname, String pathPrefix, Origin origin, boolean apiKeyRequired, int rateLimitPerMinute) {
        this.hostname = hostname; this.pathPrefix = pathPrefix; this.origin = origin;
        this.apiKeyRequired = apiKeyRequired; this.rateLimitPerMinute = rateLimitPerMinute;
    }
    public UUID getId() { return id; }
    public String getHostname() { return hostname; }
    public String getPathPrefix() { return pathPrefix; }
    public Origin getOrigin() { return origin; }
    public boolean isEnabled() { return enabled; }
    public boolean isApiKeyRequired() { return apiKeyRequired; }
    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
