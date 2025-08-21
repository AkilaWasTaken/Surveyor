# Surveyor

> This plugin is still under heavy development and might contain bugs. Feel free to report them.
>
Surveyor is a lightweight polling plugin for [Paper](https://papermc.io/).  
It lets players create polls, vote, and view results through a simple in-game menu.

## Features

- Menu-based poll creation and voting
- `/poll` command suite (browse, create, close, remove, results, help)
- Poll data is stored in SQLite by default, or MySQL if configured (HikariCP included)
- PlaceholderAPI support for live poll info
- Configurable messages and menu layouts (YAML)
- Built for Java 21 and Paper 1.21+

> PlaceholderAPI is optional but recommended if you want to expose poll stats on scoreboards, holograms, etc.

## Commands

| Command                                 | Description                                                                 |
|-----------------------------------------|-----------------------------------------------------------------------------|
| `/poll` or `/polls`                     | Opens the active polls menu                                                 |
| `/poll help`                            | Shows the in-game help page                                                 |
| `/poll create <duration> <question...>` | Starts the poll creation wizard (duration supports `30m`, `2h`, `1d`, etc.) |
| `/poll close <pollId>`                  | Closes a poll early                                                         |
| `/poll remove <pollId>`                 | Deletes a poll permanently                                                  |
| `/poll results <pollId>`                | Displays results in chat                                                    |

### Notes

- Answer options can be predefined or entered through chat.
- Options can be shuffled.
- Menus show poll status, remaining time, and total votes in real time.

## PlaceholderAPI

General:

- `%surveyor_active_polls%` → number of open polls
- `%surveyor_poll_question_<id>%` → poll question
- `%surveyor_poll_status_<id>%` → `active` / `closed`
- `%surveyor_poll_votes_<id>%` → total votes
- `%surveyor_poll_closes_in_<id>%` → time left (`1h30m`, `ended`, or `none`)

Per option (0-based index):

- `%surveyor_poll_option_<id>_<index>%` → option text
- `%surveyor_poll_option_votes_<id>_<index>%` → votes for that option

Player-specific:

- `%surveyor_has_voted_<id>%` → `yes` / `no`
- `%surveyor_my_vote_index_<id>%` → chosen option index (`-1` if none)
- `%surveyor_my_vote_text_<id>%` → chosen option text (`none` if none)

## Building from source

Requires JDK 21 and Maven.

```bash
git clone https://github.com/AkilaWasTaken/Surveyor.git
cd Surveyor
mvn clean package
The built JAR will be in target/.
```

## License

Surveyor is released under the MIT License.
Feel free to fork it, hack on it, and contribute back if you’d like.