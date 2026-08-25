package dev.edgegate.domain;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    boolean existsByTokenHashAndActiveTrue(String tokenHash);
}
