# Server-Talk

A lightweight [Paper](https://papermc.io/) plugin for Minecraft **1.21.11** that gives server admins two things:

- **Chat clearing tools** – wipe the chat for everyone, or just for yourself.
- **Persistent floating 3D text** – create, style, move, and delete text holograms (built on vanilla `TextDisplay` entities) that survive restarts, with pluggable storage (`NONE`, `JSON`, or `MYSQL`).

No armor stands, no external hologram plugin required.

---

## Features

- Floating 3D text using native `TextDisplay` entities
- Legacy color / format codes with `&` (e.g. `&c&lHello`)
- Multi-line text using `|` as a line separator
- Per-text settings: size, background color and opacity, shadow, see-through, and billboard mode
- Edit by **ID** or simply target the **nearest** text
- Three storage backends, switchable at runtime with automatic migration:
  - `NONE` – nothing is saved
  - `JSON` – saved to a file in the plugin folder
  - `MYSQL` – saved to a MySQL database
- Safe fallback: if the configured storage fails to initialize, the plugin falls back to `NONE` instead of crashing
- Texts are re-spawned automatically when their chunks load, and stale display entities are cleaned up

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Server software | Paper 1.21.11 (or compatible) |
| Java | 21 |
| MySQL (optional) | Any MySQL-compatible server, only if you use `MYSQL` storage |

---

## Installation

1. Download the latest `server-talk-<version>.jar` (or [build it yourself](#building-from-source)).
2. Drop the jar into your server's `plugins/` folder.
3. Start (or restart) the server.
4. A `configvk.yml` file is generated in `plugins/server-talk/`. Adjust it if needed.

---

## Commands

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/clearchat` | `/cc` | Clears the chat for **all** players | `servertalk.clearchat` |
| `/clearmassagechat` | `/cmc` | Clears only **your own** chat | *(none)* |
| `/text3d [size] <text>` | – | Creates floating 3D text in front of you | `servertalk.text3d` |
| `/text3d remove` | – | Removes the nearest 3D text | `servertalk.text3d` |
| `/edittext3d ...` | `/et3d` | Edits a 3D text (see below) | `servertalk.edittext3d` |
| `/deletetext3d <id\|nearest\|all>` | `/dt3d` | Deletes 3D text(s) | `servertalk.deletetext3d` (`all` also needs `servertalk.admin`) |
| `/servertalk <storage\|reload\|list>` | `/st` | Admin commands | `servertalk.admin` |

All permissions default to **OP**.

### Creating text

```
/text3d <text>
/text3d <size> <text>
```

- `size` is optional. Default is `5`, and values are clamped between `0.5` and `50`.
- Use `|` for a new line.
- Use `&` color codes.

**Example:**

```
/text3d 6 &a&lWelcome to the server | &eHave fun!
```

On creation you get an 8-character **ID** (e.g. `a1b2c3d4`). Use `/st list` any time to see all IDs.

### Editing text

You can target a text in three ways:

```
/et3d id <id> <action> ...     # explicit form
/et3d <id> <action> ...        # shorthand
/et3d <action> ...             # targets the nearest text (within 15 blocks)
```

Running `/et3d` with no arguments shows a clickable help menu in chat.

| Action | Usage | Description |
|--------|-------|-------------|
| `info` | `/et3d id <id> info` | Show position, size, billboard, shadow, background, owner |
| `text` | `/et3d id <id> text <new text>` | Replace the text (`\|` = new line, `&` colors supported) |
| `color` | `/et3d id <id> color <&c&lText>` | Replace the text with a colored version |
| `pos` | `/et3d id <id> pos <x> <y> <z>` | Set an exact position |
| `move` | `/et3d id <id> move <dx> <dy> <dz>` | Move relative to the current position |
| `size` | `/et3d id <id> size <value>` | Change size (0.5 – 50) |
| `bg` | `/et3d id <id> bg <r> <g> <b> <a>` | Set background color (each value 0–255) |
| `nobg` | `/et3d id <id> nobg` | Make the background transparent |
| `shadow` | `/et3d id <id> shadow <true\|false>` | Toggle text shadow |
| `seethrough` | `/et3d id <id> seethrough <true\|false>` | Toggle see-through (visible through blocks) |
| `billboard` | `/et3d id <id> billboard <fixed\|vertical\|horizontal\|center>` | Set how the text faces the player |
| `delete` / `remove` | `/et3d id <id> delete` | Delete the text |

**Example:**

```
/et3d id a1b2c3d4 bg 0 0 0 200
/et3d id a1b2c3d4 billboard vertical
/et3d id a1b2c3d4 move 0 1 0
```

### Deleting text

```
/dt3d <id>        # delete a specific text
/dt3d nearest     # delete the nearest text
/dt3d all         # delete ALL texts (requires servertalk.admin)
```

### Admin commands

```
/st storage <none|json|mysql|status>   # switch storage backend or show the current one
/st reload                             # reload configvk.yml
/st list                               # list all loaded texts with IDs and locations
```

When you switch storage, existing texts are **copied** to the new backend, and any texts already stored there are imported. The new mode is written back to `configvk.yml`.

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `servertalk.clearchat` | Use `/clearchat` | OP |
| `servertalk.text3d` | Use `/text3d` | OP |
| `servertalk.edittext3d` | Use `/edittext3d` | OP |
| `servertalk.deletetext3d` | Use `/deletetext3d` | OP |
| `servertalk.admin` | Use `/servertalk` and `/dt3d all` | OP |

---

## Configuration

`plugins/server-talk/configvk.yml`:

```yaml
# Storage mode: NONE | JSON | MYSQL
StorageMode: JSON

Json:
  FileName: 3dtexts.json

MySQL:
  Host: localhost
  Port: 3306
  Database: servertalk
  Username: root
  Password: ""
  Table: 3d_texts
  UseSSL: false
  PoolSize: 4
```

### Storage modes

| Mode | Behavior |
|------|----------|
| `NONE` | Nothing is saved. All 3D texts disappear on restart. |
| `JSON` | Texts are stored in `plugins/server-talk/<Json.FileName>`. Zero setup. |
| `MYSQL` | Texts are stored in the configured table (created automatically with `utf8mb4`). Recommended for multi-server setups or large amounts of text. |

> **Note:** The MySQL database itself (`MySQL.Database`) must already exist; the plugin only creates the table.

---

## Building from Source

Requirements: **JDK 21**. The Gradle wrapper (Gradle 9.4.0) is included, so you don't need to install Gradle.

```bash
git clone https://github.com/BenyVK/Server-Talk.git
cd Server-Talk
./gradlew build        # on Windows: gradlew.bat build
```

The compiled jar will be in `build/libs/`.

---

## Project Structure

```
src/main/java/ir/gharri/servertalk/
├── ServerTalk.java      # Main plugin class, commands, and 3D text logic
├── StoredText3D.java    # Data model for a saved 3D text
├── TextStorage.java     # Storage interface
├── NoneStorage.java     # No-op storage
├── JsonStorage.java     # JSON file storage (Gson)
└── MySqlStorage.java    # MySQL storage (JDBC)

src/main/resources/
├── plugin.yml           # Commands and permissions
└── configvk.yml         # Default configuration
```

---

## Contributing

Issues and pull requests are welcome at [github.com/BenyVK/Server-Talk](https://github.com/BenyVK/Server-Talk).

---

## License

This project is licensed under the [MIT License](LICENSE).

---

Created by **BenyVK** ([Gharri.ir](https://gharri.ir))

## 👤 Credits

| | |
|---|---|
| **Plugin** | Server Talk's |
| **Author** | Benyamin Gharri |
| **YouTube** | [BenyVKx](https://www.youtube.com/@BenyVKx) |
| **Tutorial Video** | Coming soon |
| **Persian README.md** | [README.FA.md](https://github.com/BenyVK/Server-Talk/blob/main/README.FA.md) |
