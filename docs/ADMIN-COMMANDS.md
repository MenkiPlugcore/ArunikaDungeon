# Implemented commands and permissions

Commands below were read from plugin.yml and command handlers. Argument syntax is case-sensitive inside several subcommands; use the lowercase spelling shown.

| Command | Permission/condition | Purpose |
|---|---|---|
| `/adg status`, `diagnostics`, `legacy`, `help` | `arunika.dungeon.admin` | Read status, measured counts, legacy paths |
| `/adg admin` | Admin, player | Native admin GUI |
| `/adg reload [all\|id]` | Admin; no active operations | Validate/apply config |
| `/adg create <id> <number> [name]` | Admin | Create floor without overwriting another |
| `/adg gateway` | Admin, player looking at anchor | Inspect Waystone UUID |
| `/adg gateway check` | Admin | Public ABI and binding diagnostics |
| `/adg gateway open` | Player; gateway permission and physical anchor validation | Bedrock/native fallback |
| `/adg setup arena <floor> <id> <shared\|private>` | Admin, player | Start arena draft |
| `/adg setup resume` | Admin, player | Resume coordinate capture |
| `/adg setup boss <floor> <Mythic-ID>` | Admin; MythicMobs present | Set boss |
| `/adg setup wave <floor> <number> <Mythic-ID> <amount>` | Admin | Replace one wave's mob list |
| `/adg setup append-wave <floor> <number> <Mythic-ID> <amount>` | Admin | Append mob group |
| `/adg setup count <floor> <1..30>` | Admin | Active wave count |
| `/adg setup enable\|disable <floor>` | Admin | Toggle battle configuration |
| `/adg loot list <floor> <shared\|private>` | Admin | Display tables |
| `/adg loot addhand <floor> <mode> <wave-1\|boss> <key> <chance> <min> <max>` | Admin, held non-ticket | Save exact item template |
| `/adg loot remove <floor> <mode> <stage> <key>` | Admin | Remove named drop entry |
| `/adg shared join <floor>`, `leave`, `status [floor]` | `arunika.dungeon.shared.test`; admin also routes here | Shared queue/status |
| `/adg private create <floor>`, `invite <player>`, `accept`, `decline` | `arunika.dungeon.private.test` | Private party |
| `/adg private ready`, `unready`, `kick <player>`, `transfer <player>`, `cancel`, `leave`, `start`, `status` | Private test permission; leader checks where relevant | Ready/queue/party actions |
| `/adg ticket give <player>`, `inspect <serial>`, `recover <serial> <session>` | Admin; recover only verified unresolved SPENT session | Staff ticket operations |
| `/adg ticket claim <serial>` | Private test permission, recorded claimant | Pending claim |
| `/adg preview start <floor> <arena> [online-admin-names...]` | Admin, survival/adventure, gateway disabled | No ticket or loot battle preview |
| `/adg preview status`, `stop <session>`, `point`, `exit` | Admin | Preview/exit support |
| `/dg`, `/dg claim <serial>` | `arunika.dg.use` and `arunika.dungeon.exchange.test` | Exchange menu/claim |
| `/opdg status`, `reload`, `rewards`, `token set` | `arunika.dg.admin` or dungeon admin at handler; Bukkit root command permission also applies | Exchange administration |
| `/opdg ticket <cost> <on\|off>` | Exchange admin | Universal Ticket offer |
| `/opdg exchange <on\|off>` | Exchange admin plus `arunika.dg.toggle` | Transaction enable switch |
| `/opdg addhand <new-key> <cost> <category>` | Exchange admin, held non-ticket | Add item reward |
| `/opdg cost <key> <cost>`, `category <key> <name>`, `enable <key> <true\|false>` | Exchange admin | Edit ordinary reward |
| `/opdg tx list` | Exchange admin, not busy | Pending ADG transactions |
| `/opdg tx resolve <id> <no-debit\|delivered>` | Exchange admin and OP | Verified manual settlement |

`/adg exchange admin ...` is the implemented admin fallback. All `*.test` permissions default to OP; they are retained in staging. Gateway also requires `arunika.dungeon.gateway.test`, `cdrwaystone.use`, `cdrwaystone.dungeon.use`, plus mode permission. When gateway is enabled, direct mutating Shared/Private commands are refused; use GUI. No `/adg gateway set` or automatic stone-registration command exists.
