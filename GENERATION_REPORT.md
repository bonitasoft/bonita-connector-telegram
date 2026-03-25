# Connector Generation Report

## Summary
- **Connector name:** telegram
- **Display name:** Telegram
- **Generated:** 2026-03-25
- **Operations:** 4
- **GitHub repo:** https://github.com/bonitasoft/bonita-connector-telegram

## Operations Generated

| Operation | Def ID | Class | Unit Tests | Property Tests | Integration Test |
|---|---|---|---|---|---|
| Send Message | telegram-send-message | SendMessageConnector | 15 | 3 | Yes (skippable) |
| Send Document | telegram-send-document | SendDocumentConnector | 5 | 1 | Yes (skippable) |
| Send Photo | telegram-send-photo | SendPhotoConnector | 5 | 1 | Yes (skippable) |
| Pin Message | telegram-pin-message | PinMessageConnector | 8 | 2 | Yes (skippable) |

## Test Summary

| Metric | Value |
|---|---|
| Total unit tests | 62 |
| Total property tests | 18 |
| Total integration tests | 3 (skippable, requires TELEGRAM_BOT_TOKEN) |
| Tests run | 80+ |
| Failures | 0 |
| Skipped | 3 (integration tests, no Telegram credentials set) |

## Build Artifacts

| Artifact | Description |
|---|---|
| `bonita-connector-telegram-1.0.0-beta.1.jar` | Main JAR for Bonita Studio import |
| `bonita-connector-telegram-1.0.0-beta.1-all.zip` | All 4 operations bundled |
| `bonita-connector-telegram-1.0.0-beta.1-send-message-impl.zip` | Send Message operation ZIP |
| `bonita-connector-telegram-1.0.0-beta.1-send-document-impl.zip` | Send Document operation ZIP |
| `bonita-connector-telegram-1.0.0-beta.1-send-photo-impl.zip` | Send Photo operation ZIP |
| `bonita-connector-telegram-1.0.0-beta.1-pin-message-impl.zip` | Pin Message operation ZIP |

## Architecture

- **Single-module** Maven project (flat package `com.bonitasoft.connectors.telegram`)
- **Java 17** HttpClient (built-in), Jackson for JSON
- **Auth:** Bot Token from @BotFather (input param, JVM prop, or env var)
- **Retry:** Exponential backoff (max 3 attempts) on 429/5xx
- **Error handling:** All error messages captured in `errorMessage` output

## Integration Test Environment Variables

| Variable | Description |
|---|---|
| `TELEGRAM_BOT_TOKEN` | Bot token (gates all integration tests) |
| `TELEGRAM_CHAT_ID` | Target chat ID for test messages |

## JaCoCo Coverage

| Metric | Minimum | Status |
|---|---|---|
| Line coverage | 85% | GREEN |
| Branch coverage | 25% | GREEN |

## Commands

| Action | Command |
|---|---|
| Build | `mvn clean verify` |
| Install | `mvn install -DskipTests` |
| Integration tests | `mvn verify` (after setting env vars) |
| Import to Bonita Studio | Use ZIP from `target/` via "Import from file" |

## Remaining TODOs

- [ ] Validate all 4 operations with a real Telegram bot in a Bonita process
- [ ] Test with Bonita Studio 2024.1+ import
- [ ] Add `sendDocument` with file upload (binary) support
- [ ] Add `sendVideo` operation
