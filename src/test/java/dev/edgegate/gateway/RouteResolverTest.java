package dev.edgegate.gateway;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RouteResolverTest {
    @Test void hostnameCanBeOptionalOrExact() {
        assertTrue(RouteResolver.hostnameMatches(null, "api.example.com"));
        assertTrue(RouteResolver.hostnameMatches("API.example.com", "api.example.com"));
        assertFalse(RouteResolver.hostnameMatches("app.example.com", "api.example.com"));
    }
    @Test void pathPrefixHonoursSegmentBoundary() {
        assertTrue(RouteResolver.pathMatches("/api", "/api"));
        assertTrue(RouteResolver.pathMatches("/api", "/api/users"));
        assertFalse(RouteResolver.pathMatches("/api", "/apiary"));
    }
}
