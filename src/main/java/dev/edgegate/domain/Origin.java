package dev.edgegate.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class Origin {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, unique = true) private String name;
    @Column(nullable = false) private String baseUrl;
    @Column(nullable = false) private boolean enabled = true;

    protected Origin() { }
    public Origin(String name, String baseUrl) { this.name = name; this.baseUrl = baseUrl; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getBaseUrl() { return baseUrl; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
