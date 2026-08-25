package dev.edgegate.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class ApiKey {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, unique = true) private String name;
    @Column(nullable = false, unique = true) private String tokenHash;
    @Column(nullable = false) private String prefix;
    @Column(nullable = false) private boolean active = true;

    protected ApiKey() { }
    public ApiKey(String name, String tokenHash, String prefix) { this.name = name; this.tokenHash = tokenHash; this.prefix = prefix; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getTokenHash() { return tokenHash; }
    public String getPrefix() { return prefix; }
    public boolean isActive() { return active; }
    public void revoke() { active = false; }
}
