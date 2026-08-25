package dev.edgegate.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class BlockedIp {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, unique = true) private String address;
    @Column(nullable = false) private String reason;

    protected BlockedIp() { }
    public BlockedIp(String address, String reason) { this.address = address; this.reason = reason; }
    public UUID getId() { return id; }
    public String getAddress() { return address; }
    public String getReason() { return reason; }
}
