package dev.edgegate.domain;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface GatewayRouteRepository extends JpaRepository<GatewayRoute, UUID> { }
