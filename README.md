# EdgeGate

Cloudflare-inspired HTTP gateway with a Spring Boot control plane for routing traffic, protecting origins, and powering a self-service dashboard.

EdgeGate lets a team register origin services, route traffic by hostname and path, require API keys, enforce rate limits, and block abusive IP addresses from one control API. It is built as a portfolio-ready foundation for a Cloudflare-style dashboard.

## Run

```bash
docker compose up -d
../VeilRoute/gradlew bootRun
```

## Quick demo

Create an origin and route. Management endpoints are reserved under `/api/v1`; all other matching requests are proxied.

```bash
curl -X POST localhost:8080/api/v1/origins -H 'Content-Type: application/json' \
  -d '{"name":"httpbin","baseUrl":"https://httpbin.org"}'

curl -X POST localhost:8080/api/v1/routes -H 'Content-Type: application/json' \
  -d '{"pathPrefix":"/anything","originId":"<origin-id>","apiKeyRequired":false,"rateLimitPerMinute":60}'

curl localhost:8080/anything/hello
```

API key creation returns the raw token once. Send it to protected routes in `X-EdgeGate-Key`.

## Control API

- `GET|POST /api/v1/origins`
- `GET|POST /api/v1/routes`
- `GET|POST|DELETE /api/v1/api-keys`
- `GET|POST|DELETE /api/v1/security/blocked-ips`
- `GET /api/v1/dashboard/summary`

## MVP limits

Rate limiting is local to one application instance; production multi-instance deployment should move counters to Redis. The control API has no dashboard-user authentication yet, so it must not be exposed publicly as-is.
