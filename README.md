# MyApp — Deployment & Update Guide

## Overview

This document describes the deployment procedures for the **Spring Boot MyApp** application.

Two deployment methods are available:

1. **Manual Deployment** — used for testing, troubleshooting, and emergency deployments.
2. **GitHub Actions CI/CD** — the recommended method for normal application updates.

The CI/CD pipeline automatically builds the application and deploys the new JAR to the server when changes are pushed to the configured branch.

---

# Application Information

| Configuration       | Value                                                   |
| ------------------- | ------------------------------------------------------- |
| Application         | MyApp                                                   |
| Service Name        | `myapp`                                                 |
| Datadog APM Service | `spring-boot-messages`                                  |
| Environment         | `your env`                                              |
| Port                | `8004`                                                  |
| Working Directory   | `/home/user/rafa/myapp`                                 |
| JAR Location        | `/home/user/rafa/myapp/target/myapp-0.0.1-SNAPSHOT.jar` |
| Systemd Service     | `/etc/systemd/system/myapp.service`                     |
| Application Log     | `/home/user/rafa/myapp/log/app.log`                     |
| CI/CD               | GitHub Actions                                          |

---

# 1. Manual Deployment

Manual deployment should primarily be used for testing, troubleshooting, or when the CI/CD pipeline is unavailable.

## 1.1 Navigate to the Application Directory

```bash
cd /home/user/rafa/myapp
```

Verify:

```bash
pwd
ls
```

Expected structure:

```text
myapp/
├── pom.xml
├── src/
├── target/
└── log/
```

---

## 1.2 Update the Source Code

Pull the latest changes:

```bash
git pull
```

Verify the latest commit:

```bash
git log -1 --oneline
```

---

## 1.3 Build the Application

```bash
./mvnw clean package
```

If Maven Wrapper is not executable:

```bash
chmod +x mvnw
./mvnw clean package
```

The build must finish with:

```text
BUILD SUCCESS
```

Verify the JAR:

```bash
ls -lh target/*.jar
```

Expected:

```text
target/myapp-0.0.1-SNAPSHOT.jar
```

> Do not restart the service if the build fails.

---

## 1.4 Restart the Application

```bash
sudo systemctl restart myapp
```

Check:

```bash
sudo systemctl status myapp --no-pager
```

Expected:

```text
Active: active (running)
```

---

## 1.5 Verify the Application

Check port `8004`:

```bash
ss -tulpn | grep 8004
```

Test GET:

```bash
curl http://localhost:8004/api/messages
```

Test POST:

```bash
curl -X POST http://localhost:8004/api/messages \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello Spring Boot"}'
```

---

# 2. GitHub Actions CI/CD

The recommended deployment method is GitHub Actions.

The deployment flow is:

```text
Developer
    │
    │ git push
    ▼
GitHub Repository
    │
    │ GitHub Actions
    ▼
Build
    │
    │ ./mvnw clean package
    ▼
JAR Artifact
    │
    │ Deploy via SSH
    ▼
Server
/home/user/rafa/myapp
    │
    │ Update JAR
    ▼
systemd
myapp.service
    │
    │ restart
    ▼
Spring Boot
    │
    ├── Port 8004
    ├── Logs
    └── Datadog APM
```

---

# 3. CI/CD Pipeline Stages

The GitHub Actions pipeline should perform the following stages:

```text
1. Checkout
      ↓
2. Setup Java 17
      ↓
3. Maven Build
      ↓
4. Package JAR
      ↓
5. Upload Artifact
      ↓
6. SSH to Server
      ↓
7. Deploy JAR
      ↓
8. Restart systemd
      ↓
9. Health Check
```

---

# 4. GitHub Actions Workflow

Create the workflow:

```text
.github/
└── workflows/
    └── deploy.yml
```


> Adjust the server hostname, username, JAR path, and authentication method to match the actual environment.

---

# 5. GitHub Repository Secrets

The GitHub repository should contain the following Actions secrets:

| Secret            | Description                            |
| ----------------- | -------------------------------------- |
| `SERVER_HOST`     | Server hostname or IP address          |
| `SERVER_USER`     | SSH user                               |
| `SSH_PRIVATE_KEY` | Private SSH key used by GitHub Actions |

Example:

```text
SERVER_HOST=your-server-ip
SERVER_USER=user
SSH_PRIVATE_KEY=<private SSH key>
```

Do not commit private keys, passwords, or other credentials to the repository.

---

# 6. Required Server Configuration

The server must allow the GitHub Actions runner to connect through SSH.

The deployment user must also be able to restart the application:

```bash
sudo systemctl restart myapp
```

For a production-style setup, configure `sudo` so the deployment user can restart only the required service instead of granting unrestricted sudo access.

Example sudoers configuration:

```text
user ALL=(root) NOPASSWD: /usr/bin/systemctl restart myapp
user ALL=(root) NOPASSWD: /usr/bin/systemctl status myapp
```

Use:

```bash
sudo visudo
```

to modify sudoers safely.

---

# 7. CI/CD Update Procedure

After the CI/CD pipeline is configured, normal application updates become:

```bash
git add .
git commit -m "Update application"
git push origin main
```

GitHub Actions automatically performs:

```text
git push
   ↓
GitHub Actions
   ↓
Checkout source
   ↓
Maven build
   ↓
Create JAR
   ↓
Upload JAR to server
   ↓
systemctl restart myapp
   ↓
Health check
```

You no longer need to manually run:

```bash
./mvnw clean package
```

or:

```bash
sudo systemctl restart myapp
```

for normal deployments.

---

# 8. Verify CI/CD Deployment

After pushing to GitHub, open the repository's **Actions** tab.

Verify:

```text
Build Application       ✓
Deploy to Server        ✓
Restart Application     ✓
Verify Application      ✓
```

Then verify directly on the server:

```bash
sudo systemctl status myapp --no-pager
```

Expected:

```text
Active: active (running)
```

Check the JAR:

```bash
ls -lh /home/user/rafa/myapp/target/
```

Test the API:

```bash
curl http://localhost:8004/api/messages
```

---

# 9. CI/CD Health Check

The pipeline should not consider the deployment successful only because `systemctl restart` succeeded.

The application should also be tested.

Example:

```bash
curl --fail http://localhost:8004/api/messages
```

A better deployment verification is:

```bash
sudo systemctl is-active --quiet myapp
curl --fail http://localhost:8004/api/messages
```

If either command fails, the deployment should be marked as failed.

---

# 10. Application Logs After Deployment

Check application logs:

```bash
tail -n 100 /home/user/rafa/myapp/log/app.log
```

Follow logs:

```bash
tail -f /home/user/rafa/myapp/log/app.log
```

Check systemd logs:

```bash
sudo journalctl -u myapp -n 100 --no-pager
```

Follow:

```bash
sudo journalctl -u myapp -f
```

---

# 11. Datadog Verification

The application uses the Datadog Java Agent.

Expected configuration:

```text
Service: spring-boot-messages
Environment: your env
Source: java
```

After deployment, verify that the application is still reporting to Datadog.

Check:

```bash
sudo journalctl -u myapp -n 100 --no-pager
```

Look for:

```text
DATADOG TRACER CONFIGURATION
```

Verify the APM service name:

```text
spring-boot-messages
```

---

# 12. Automatic Traffic

`TrafficScheduler` generates periodic API requests.

Expected log:

```text
Traffic generated. Status=200
```

If intentional error traffic is configured:

```text
Traffic generated. Status=500
```

This traffic can be used to test:

* APM
* Traces
* Error tracking
* Logs
* Service performance
* Datadog dashboards
* Monitors

---

# 13. Rollback

If the newly deployed version causes problems, restore a known-good JAR.

Example:

```bash
ls -lh /home/user/rafa/myapp/target/
```

Restore the previous JAR if available.

Then:

```bash
sudo systemctl restart myapp
```

Verify:

```bash
sudo systemctl status myapp --no-pager
```

Test:

```bash
curl --fail http://localhost:8004/api/messages
```

---

# 14. Troubleshooting

## Build Failure

If GitHub Actions reports:

```text
BUILD FAILURE
```

the deployment stage should not execute.

Fix the build locally:

```bash
./mvnw clean package
```

Then push the correction.

---

## Deployment Failure

Check the GitHub Actions logs.

On the server:

```bash
sudo journalctl -u myapp -n 100 --no-pager
```

---

## Service Failed

```bash
sudo systemctl status myapp --no-pager -l
```

Then:

```bash
sudo journalctl -xeu myapp
```

---

## Port 8004 Not Listening

```bash
ss -tulpn | grep 8004
```

Check:

```bash
sudo journalctl -u myapp -n 100 --no-pager
```

---

# 15. Manual vs CI/CD

| Task                |           Manual |             CI/CD |
| ------------------- | ---------------: | ----------------: |
| `git pull`          |              Yes |         Automatic |
| Maven build         |           Manual |         Automatic |
| `mvn clean package` |           Manual |         Automatic |
| Upload JAR          |           Manual |         Automatic |
| Restart systemd     |           Manual |         Automatic |
| Health check        |           Manual |         Automatic |
| Deployment history  |          Limited |    GitHub Actions |
| Recommended         | Testing/Fallback | Normal deployment |

---

# 16. Recommended Workflow

For normal development:

```text
Developer
    │
    │ Code changes
    ▼
Git
    │
    │ git push origin main
    ▼
GitHub
    │
    ▼
GitHub Actions
    │
    ├── Build
    ├── Test
    ├── Package
    └── Deploy
            │
            ▼
       Server
            │
            ▼
       myapp.service
            │
            ▼
      Spring Boot
            │
            ├── API :8004
            ├── Logs
            └── Datadog
```

Manual deployment should remain available as a fallback:

```text
cd /home/user/rafa/myapp
./mvnw clean package
sudo systemctl restart myapp
```

---

# 17. Quick Deployment Checklist

## Manual Deployment

* [ ] `git pull` completed
* [ ] `./mvnw clean package` → `BUILD SUCCESS`
* [ ] JAR exists
* [ ] `systemctl restart myapp` succeeded
* [ ] Service is `active (running)`
* [ ] Port `8004` is listening
* [ ] GET API works
* [ ] POST API works
* [ ] Application logs are generated
* [ ] Datadog APM is reporting
* [ ] TrafficScheduler is generating traffic

## CI/CD Deployment

* [ ] Changes committed
* [ ] `git push origin main`
* [ ] GitHub Actions started
* [ ] Build succeeded
* [ ] JAR generated
* [ ] Deployment succeeded
* [ ] `systemctl restart myapp` succeeded
* [ ] Health check succeeded
* [ ] API responds
* [ ] Logs are generated
* [ ] Datadog APM is reporting
