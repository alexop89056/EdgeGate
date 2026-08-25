package dev.edgegate.gateway;

import dev.edgegate.domain.ApiKey;
import dev.edgegate.domain.ApiKeyRepository;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {
    private final ApiKeyRepository keys;
    private final SecureRandom random = new SecureRandom();
    public ApiKeyService(ApiKeyRepository keys) { this.keys = keys; }

    public CreatedKey create(String name) {
        byte[] bytes = new byte[24]; random.nextBytes(bytes);
        String token = "eg_" + HexFormat.of().formatHex(bytes);
        ApiKey key = keys.save(new ApiKey(name, sha256(token), token.substring(0, 11)));
        return new CreatedKey(key, token);
    }
    public boolean valid(String token) { return token != null && keys.existsByTokenHashAndActiveTrue(sha256(token)); }
    public static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes())); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    public record CreatedKey(ApiKey key, String token) { }
}
