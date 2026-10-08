# Running locally with Docker Desktop

This guide runs the complete development stack locally without changing the normal
Compose file or the production profile.

## 1. Install Docker Desktop

Install Docker Desktop for Windows and select the **WSL 2 based engine** during setup.
Restart Windows if Docker Desktop requests it. Start Docker Desktop and wait until it
reports that the engine is running.

Verify in PowerShell:

```powershell
docker version
docker compose version
```

## 2. Configure secrets

The project requires a local `.env` file. Keep it out of Git.

```powershell
Copy-Item .env.example .env
```

Set a unique `JWT_SECRET` of at least 32 characters before starting the stack. The
included MailHog container receives development emails, so leave `SMTP_ENABLED=true`
for local work unless you deliberately configure Mailgun.

## 3. Start with local memory tuning

From the repository root, run:

```powershell
$env:COMPOSE_PARALLEL_LIMIT = "2"
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

`docker-compose.local.yml` reduces the JVM starting heaps while keeping all application
routes and dependencies unchanged. Limiting parallel image builds helps Docker Desktop
remain responsive on laptops.

## 4. Verify startup

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml ps
docker compose -f docker-compose.yml -f docker-compose.local.yml logs -f api-gateway
```

When startup completes, the API gateway is available at `http://localhost:8080` and its
health endpoint is `http://localhost:8080/actuator/health`.

Useful local tools:

- RabbitMQ management: `http://localhost:15672`
- MailHog inbox: `http://localhost:8025`
- Eureka dashboard: `http://localhost:8761`

## 5. Stop or restart safely

Stop containers while preserving database data:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

To remove local database and RabbitMQ data as well (this deletes local test data):

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml down -v
```

## If the laptop feels slow

Close memory-heavy applications first. Start the infrastructure and only the services
you are actively working on, for example:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d postgres redis rabbitmq config-server service-registry api-gateway auth-service
```

Add other services as needed. The gateway will return an unavailable response for a
route whose corresponding service is not running; this is expected during focused local
development.
