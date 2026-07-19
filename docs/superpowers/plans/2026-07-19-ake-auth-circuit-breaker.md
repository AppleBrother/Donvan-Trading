# AKE Authentication Circuit Breaker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Monitor only Coinr project `59 / AKE` and stop every futures and spot API call for the rest of the process after the first confirmed authentication failure.

**Architecture:** Add a Spring singleton `CoinrAuthenticationCircuitBreaker` backed by an atomic first-failure reason and inject the same instance into both monitors. Each scheduled method checks it before advancing the half-hour gate; existing authentication classification opens it, while the first opener alone sends the Telegram stop notification.

**Tech Stack:** Java 17, Spring Boot 4, Java `HttpClient`, JUnit 5, Maven, systemd.

## Global Constraints

- Fixed project is `projectId=59`, display name `AKE`.
- Do not call the enabled-project-list endpoint.
- Preserve immediate startup execution and natural half-hour random windows.
- HTTP `401/403`, Coinr code `2001`, matching authentication messages, and missing credentials open the breaker.
- Network, TLS, HTTP 5xx, parsing, and other non-authentication failures do not open it.
- The breaker resets only when the service process restarts.
- Never commit or log the supplied token or Device-ID.
- Store production credentials only in `/etc/donvan/donvan.env`.
- Do not stage generated `target/` files.

---

### Task 1: Fixed AKE Project

**Files:**
- Modify: `src/test/java/com/example/donvan/forTest/trigger/FixedProjectSelectionTests.java`
- Modify: `src/main/java/com/example/donvan/forTest/vo/MonitorConstants.java`

**Interfaces:**
- Produces: `MonitorConstants.MONITORED_PROJECT_ID == 59L` and `MONITORED_PROJECT_NAME.equals("AKE")`.
- Consumes: existing `resolveProjectIds()` and `projectLabel(Long)` methods in both monitors.

- [ ] **Step 1: Change the fixed-project expectations to AKE**

```java
assertEquals(59L, projectIdField.getLong(null));
assertEquals("AKE", projectNameField.get(null));
assertEquals(List.of(59L), resolveProjectIds.invoke(monitor));
assertEquals("ake", projectLabel.invoke(monitor, 59L));
```

- [ ] **Step 2: Run the test and verify RED**

Run: `./mvnw -Dtest=FixedProjectSelectionTests test`

Expected: failures showing current values `56`, `HANA2`, and project list `[56]`.

- [ ] **Step 3: Change only the two fixed constants**

```java
public static final long MONITORED_PROJECT_ID = 59L;
public static final String MONITORED_PROJECT_NAME = "AKE";
```

- [ ] **Step 4: Run the test and verify GREEN**

Run: `./mvnw -Dtest=FixedProjectSelectionTests test`

Expected: 2 tests pass with no failures.

### Task 2: Shared Authentication Circuit Breaker

**Files:**
- Create: `src/main/java/com/example/donvan/forTest/trigger/CoinrAuthenticationCircuitBreaker.java`
- Create: `src/test/java/com/example/donvan/forTest/trigger/CoinrAuthenticationCircuitBreakerTests.java`

**Interfaces:**
- Produces: package-private `boolean isOpen()`, `boolean open(String reason)`, and `String reason()`.
- Consumes: both monitor constructors receive the same Spring-managed breaker instance.

- [ ] **Step 1: Write the failing breaker tests**

```java
CoinrAuthenticationCircuitBreaker breaker = new CoinrAuthenticationCircuitBreaker();
assertFalse(breaker.isOpen());
assertTrue(breaker.open("HTTP 401"));
assertTrue(breaker.isOpen());
assertEquals("HTTP 401", breaker.reason());
assertFalse(breaker.open("HTTP 403"));
assertEquals("HTTP 401", breaker.reason());
```

Add a second test proving a null or blank reason is normalized to `authentication failure`.

- [ ] **Step 2: Run the test and verify RED**

Run: `./mvnw -Dtest=CoinrAuthenticationCircuitBreakerTests test`

Expected: compilation failure because the breaker class does not exist.

- [ ] **Step 3: Implement the singleton**

Create a `@Component` with `AtomicReference<String> firstFailureReason`. `open` normalizes the reason and returns `compareAndSet(null, normalizedReason)`; `isOpen` checks for non-null and `reason` returns the stored value.

- [ ] **Step 4: Run the test and verify GREEN**

Run: `./mvnw -Dtest=CoinrAuthenticationCircuitBreakerTests test`

Expected: all breaker tests pass.

### Task 3: Stop Both Monitors After Authentication Failure

**Files:**
- Modify: `src/main/java/com/example/donvan/forTest/trigger/CoinrFuturesPnlVolumeMonitor.java`
- Modify: `src/main/java/com/example/donvan/forTest/trigger/CoinrSpotPnlVolumeMonitor.java`
- Modify: `src/test/java/com/example/donvan/forTest/trigger/FixedProjectSelectionTests.java`
- Create: `src/test/java/com/example/donvan/forTest/trigger/AuthenticationCircuitBreakerMonitorTests.java`

**Interfaces:**
- Consumes: `CoinrAuthenticationCircuitBreaker.isOpen()` before each schedule gate and `open(reason)` inside each `handleAuthFailure`.
- Produces: public constructor injection for each Spring monitor and one Telegram notification from the first successful `open` call.

- [ ] **Step 1: Write failing monitor short-circuit tests**

For each monitor, open one shared breaker, construct the monitor, call its scheduled polling method without initializing the HTTP client, and use reflection to assert the gate's `startupExecutionPending` field remains `true`. This proves the method returned before scheduling or HTTP work.

- [ ] **Step 2: Run the test and verify RED**

Run: `./mvnw -Dtest=AuthenticationCircuitBreakerMonitorTests test`

Expected: compilation failure because monitor constructors do not yet accept the breaker.

- [ ] **Step 3: Inject and enforce the breaker**

Add a final breaker field and public constructor to both monitors. At the beginning of each scheduled method, return when disabled or when `isOpen()` is true, before calling `halfHourlyExecutionGate.shouldExecute`. Change `handleAuthFailure` to return immediately when `open(reason)` returns false; only the first opener sends Telegram, with text stating monitoring is stopped until token replacement and service restart. Remove the per-project authentication-reason maps and success reset methods because an open breaker cannot reset within the process.

- [ ] **Step 4: Update construction tests and verify GREEN**

Pass a new breaker into both monitor constructors in `FixedProjectSelectionTests`, then run:

`./mvnw -Dtest=AuthenticationCircuitBreakerMonitorTests,FixedProjectSelectionTests,CoinrAuthenticationCircuitBreakerTests test`

Expected: all targeted tests pass.

### Task 4: Verification, Push, and Deployment

**Files:**
- Modify only on server: `/etc/donvan/donvan.env`

**Interfaces:**
- Consumes: `MONITOR_ACCESS_TOKEN` and `MONITOR_DEVICE_ID` from systemd's environment file.
- Produces: active `donvan.service` running the pushed commit and monitoring only AKE.

- [ ] **Step 1: Run repository verification**

Run `./mvnw test`, `./mvnw -DskipTests package`, and `git diff --check`. Scan non-generated files for supplied credential fragments and confirm no match.

- [ ] **Step 2: Commit and push intentional files only**

Stage the breaker, monitor changes, AKE constants, tests, and this plan. Confirm no `target/` path is staged. Commit and fast-forward the verified branch into `master`, then push `origin master`.

- [ ] **Step 3: Update server credentials and deploy**

Write the new values to `/etc/donvan/donvan.env` through hidden stdin, retain mode `600 root:root`, restore and clean generated server `target/` changes, pull, build with Java 17, and restart `donvan.service`.

- [ ] **Step 4: Verify live behavior**

Confirm service state `active`, deployed commit, exactly one BUY, one SELL, and one spot startup request containing `projectId=59`, three successful Coinr `code=0` responses, zero `/projects/enabled` calls, and no credential values in logs. Circuit-breaker stop behavior is proven by automated tests, not by invalidating production credentials.
