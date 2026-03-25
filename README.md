# Bonita Telegram Connector

Telegram Bot API connector for the [Bonita](https://www.bonitasoft.com) BPM platform. Send messages, documents, photos, and pin messages in Telegram chats directly from your Bonita processes.

## Operations

| Operation | Def ID | Description |
|---|---|---|
| Send Message | `telegram-send-message` | Send a text message (HTML, Markdown, MarkdownV2) |
| Send Document | `telegram-send-document` | Send a document by URL |
| Send Photo | `telegram-send-photo` | Send a photo by URL |
| Pin Message | `telegram-pin-message` | Pin an existing message in a chat |

## Prerequisites

1. Create a Telegram Bot via [@BotFather](https://t.me/BotFather) and obtain a **Bot Token**
2. Add the bot to your target chat/group/channel
3. Get the **Chat ID** (use [@userinfobot](https://t.me/userinfobot) or the Telegram API `getUpdates` method)

## Installation

1. Build the connector: `mvn clean package -DskipTests`
2. In Bonita Studio, go to **Development > Connector > Import connector...**
3. Select the generated ZIP file from `target/` (e.g., `bonita-connector-telegram-1.0.0-beta.1-all.zip` for all operations, or individual operation ZIPs)

## Configuration

### Connection Parameters

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `botToken` | String | Yes | - | Telegram Bot API token (from @BotFather) |
| `baseUrl` | String | No | `https://api.telegram.org` | API base URL (for proxies) |
| `connectTimeout` | Integer | No | `30000` | Connection timeout in ms |
| `readTimeout` | Integer | No | `60000` | Read timeout in ms |

### Send Message Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `chatId` | String | Yes | Target chat ID |
| `text` | String | Yes | Message text (max 4096 chars) |
| `parseMode` | String | No | `HTML`, `Markdown`, or `MarkdownV2` (default: `HTML`) |
| `disableNotification` | Boolean | No | Send silently (default: `false`) |

### Send Document Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `chatId` | String | Yes | Target chat ID |
| `documentUrl` | String | Yes | URL of the document to send |
| `caption` | String | No | Document caption |
| `parseMode` | String | No | Parse mode for caption |
| `disableNotification` | Boolean | No | Send silently |

### Send Photo Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `chatId` | String | Yes | Target chat ID |
| `photoUrl` | String | Yes | URL of the photo to send |
| `caption` | String | No | Photo caption |
| `parseMode` | String | No | Parse mode for caption |
| `disableNotification` | Boolean | No | Send silently |

### Pin Message Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `chatId` | String | Yes | Target chat ID |
| `messageId` | Long | Yes | ID of the message to pin |
| `disableNotification` | Boolean | No | Pin silently |

### Output Parameters

All operations set:

| Output | Type | Description |
|---|---|---|
| `success` | Boolean | `true` if the operation succeeded |
| `errorMessage` | String | Error description (empty on success) |
| `messageId` | Long | ID of the sent message (send operations) |
| `chatId` | String | Chat ID (send operations) |
| `fileId` | String | Telegram file ID (document/photo operations) |
| `pinned` | Boolean | `true` if pinning succeeded (pin operation) |

## Build

```bash
# Build with tests
mvn clean verify

# Build without tests
mvn clean package -DskipTests

# Run unit tests only
mvn test

# Run a single test class
mvn test -Dtest=SendMessageConnectorTest
```

## Integration Tests

Integration tests require real Telegram credentials and are skipped by default:

```bash
export TELEGRAM_BOT_TOKEN="your-bot-token"
export TELEGRAM_CHAT_ID="your-chat-id"
mvn verify
```

## Architecture

- **Single-module** Maven project
- **Java 17** (records, pattern matching, sealed classes)
- **Retry policy** with exponential backoff for 429/5xx errors
- **Bonita Runtime 10.2.0+** compatible
- All classes in flat package `com.bonitasoft.connectors.telegram`

## Version

`1.0.0-beta.1`

## License

[GPL-2.0](LICENSE)
