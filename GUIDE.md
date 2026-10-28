# GUIDE — How to finish, run, explain and demo this project

This is your working manual. The code is already done. What's left is **your accounts, Jenkins
setup, GitHub pushes, screenshots, the report and the demo**. Work through the parts in order.

- **Part A**: day-by-day plan up to the demo (Mon 26-Oct)
- **Part B**: one-time setup (GitHub, Docker Hub, Jenkins, ngrok, email, webhook)
- **Part C**: how every file works, so you can explain it in the viva
- **Part D**: demo script for Class 17
- **Part E**: troubleshooting
- **Part F**: likely viva questions
- **Part G**: screenshot checklist
- **Part H**: how to write the report

---

## Part A: Day-by-day plan

The brief penalises "one big commit the night before", so push **progressively**. The local repo
already has small commits on five stacked branches:

```
main                     Initial Spring Boot skeleton
└─ feature/calculator-api   service, unit tests, controller, error handling, integration tests
   └─ feature/docker           .dockerignore, multi-stage Dockerfile
      └─ feature/compose-nginx    nginx.conf, docker-compose.yml, .env.example
         └─ feature/jenkins-pipeline Jenkinsfile
            └─ docs/readme-report       README, GUIDE, report
```

| Day | Do this |
|---|---|
| **Thu 22** | B1 (GitHub repo), push `main` + `feature/calculator-api`, open PR #1 and merge it. Run `mvn test` yourself and read Part C1. |
| **Fri 23** | Push `feature/docker`, PR #2 and merge. Push `feature/compose-nginx`, PR #3 and merge. Try `docker compose up -d` and curl it. Read C2 and C3. |
| **Sat 24** | B2 to B7 (Docker Hub, Jenkins, ngrok, email, job, webhook). Push `feature/jenkins-pipeline`, PR #4 and merge. **That merge should trigger the first automatic pipeline run.** Fix whatever breaks (Part E). Read C4. |
| **Sun 25** | Push `docs/readme-report`, PR #5 and merge. Take all screenshots (Part G). Fill the report and export it to PDF (Part H). Rehearse the demo twice (Part D). |
| **Mon 26** | Before class: start Docker Desktop, Jenkins and ngrok, and check that the webhook is green. Run the demo. |

> Merge PRs with **"Create a merge commit"** (not squash) so the branch history stays visible
> and the next stacked PR shows only its own changes.

---

## Part B: One-time setup

> Everything here involves your passwords and tokens, so you have to do it yourself.
> Never paste a token into a file in the repo.

### B1. GitHub repository and the first push

1. On github.com, create a new **public** repo named `calculator_devops`. Leave it empty: no README, no .gitignore.
2. Push `main` and the first feature branch:

```bash
cd "/Users/nirbhay/Desktop/Study/Sem 7/SPE/calculator-devops"
```
```bash
git remote add origin https://github.com/NirbhaySharma504/calculator_devops.git
```
```bash
git push -u origin main feature/calculator-api
```

3. On GitHub: **Compare & pull request** → base `main` ← compare `feature/calculator-api`.
   Write a short description, then **Create pull request** → **Merge pull request** (merge commit).
4. Pull the merge back locally: `git checkout main && git pull`.
5. Repeat the push, PR and merge for each next branch on its planned day:
   `git push -u origin feature/docker`, and so on.

When `git push` asks for a password, GitHub needs a **Personal Access Token**, not your account
password. Alternatively, install the GitHub CLI (`brew install gh`, then `gh auth login`) and
create PRs with `gh pr create --base main --head feature/docker --fill`.

### B2. Docker Hub

1. Sign in to hub.docker.com → **Create repository** → name `calculator-api`, visibility **Public**.
2. Account settings → **Personal access tokens** → **Generate new token**. Set the access to
   **Read & Write** and copy the token. You will paste it into Jenkins in B4.

### B3. Start Jenkins (Homebrew)

```bash
brew services start jenkins-lts
```

1. Open http://localhost:8080. Get the unlock password with:
   ```bash
   cat ~/.jenkins/secrets/initialAdminPassword
   ```
2. Choose **Install suggested plugins**. This covers Git, GitHub, Pipeline, Credentials Binding,
   Email Extension, Timestamper and JUnit.
3. Create your admin user.
4. **Manage Jenkins → Plugins → Available**: install **Pipeline: Stage View** if it isn't
   already installed. It gives the green stage grid for your screenshot.
5. Check that Jenkins can see your tools. Open **Manage Jenkins → Script Console** and run
   `println "which mvn docker".execute().text`. It may print nothing, because Homebrew Jenkins has
   a minimal PATH. That's expected: the `Jenkinsfile` adds `/opt/homebrew/bin` and `/usr/local/bin` itself.

Jenkins runs on **port 8080**. That's why the app is published on **8081** through Nginx.

### B4. Docker Hub credential in Jenkins

**Manage Jenkins → Credentials → System → Global credentials → Add Credentials**

| Field | Value |
|---|---|
| Kind | Username with password |
| Username | `nirbhaysharma504` |
| Password | the Docker Hub **access token** from B2 |
| ID | `dockerhub-creds` (this must match exactly, because the Jenkinsfile uses it) |

### B5. Expose Jenkins with ngrok (so the GitHub webhook can reach it)

GitHub runs on the internet and can't reach `localhost:8080`. ngrok gives Jenkins a public HTTPS URL.

```bash
brew install ngrok
```
1. Sign up at ngrok.com and copy your authtoken from the dashboard. Then run:
   `ngrok config add-authtoken <YOUR_TOKEN>`
2. Dashboard → **Domains**: claim your **free static domain**, e.g. `something.ngrok-free.app`.
   It keeps the same URL every time, so you never have to edit the webhook.
3. Run this, and keep it running while you work and during the demo:
   ```bash
   ngrok http --url=<your-domain>.ngrok-free.app 8080
   ```
4. In Jenkins: **Manage Jenkins → System → Jenkins Location → Jenkins URL** =
   `https://<your-domain>.ngrok-free.app/`. This makes the links in emails correct.

### B6. Email notifications (Gmail)

1. Your Google account needs **2-Step Verification** turned on. Then go to
   myaccount.google.com → Security → **App passwords** → create one named "Jenkins" and copy
   the 16-character password.
2. **Manage Jenkins → Credentials → Add** → Username with password:
   username = your Gmail address, password = the app password, ID = `gmail-smtp`.
3. **Manage Jenkins → System**:
   - **Jenkins Location → System Admin e-mail address**: your Gmail address. Gmail rejects
     mail whose "from" address differs from the account.
   - **Global properties → Environment variables → Add**: name `NOTIFY_EMAIL`, value = the
     address that should receive build mails. Keeping it in Jenkins means your email isn't in
     the public repo.
   - **Extended E-mail Notification**: SMTP server `smtp.gmail.com`, port `465`.
     Under Advanced, pick the credential `gmail-smtp` and tick **Use SSL**.
     Default Content Type: HTML.
   - Optional, for the **Test configuration** button: fill the classic **E-mail Notification**
     section the same way (smtp.gmail.com, Use SMTP Authentication, Use SSL, port 465), then
     send a test mail.
4. Save.

### B7. Create the pipeline job

1. **New Item** → name `calculator-devops` → **Pipeline** → OK.
2. **Triggers**: tick **GitHub hook trigger for GITScm polling**.
3. **Pipeline**: Definition **Pipeline script from SCM** → SCM **Git** →
   Repository URL `https://github.com/NirbhaySharma504/calculator_devops.git`.
   The repo is public, so no credentials are needed. Branch `*/main`, Script Path `Jenkinsfile`.
4. Save, then click **Build Now** once. The first manual run is what registers the `triggers {}`
   block from the Jenkinsfile. After that, pushes trigger builds by themselves.

### B8. GitHub webhook

In the GitHub repo: **Settings → Webhooks → Add webhook**

| Field | Value |
|---|---|
| Payload URL | `https://<your-domain>.ngrok-free.app/github-webhook/` (the trailing `/` matters) |
| Content type | `application/json` |
| Events | Just the push event |

After saving, open the webhook → **Recent Deliveries**. The ping should show a green ✓ with
response 200. Take a screenshot of it.

**Fallback:** if ngrok misbehaves, the Jenkinsfile also polls GitHub every 5 minutes
(`pollSCM('H/5 * * * *')`). The brief accepts that.

### B9. Verify end-to-end

```bash
git checkout main && git pull
```
Make a tiny change (e.g. a line in README), then commit and push it. Within seconds you
should see:
- a new build start in Jenkins, triggered by "Started by GitHub push by NirbhaySharma504",
- all 6 stages green,
- the new tag on Docker Hub (`:<build number>` and `:latest`),
- `docker ps` showing `calculator-app-1` and `calculator-nginx-1` as **healthy**,
- a SUCCESS email in your inbox.

---

## Part C: How every file works (viva prep)

### C1. The application (`src/`)

| File | Role |
|---|---|
| `CalculatorApplication.java` | Spring Boot entry point (`@SpringBootApplication`). |
| `service/CalculatorService.java` | Pure maths and validation, with no HTTP code. It rejects NaN/Infinity inputs, divide or modulo by zero, sqrt of a negative number, and non-finite results (overflow). |
| `controller/CalculatorController.java` | Maps `GET /api/v1/{op}?a=&b=` to the service and wraps the result in `CalculationResponse`. The params are primitive `double`, so Spring itself rejects missing or non-numeric values. |
| `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice`. It turns `InvalidOperationException`, missing-param and type-mismatch exceptions into **HTTP 400** with a JSON `ApiError` body. |
| `dto/CalculationResponse.java`, `dto/ApiError.java` | Java `record`s, which become the JSON bodies. |
| `application.properties` | Exposes only `health` and `info` from Actuator, and sets graceful shutdown. |

Tests (`mvn test`, 57 tests):
- `CalculatorServiceTest` holds plain **unit tests**: no Spring, so they're fast. `@ParameterizedTest` + `@CsvSource` covers many cases per operation, and `@Nested` groups them.
- `CalculatorControllerIntegrationTest` holds **integration tests**: `@SpringBootTest` starts the real app context, and `MockMvc` sends real HTTP requests through the dispatcher, controller, service and exception handler. It also checks `/actuator/health` = UP.
- Maven Surefire writes `target/surefire-reports/*.xml`, which is what Jenkins' `junit` step publishes.

### C2. `Dockerfile`

```
Stage 1  maven:3.9-eclipse-temurin-21   ← has JDK + Maven (≈ 500 MB), used only to build
   COPY pom.xml → mvn dependency:go-offline   ← dependency layer, cached until pom.xml changes
   COPY src     → mvn package -DskipTests     ← only this re-runs after a code change
Stage 2  eclipse-temurin:21-jre-alpine   ← only a JRE on Alpine (small)
   adduser spring                             ← non-root user
   COPY --from=build calculator.jar           ← nothing else from stage 1 survives
   USER spring / EXPOSE 8080 / HEALTHCHECK / ENTRYPOINT java -jar
```

- **Why multi-stage?** The final image has no Maven, no JDK and no source code. That makes it
  smaller, faster to pull, and gives it a smaller attack surface.
- **Why copy `pom.xml` before `src`?** Docker caches each layer. Dependencies change rarely and
  code changes often, so this order means rebuilds don't re-download everything.
- **Why non-root?** If the app were compromised, the attacker wouldn't be root inside the
  container.
- **`EXPOSE 8080`** documents the port. Publishing it happens with `-p` or in Compose.
- **`HEALTHCHECK`** makes `docker ps` show `(healthy)`. It uses `wget`, which Alpine already has.
- **`JAVA_TOOL_OPTIONS`**: the JVM reads it automatically. `MaxRAMPercentage=75` sizes the heap
  from the container's memory limit.
- **`.dockerignore`** keeps `target/`, `.git`, IDE files and `.env` out of the build context.
  That makes builds faster and stops secrets from leaking into the image.

### C3. `docker-compose.yml` + `nginx/nginx.conf`

- **Two services**: `app`, the API, isn't published to the host. `nginx` is the reverse proxy
  and the only thing published, on host port `8081` → container port 80.
- **`calc-net`** is a user-defined bridge network. Containers on it reach each other by service
  name (`http://app:8080`) through Docker's DNS.
- **Health checks**: `app` checks `/actuator/health`. `nginx` has `depends_on: app: condition: service_healthy`, so Nginx only starts once the API is actually UP.
- **`restart: unless-stopped`** restarts a container after a crash or a Docker restart, unless
  you stopped it on purpose.
- **Environment variables**: `SPRING_PROFILES_ACTIVE`, `SERVER_PORT` and `JAVA_TOOL_OPTIONS`.
  `IMAGE_TAG` picks which image version to run. Jenkins passes the build number.
- **`build: .`** alongside `image:`: on a fresh machine, `docker compose up` builds the image if
  it can't be pulled. So a single command always brings up the full stack.
- **The `resolver 127.0.0.11` line in nginx.conf**: Nginx normally resolves `app` to an IP once,
  at start-up. When Jenkins redeploys, the new app container gets a **new IP** and Nginx would
  return 502. Using Docker's DNS (`127.0.0.11`) with a variable in `proxy_pass` makes Nginx
  re-resolve every 10 s. This was tested by force-recreating the app container behind a running
  Nginx.
- **Logging** is capped at 3 × 10 MB per container, so logs can't fill the disk.

### C4. `Jenkinsfile`

| Block | What it does and why |
|---|---|
| `agent any` | Runs on the Jenkins machine itself (your Mac), which has Docker and Maven. |
| `options` | `disableConcurrentBuilds()`: two quick pushes can't deploy at the same time. `buildDiscarder`: keep the last 10 builds. `timeout`: kill a hung build. `timestamps()`: timestamps in the log. |
| `triggers` | `githubPush()` handles the webhook. `pollSCM('H/5 * * * *')` is the fallback: every 5 minutes, Jenkins asks GitHub for new commits. `H` spreads the load across jobs. |
| `environment` | Adds Homebrew and Docker to `PATH` (Homebrew services start with a minimal PATH). `IMAGE`, `TAG = BUILD_NUMBER`, `APP_URL`. |
| **Checkout** | `checkout scm` fetches the exact commit that triggered the build, then prints it. |
| **Build** | `mvn -B clean package -DskipTests` compiles and packages the JAR. `-B` is batch mode, for clean logs. |
| **Test** | `mvn -B test`. A failing test → Maven exits non-zero → the stage and build fail, and nothing gets deployed. `post { always { junit ... } }` publishes the results even when tests fail, giving you the **Test Result** page and trend graph. |
| **Docker Build** | Builds the image with two tags, `:<BUILD_NUMBER>` (immutable, traceable) and `:latest`. It also adds a label with the Git commit SHA. |
| **Push** | `withCredentials` injects the Docker Hub user and token from the Jenkins credential store as env vars. Jenkins masks them in the log. `--password-stdin` keeps the token out of the process list. The sh string uses **single quotes**, so the shell expands the secret, not Groovy, and it never appears in the log. `docker logout` always runs afterwards. |
| **Deploy** | `IMAGE_TAG=$TAG docker compose up -d --remove-orphans`, then it polls `/actuator/health` through Nginx for up to 60 s, then runs smoke tests: `add` must return 5.0 and `divide by 0` must return 400. On failure, it prints `docker compose ps` and the app logs. |
| `post` | Sends a success or failure email (`emailext`; the failure email attaches the log). `always` cleans up dangling images and keeps only the 3 newest build tags locally. |

**Why is it idempotent / re-runnable?** `docker compose up -d` compares the desired state with
what's running and only recreates containers whose image or config changed. The pipeline never
runs `down`. Re-running the same build leaves the healthy stack untouched. A new build swaps only
the `app` container, and Nginx follows it thanks to the resolver.

**Where are the secrets?** Only in the Jenkins credential store (`dockerhub-creds`,
`gmail-smtp`). The repo contains none: `.gitignore` excludes `.env`, `*.pem` and `*.key`.

---

## Part D: Demo script (Class 17, about 5 minutes)

**Before class:** Docker Desktop running, `brew services start jenkins-lts`,
`ngrok http --url=<domain> 8080` running, and `caffeinate -d` in a terminal so the Mac doesn't
sleep. Check that the last webhook delivery is green.

1. **Show the running state**
   ```bash
   docker ps
   ```
   ```bash
   curl -s localhost:8081/actuator/info
   ```
   It shows `"version":"0.0.1-SNAPSHOT"` (or whatever the current version is).
2. **Make a visible change.** In `pom.xml`, change `<version>0.0.1-SNAPSHOT</version>` to `<version>1.0.0</version>`, then:
   ```bash
   git checkout -b feature/release-1.0.0
   git commit -am "Release 1.0.0"
   git push -u origin feature/release-1.0.0
   ```
   Open a PR and merge it. You can also push straight to `main`, which is quicker in a demo.
3. **Show Jenkins**: a build starts by itself ("Started by GitHub push"). Walk through the
   stages as they turn green, and open the **Test Result** page.
4. **Show the deployment updated**
   ```bash
   curl -s localhost:8081/actuator/info
   ```
   It now says `1.0.0`.
   ```bash
   docker ps
   ```
   The app container is now running the image tag that matches the new build number.
5. **Show Docker Hub** (the new tag) and **your inbox** (the SUCCESS email).
6. **Optional, strong point:** break a test (e.g. change an expected value in
   `CalculatorServiceTest`), then push. The Test stage goes red, Deploy is **skipped**, the
   old version keeps serving traffic, and you get a FAILURE email. Revert the change and push again.

---

## Part E: Troubleshooting

| Symptom | Fix |
|---|---|
| `mvn: command not found` / `docker: not found` in Jenkins | The Jenkinsfile prepends `/opt/homebrew/bin:/usr/local/bin` to PATH. Check those paths with `which mvn docker` on your Mac. |
| `error getting credentials - err: exec: "docker-credential-desktop": executable file not found` | Same cause (PATH). `/usr/local/bin` contains `docker-credential-desktop`. If it still fails, remove the `"credsStore": "desktop"` line from `~/.docker/config.json`. |
| `Cannot connect to the Docker daemon` | Docker Desktop isn't running. Start it. Also check Docker Desktop → Settings → Advanced → "Allow the default Docker socket". |
| Port 8080 already in use | That's Jenkins. The app is only exposed through Nginx on **8081**. Use `docker run -p 9090:8080` for a standalone test. |
| Webhook delivery shows 404/502/timeout | Is ngrok running? Does the payload URL end in `/github-webhook/`? Is the domain the same as in ngrok? Use the **Redeliver** button after fixing. |
| Webhook is 200 but no build starts | Run the job once manually (B7.4). Tick "GitHub hook trigger for GITScm polling". The repo URL in the job must match the repo exactly. |
| `denied: requested access to the resource is denied` on push | The token needs Read & Write. The username must be lowercase `nirbhaysharma504`. The repo `calculator-api` must exist. |
| Email `535 Authentication failed` | Use a Google **App Password**, not your normal password. Port 465 with SSL, or port 587 with TLS. The System Admin e-mail must be the same Gmail address. |
| Email says to `null` / no email | Set the `NOTIFY_EMAIL` global environment variable (B6.3). |
| Deploy health check times out | Run `docker compose logs app`. Usually the image failed to start, or Docker is low on memory. |
| Nginx 502 right after deploy | The app was still starting. The health loop waits for it. If it persists, run `docker compose restart nginx`. |
| Image doesn't run on a classmate's / an Intel machine | It was built on Apple Silicon, so it's `arm64`. For a multi-arch image: `docker buildx build --platform linux/amd64,linux/arm64 -t nirbhaysharma504/calculator-api:latest --push .` |
| Pipeline stopped working overnight | The Mac slept, so Jenkins and ngrok were paused. Use `caffeinate -d` and restart ngrok. |

---

## Part F: Likely viva questions (short answers)

1. **What is CI vs CD?** CI integrates and tests every change automatically. Continuous
   Delivery/Deployment also packages and releases it automatically. Here, every push to `main`
   is deployed.
2. **Declarative vs scripted pipeline?** Declarative has a fixed structure (`pipeline`,
   `stages`, `post`) and is easier to read and validate. Scripted is free-form Groovy.
3. **Webhook vs Poll SCM?** A webhook is a push model: GitHub calls Jenkins right away. Polling
   is a pull model: Jenkins asks every N minutes, which is slower and wastes requests. I use the
   webhook, with polling as a fallback.
4. **Why ngrok?** Jenkins runs on localhost, and GitHub needs a public HTTPS URL to deliver the webhook.
5. **How are credentials kept safe?** They're in the Jenkins Credentials store and injected by
   `withCredentials`. They're masked in logs, sent with `--password-stdin`, and never written to Git.
6. **Why tag with the build number as well as `latest`?** `latest` moves. The build-number tag is
   immutable, so it traces a running container back to the exact build and commit, and lets you
   roll back (`IMAGE_TAG=41 docker compose up -d`).
7. **What makes the pipeline idempotent?** `compose up -d` reconciles to the desired state and
   never runs `down`. `disableConcurrentBuilds` prevents two deploys from racing each other.
8. **What happens if a test fails?** The Test stage fails, so the later stages (image, push,
   deploy) are skipped. The running deployment is untouched, and a failure email is sent.
9. **Multi-stage build benefits?** A smaller image, no build tools in production, and a smaller
   attack surface. Build and runtime concerns stay separate.
10. **Docker image vs container?** An image is a read-only template made of layers. A container
    is a running instance of an image, with a writable layer on top.
11. **Why Nginx in front?** It's a single entry point. The app isn't exposed directly, and the
    proxy is where TLS, rate limiting or load balancing across replicas can be added later.
12. **How does Nginx find the app?** By service name, through Docker's embedded DNS on the
    user-defined network `calc-net`. The `resolver` directive makes it re-resolve after redeploys.
13. **`depends_on` with `service_healthy`?** Without a condition, `depends_on` only orders
    container start. With `service_healthy`, Nginx waits until the app's healthcheck passes.
14. **Unit vs integration test here?** Unit tests check `CalculatorService` alone. Integration
    tests start Spring and go through HTTP → controller → service → exception handler.
15. **`mvn package` vs `mvn install`?** `package` builds the JAR into `target/`. `install` also
    copies it into the local `~/.m2` repository, which isn't needed here.
16. **What would the capstone add?** Ansible for configuration and deploy, Kubernetes instead of
    Compose, and Prometheus + Grafana scraping Actuator metrics.

---

## Part G: Screenshot checklist

Take these after B9 works. Save them in `report/screenshots/`.

1. Jenkins **Stage View**: all 6 stages green, with several builds visible.
2. Jenkins build page showing **"Started by GitHub push"**.
3. Jenkins **Test Result** page (57 tests, 0 failures) or the test trend graph.
4. **Email** in your inbox: SUCCESS, plus a FAILURE one if you did the optional demo step.
5. `docker ps`, or `docker compose ps`, showing both containers **(healthy)**.
6. Terminal with **curl responses**: add, divide, sqrt, plus a 400 for divide-by-zero and for `a=abc`.
7. **Docker Hub** repo → Tags page showing the build-number tags.
8. **GitHub**: the PR list (merged), and the commit history / network graph (Insights → Network).
9. **GitHub webhook** → Recent Deliveries with a green ✓.
10. Optional: Jenkins console log of the Deploy stage, showing "Application is UP" and the smoke tests.

---

## Part H: How to write the report (2 to 3 pages + appendix)

A draft is in `report/SPE_MiniProject_Report.docx`. It already contains the architecture
diagram, stage descriptions and challenges. You need to:

1. Fill in your **name and roll number** on the title block.
2. Read every section and **rewrite anything in your own words** that you can't explain.
   The brief says you must be able to explain everything.
3. Edit **Challenges faced** to match what *actually* happened to you during setup. Real
   problems carry more weight than generic ones. Keep the ones that happened, delete the rest,
   and add your own.
4. Paste your screenshots into the **Appendix** placeholders (Insert → Pictures). Keep the captions.
5. Check that the main body (everything before the Appendix) fits in **2 to 3 pages**.
6. **File → Save As → PDF** (or Export → PDF).

What graders look for, mapped to the rubric:

| Rubric item (marks) | Where it's shown in the report |
|---|---|
| API + tests (2.0) | Section 3, plus the Test Result screenshot |
| Git practices (1.0) | Section 6, plus the PR and commit-graph screenshots |
| Jenkins pipeline (3.0) | Sections 2 and 4, plus the Stage View, webhook and email screenshots |
| Dockerfile (1.5) | Section 5 |
| Compose (1.5) | Section 5, plus the `docker ps` (healthy) screenshot |
| Report/demo (1.0) | The whole document, plus the live demo |
