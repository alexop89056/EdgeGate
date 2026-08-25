package dev.edgegate.gateway;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    public boolean allowed(String key, int limit) {
        if (limit <= 0) return true;
        long minute = Instant.now(clock).getEpochSecond() / 60;
        Window window = windows.compute(key, (ignored, current) -> current == null || current.minute != minute
                ? new Window(minute, 1) : new Window(minute, current.count + 1));
        return window.count <= limit;
    }

    private record Window(long minute, int count) { }
}
