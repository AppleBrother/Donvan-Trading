# Half-Hour Random Scheduling and Credential Configuration

## Goal

Update Donvan so each monitor runs once at a random time in every natural half-hour window while preserving the immediate startup run. Replace the Coinr credentials without committing the token or device identifier to Git.

## Scheduling Behavior

- Divide each clock hour into two windows: minutes `00-29` and `30-59`.
- Futures and spot monitors independently choose one eligible random minute in each window.
- A monitor executes at most once per window.
- When the application starts, each monitor executes immediately after the existing startup delay.
- The startup execution does not consume the current window's random execution.
- If startup occurs partway through a window, the random minute is selected only from the remaining minutes in that window.
- The existing one-minute schedule check remains unchanged, so execution precision is one minute.

## Credential Handling

- Do not add the supplied token or device identifier to source code, tests, logs, commits, or documentation.
- Add configuration resolution for a shared Coinr access token and device identifier, with optional futures- and spot-specific overrides if the existing configuration structure benefits from them.
- Store the real values only in `/etc/donvan/donvan.env` on the server.
- Normalize a percent-encoded token once for `X-Token` and `Authorization` headers.
- Build the Cookie without double-encoding the token and include both `tickup-token` and `Device-ID` cookie entries.
- Send the device identifier as a `Device-ID` request header as well.
- Never print credential values in application logs.

## Code Structure

- Replace the hourly gate semantics with a half-hour-window gate. The class name and log labels should describe half-hour behavior rather than hourly behavior.
- Both futures and spot monitors continue to share the same scheduling implementation but retain independent gate instances.
- Extract shared credential normalization or cookie construction only if it removes meaningful duplication without broad refactoring.
- Keep fixed monitoring scope unchanged: only `projectId=56`, display name `HANA2`.
- Keep request endpoints, comparison thresholds, Telegram notification rules, and startup delay unchanged.

## Failure Handling

- Missing token or device identifier must be treated as an authentication configuration failure and follow the existing notification/cooldown path.
- HTTP authentication failures continue to produce the existing Telegram warning without exposing credentials.
- Scheduling state must advance once per window even when an API request fails, preventing retry floods within the same window.

## Verification

- Add deterministic tests covering immediate startup execution, a second execution within the current half-hour window, at-most-once execution per window, and independent execution in the next window.
- Add tests for encoded-token normalization and Cookie/header construction without embedding the real credential.
- Run the complete Maven test suite and package build.
- Commit and push only source, tests, and documentation; do not commit generated `target/` changes.
- On the server, update `/etc/donvan/donvan.env`, pull the pushed commit, build with Java 17, restart `donvan.service`, and verify:
  - the service is active;
  - startup requests for the three Coinr endpoints succeed for `projectId=56`;
  - logs show half-hour scheduling rather than hourly scheduling;
  - credentials do not appear in logs.
