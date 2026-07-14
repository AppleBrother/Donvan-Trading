# Half-Hour Random Scheduling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Run each Donvan monitor once at a random time in every natural half-hour window, retain the immediate startup run, and load the new Coinr token and Device-ID securely from server environment variables.

**Architecture:** Replace the hourly execution gate with a half-hour-window gate driven by `LocalDateTime`. Centralize Coinr token normalization and request-header construction in a package-private helper used by both monitors, while resolving credentials from system properties/environment variables in `MonitorConstants`.

**Tech Stack:** Java 17, Spring Boot scheduling, Java `HttpClient`, JUnit 5, Maven, systemd.

## Global Constraints

- Monitor only `projectId=56` with display name `HANA2`.
- Preserve the existing five-second startup delay and one-minute scheduler check.
- Startup execution must not consume the current half-hour window's random execution.
- Never commit or log the supplied token or Device-ID.
- Store production credentials only in `/etc/donvan/donvan.env`.
- Do not stage generated `target/` files.

---

### Task 1: Half-Hour Execution Gate

**Files:**
- Create: `src/main/java/com/example/donvan/forTest/trigger/HalfHourlyRandomExecutionGate.java`
- Delete: `src/main/java/com/example/donvan/forTest/trigger/HourlyRandomExecutionGate.java`
- Create: `src/test/java/com/example/donvan/forTest/trigger/HalfHourlyRandomExecutionGateTests.java`
- Delete: `src/test/java/com/example/donvan/forTest/trigger/HourlyRandomExecutionGateTests.java`
- Modify: `src/main/java/com/example/donvan/forTest/trigger/CoinrFuturesPnlVolumeMonitor.java`
- Modify: `src/main/java/com/example/donvan/forTest/trigger/CoinrSpotPnlVolumeMonitor.java`

**Interfaces:**
- Produces: `HalfHourlyRandomExecutionGate(String name)` and synchronized `boolean shouldExecute(LocalDateTime now)`.
- Consumes: both monitors call `shouldExecute(LocalDateTime.now())` from their existing scheduled methods.

- [ ] **Step 1: Write the failing scheduling tests**

Create tests that assert: first invocation runs immediately; a second invocation at minute 29 runs for the current window; a third does not; minute 59 runs once for the next window; minute 29 of the following hour runs once again.

```java
HalfHourlyRandomExecutionGate gate = new HalfHourlyRandomExecutionGate("TEST");
LocalDateTime start = LocalDateTime.of(2026, 7, 14, 5, 29, 0);
assertTrue(gate.shouldExecute(start));
assertTrue(gate.shouldExecute(start.plusSeconds(1)));
assertFalse(gate.shouldExecute(start.plusSeconds(2)));
assertTrue(gate.shouldExecute(LocalDateTime.of(2026, 7, 14, 5, 59, 0)));
assertFalse(gate.shouldExecute(LocalDateTime.of(2026, 7, 14, 5, 59, 1)));
assertTrue(gate.shouldExecute(LocalDateTime.of(2026, 7, 14, 6, 29, 0)));
```

- [ ] **Step 2: Run the test to verify RED**

Run: `./mvnw -Dtest=HalfHourlyRandomExecutionGateTests test`

Expected: compilation failure because `HalfHourlyRandomExecutionGate` does not exist.

- [ ] **Step 3: Implement the minimal half-hour gate**

Compute the current window start as the hour plus either 0 or 30 minutes. Store `currentWindowStart`, `executedWindowStart`, and `triggerAt`; choose `triggerAt` from the remaining whole minutes through the window's final minute. Keep `startupExecutionPending` independent of the per-window executed marker. Use `half-hour random schedule` and `half-hour random execution` in log messages.

- [ ] **Step 4: Switch both monitors to the new gate and verify GREEN**

Replace each `HourlyRandomExecutionGate hourlyExecutionGate` field with `HalfHourlyRandomExecutionGate halfHourlyExecutionGate`, update the scheduled method call, and run:

`./mvnw -Dtest=HalfHourlyRandomExecutionGateTests test`

Expected: all scheduling tests pass.

### Task 2: Secure Coinr Credential Headers

**Files:**
- Create: `src/main/java/com/example/donvan/forTest/trigger/CoinrRequestCredentials.java`
- Create: `src/test/java/com/example/donvan/forTest/trigger/CoinrRequestCredentialsTests.java`
- Modify: `src/main/java/com/example/donvan/forTest/vo/MonitorConstants.java`
- Modify: `src/main/java/com/example/donvan/forTest/trigger/CoinrFuturesPnlVolumeMonitor.java`
- Modify: `src/main/java/com/example/donvan/forTest/trigger/CoinrSpotPnlVolumeMonitor.java`

**Interfaces:**
- Produces: `static boolean isConfigured(String token, String deviceId)` and `static void applyTo(HttpRequest.Builder builder, String token, String deviceId)`.
- Produces constants: `MonitorConstants.Futures.ACCESS_TOKEN`, `MonitorConstants.Futures.DEVICE_ID`, `MonitorConstants.Spot.ACCESS_TOKEN`, and `MonitorConstants.Spot.DEVICE_ID`.
- Consumes configuration keys: shared `monitor.access-token`/`MONITOR_ACCESS_TOKEN` and `monitor.device-id`/`MONITOR_DEVICE_ID`, preceded by optional monitor-specific equivalents.

- [ ] **Step 1: Write failing credential tests**

Use synthetic values only. Build a request with token `57%3Aexample%3Asignature` and Device-ID `device-123`, then assert headers:

```java
assertEquals("57:example:signature", request.headers().firstValue("X-Token").orElseThrow());
assertEquals("Bearer 57:example:signature", request.headers().firstValue("Authorization").orElseThrow());
assertEquals("device-123", request.headers().firstValue("Device-ID").orElseThrow());
assertEquals("tickup-token=57%3Aexample%3Asignature; Device-ID=device-123",
        request.headers().firstValue("Cookie").orElseThrow());
```

Also assert `isConfigured` is false when either credential is blank or the token equals `MonitorConstants.TOKEN_PLACEHOLDER`.

- [ ] **Step 2: Run the test to verify RED**

Run: `./mvnw -Dtest=CoinrRequestCredentialsTests test`

Expected: compilation failure because `CoinrRequestCredentials` does not exist.

- [ ] **Step 3: Implement credential resolution and header helper**

Remove the committed access-token literals. Resolve monitor-specific property/environment values first, then shared values, then `TOKEN_PLACEHOLDER`. URL-decode the token once, strip an optional `Bearer ` prefix, URL-encode it once for the Cookie, append the encoded Device-ID cookie entry, and add `X-Token`, `Authorization`, `Device-ID`, and `Cookie` headers.

- [ ] **Step 4: Integrate both monitors and verify GREEN**

Replace duplicated token normalization and Cookie methods with `CoinrRequestCredentials`. Treat either missing token or missing Device-ID as an authentication configuration failure. Run:

`./mvnw -Dtest=CoinrRequestCredentialsTests test`

Expected: all credential tests pass and no real credential appears in test output.

### Task 3: Full Verification, Push, and Deployment

**Files:**
- Modify only on server: `/etc/donvan/donvan.env`

**Interfaces:**
- Consumes: `MONITOR_ACCESS_TOKEN` and `MONITOR_DEVICE_ID` from systemd's environment file.
- Produces: running `donvan.service` built from the pushed Git commit.

- [ ] **Step 1: Run repository verification**

Run `./mvnw test` and `./mvnw -DskipTests package`. Expected: zero test failures and successful package build. Run `git diff --check` and scan tracked changes for the supplied credential fragments; expected: no matches.

- [ ] **Step 2: Commit and push only intentional files**

Stage the new/renamed scheduling files, credential helper/tests, both monitors, `MonitorConstants`, and this plan. Confirm no `target/` path is staged. Commit and push `master` to `origin`.

- [ ] **Step 3: Update server secrets and deploy**

Update `/etc/donvan/donvan.env` without printing its contents. Restore and clean generated server `target/` changes, pull with `runuser -u donvan -- git -C /opt/donvan pull --ff-only`, build with Java 17, and restart `donvan.service`.

- [ ] **Step 4: Verify live behavior**

Check `systemctl is-active donvan`, the deployed commit, and fresh journal lines. Confirm startup generated exactly the three `projectId=56` API requests, successful API response codes, half-hour scheduling log labels, no enabled-project-list request, and no credential value in logs.
