package dev.edgegate.domain;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BlockedIpRepository extends JpaRepository<BlockedIp, UUID> {
    boolean existsByAddress(String address);
}
