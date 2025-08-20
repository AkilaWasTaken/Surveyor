# Surveyor

Surveyor is a lightweight, GUI-based polling plugin built for [Paper](https://papermc.io/).  
It lets players create polls, vote, and view results quickly and seamlessly, all within the game interface.

## ✨ Features

- Interactive menus for creating polls and casting votes
- `/poll` command suite (GUI browsing, create, close, remove, results, help)
- Polls persist to SQLite (default) or MySQL via HikariCP
- PlaceholderAPI expansion for live poll data (`%surveyor_*%`)
- Fully configurable messages and menu layouts (YAML)
- Java 21 and Paper 1.21+ ready

## 📥 Installation

1. Download or build the JAR (see _Building_).
2. Drop it into your server’s `plugins/` folder.
3. Start the server to generate `config.yml`, `menus.yml`, and `messages.yml`.
4. Adjust the configs to taste, then reload/restart.

> PlaceholderAPI is optional but recommended to expose poll info in scoreboards, holograms, etc.

## 🕹️ Commands

| Command                                 | Description                                                                      |
|-----------------------------------------|----------------------------------------------------------------------------------|
| `/poll` or `/polls`                     | Open the active-polls menu.                                                      |
| `/poll help`                            | In‑game command reference.                                                       |
| `/poll create <duration> <question...>` | Start the GUI wizard to create a poll. Duration supports `30m`, `2h`, `1d`, etc. |
| `/poll close <pollId>`                  | Close a poll early.                                                              |
| `/poll remove <pollId>`                 | Permanently delete a poll.                                                       |
| `/poll results <pollId>`                | View poll results in chat.                                                       |

**GUI notes**

- Preset answer sets, custom options via chat, and option shuffling.
- Voters see poll status, remaining time, and total votes live.

## 🔌 PlaceholderAPI

`%surveyor_active_polls%` — number of polls that are still open  
`%surveyor_poll_question_<id>%` — poll question  
`%surveyor_poll_status_<id>%` — `active` / `closed`  
`%surveyor_poll_votes_<id>%` — total votes  
`%surveyor_poll_closes_in_<id>%` — remaining time (`1h30m`, `ended`, or `none`)

Per option (0‑based index):

- `%surveyor_poll_option_<id>_<index>%` — option text
- `%surveyor_poll_option_votes_<id>_<index>%` — votes for that option

Player-specific:

- `%surveyor_has_voted_<id>%` — `yes` / `no`
- `%surveyor_my_vote_index_<id>%` — chosen option index (`-1` if none)
- `%surveyor_my_vote_text_<id>%` — chosen option text (`none` if none)

## ⚙️ Configuration

- `config.yml` — database settings (SQLite or MySQL)
- `menus.yml` — menu titles, slots, item materials, etc.
- `messages.yml` — chat feedback and component text

The plugin ships with sensible defaults; each file is heavily commented for easy tweaking.

## 🛠️ Building from source

Requires JDK 21 and Maven.

```bash
git clone https://github.com/AkilaWasTaken/Surveyor.git
cd Surveyor
mvn clean package
```

## 📄 License

Released under the MIT License. Feel free to fork, modify, and contribute back.