# AKE Monitoring and Authentication Circuit Breaker

## Goal

Switch Donvan's fixed monitoring target to the Coinr project returned as `id=59`, `name=AKE`, and permanently stop all Coinr API calls within the running process after a confirmed authentication failure.

## Fixed Project

- Set the monitored project ID to `59` and the display name to `AKE`.
- Continue using the fixed-project path; do not call the enabled-project-list endpoint.
- Keep the existing futures BUY, futures SELL, and spot endpoints unchanged apart from `projectId=59`.
- Keep the half-hour random scheduling windows, immediate startup execution, comparison thresholds, and Telegram change notifications unchanged.

## Credential Handling

- Do not add the supplied token or Device-ID to source code, tests, logs, commits, or documentation.
- Store the real values only in `/etc/donvan/donvan.env` using the existing `MONITOR_ACCESS_TOKEN` and `MONITOR_DEVICE_ID` variables.
- Preserve the existing token normalization and request headers implemented by `CoinrRequestCredentials`.

## Global Authentication Circuit Breaker

- Add one Spring singleton circuit breaker shared by the futures and spot monitors.
- The circuit breaker starts closed whenever the application process starts.
- Both scheduled monitor methods check the breaker before advancing their scheduling gate or creating an HTTP request. When open, they return immediately.
- A confirmed authentication failure opens the breaker for the rest of the process lifetime.
- Opening is atomic and idempotent: only the first caller is responsible for sending the Telegram notification.
- The monitor that detects the first authentication failure sends one notification stating that monitoring has stopped and that the token must be updated before restarting the service.
- If futures BUY fails authentication, the existing early return prevents the futures SELL request. A failure in any futures or spot request prevents every later Coinr request from either monitor.
- Replacing credentials without restarting does not close the breaker. Restarting the service creates a new closed breaker and allows the startup requests to run with the new environment values.

## Authentication Failure Classification

The following existing conditions open the breaker:

- HTTP status `401` or `403`;
- Coinr response code `2001`;
- a response message classified by the existing authentication-message matcher as token expiration or authentication failure;
- missing token or Device-ID detected before the first request.

Network timeouts, TLS failures, HTTP 5xx responses, malformed successful responses, and other non-authentication errors continue through the existing retry and cooldown behavior and do not open the breaker.

## Testing

- Add unit tests proving the shared breaker is initially closed, opens once, retains the first reason, and reports only the first transition as new.
- Update monitor construction tests to use the shared breaker dependency.
- Add monitor-level tests proving an already-open breaker makes futures and spot scheduled methods return without advancing their schedule gates or initializing HTTP work.
- Keep fixed-project tests and update their expected values to `59` and `AKE`.
- Run the complete Maven test suite and package build.

## Deployment Verification

- Commit and push only source, tests, and documentation; never commit generated `target/` files or credentials.
- Update `/etc/donvan/donvan.env` without printing the credential values.
- Pull, build with Java 17, restart `donvan.service`, and verify it is active at the deployed commit.
- Verify startup performs exactly one futures BUY, one futures SELL, and one spot request for `projectId=59`, all with successful Coinr responses.
- Verify the enabled-project-list endpoint is not called and credentials do not appear in logs.
- Use automated tests rather than intentionally invalidating the production token to verify circuit-breaker behavior.
