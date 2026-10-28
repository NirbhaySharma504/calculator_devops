# Calculator REST API — End-to-End CI/CD Pipeline

CSE 816 Software Production Engineering — Mini-Project.

A Spring Boot calculator service, delivered by a fully automated pipeline:
**GitHub → Jenkins (build, test, image, push, deploy) → Docker Hub → Docker Compose (Nginx + API)**.
A `git push` to `main` builds, tests, containerises and redeploys the app with no manual steps.

| | |
|---|---|
| GitHub | https://github.com/NirbhaySharma504/calculator_devops |
| Docker Hub | https://hub.docker.com/r/nirbhaysharma504/calculator-api |
| Stack | Java 21 · Spring Boot 4.1 · Maven · JUnit 5 · Docker · Docker Compose · Nginx · Jenkins |

## Architecture

```
 developer ──git push──▶ GitHub ──webhook (via ngrok)──▶ Jenkins
                                                          │
        ┌───────────┬────────┬──────────────┬─────────┬───┴──────┐
        │ Checkout  │ Build  │ Test (JUnit) │ Docker  │  Push    │──▶ Docker Hub
        └───────────┴────────┴──────────────┴ Build   ┴──────────┘
                                                          │
                                       Deploy: docker compose up -d
                                                          ▼
                     host :8081 ──▶ [ nginx ] ──calc-net──▶ [ app :8080 ]
                                                          │
                                              email (success / failure)
```

## API

All endpoints are `GET` and return JSON.

| Operation | Endpoint | Example result |
|---|---|---|
| Add | `/api/v1/add?a=2&b=3` | `{"operation":"add","a":2.0,"b":3.0,"result":5.0}` |
| Subtract | `/api/v1/subtract?a=10&b=4` | `result: 6.0` |
| Multiply | `/api/v1/multiply?a=6&b=7` | `result: 42.0` |
| Divide | `/api/v1/divide?a=9&b=2` | `result: 4.5` |
| Modulo | `/api/v1/modulo?a=10&b=3` | `result: 1.0` |
| Power | `/api/v1/power?a=2&b=10` | `result: 1024.0` |
| Square root | `/api/v1/sqrt?a=81` | `{"operation":"sqrt","a":81.0,"result":9.0}` |
| Health | `/actuator/health` | `{"status":"UP", ...}` |
| Info | `/actuator/info` | app name + version |

### Errors

Invalid input returns **HTTP 400** with a structured body:

```json
{"timestamp":"2026-10-22T10:15:30Z","status":400,"error":"Bad Request",
 "message":"Division by zero is not allowed","path":"/api/v1/divide"}
```

Returned for: division/modulo by zero, square root of a negative number, missing parameter
(`?a=2`), non-numeric parameter (`?a=abc`), `NaN`/`Infinity` input, and results that overflow
(`power?a=10&b=400`).

## Build and test

Requires JDK 21+ and Maven 3.9+.

```bash
mvn clean test          # 57 unit + integration tests, JUnit XML in target/surefire-reports/
mvn clean package       # builds target/calculator.jar
java -jar target/calculator.jar
curl "http://localhost:8080/api/v1/add?a=2&b=3"
```

Tests:
- `CalculatorServiceTest`: JUnit 5 unit tests for every operation and edge case (divide by zero, overflow, NaN).
- `CalculatorControllerIntegrationTest`: `@SpringBootTest` + MockMvc, hitting the real endpoints, error bodies and `/actuator/health`.

## Run with Docker

```bash
docker build -t nirbhaysharma504/calculator-api:latest .
docker run -d -p 9090:8080 --name calc nirbhaysharma504/calculator-api:latest
curl "http://localhost:9090/api/v1/multiply?a=6&b=7"
```

The image is multi-stage (Maven/JDK build stage → `eclipse-temurin:21-jre-alpine` runtime), runs
as a non-root user, exposes 8080 and has a built-in `HEALTHCHECK`.

## Run the full stack with Docker Compose

```bash
docker compose up -d                 # Nginx on host port 8081 → app (internal only)
docker compose ps                    # both services should be "healthy"
curl "http://localhost:8081/api/v1/divide?a=1&b=0"
docker compose down
```

Variables (see `.env.example`): `IMAGE_TAG` (default `latest`), `NGINX_PORT` (default `8081`),
`SPRING_PROFILES_ACTIVE`, `JAVA_TOOL_OPTIONS`.

## CI/CD

The declarative [`Jenkinsfile`](Jenkinsfile) runs on every push (GitHub webhook, with Poll SCM as fallback):

1. **Checkout** — pulls the pushed commit.
2. **Build** — `mvn clean package -DskipTests`.
3. **Test** — `mvn test`; JUnit results are published and a failing test fails the build.
4. **Docker Build** — image tagged `:<BUILD_NUMBER>` and `:latest`.
5. **Push** — to Docker Hub using the Jenkins credential `dockerhub-creds`.
6. **Deploy** — `IMAGE_TAG=<BUILD_NUMBER> docker compose up -d`, then waits for `/actuator/health` and runs smoke tests through Nginx.

An email is sent on success and on failure. Full setup instructions are in [GUIDE.md](GUIDE.md).

## Repository layout

```
calculator-devops/
├── src/main/java/com/spe/calculator/   # controller, service, dto, exception handler
├── src/test/java/com/spe/calculator/   # unit + MockMvc integration tests
├── pom.xml
├── Dockerfile                          # multi-stage build
├── .dockerignore
├── docker-compose.yml                  # app + nginx
├── nginx/nginx.conf                    # reverse proxy
├── Jenkinsfile                         # CI/CD pipeline
├── .env.example                        # compose overrides (no secrets)
├── GUIDE.md                            # step-by-step setup, explanations, demo script
└── report/                             # project report
```
