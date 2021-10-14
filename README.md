# ChestRefill
A serverside mod that refills loot containers.

## Features
* configurable (time, max fills, reset loot seed ...)
* permission support (`chestrefill.allowReloot`)
* works on any container extending `RandomizableContainerBlockEntity`

## Permissions

* `chestrefill.allowReloot` - allows relooting chest
* `chestrefill.config`
  * `chestrefill.config.edit` - allows in-game config editing
  * `chestrefill.config.reload` - allows reloading config
 
## Config

You can use an in-game `/chestrefill` command to edit the config in game.
Available options:
```toml
# Whether to randomize loot table seed."
# This ensures that regenerated loot is different each time.
# (default = true)
randomize_loot_seed = true

# Whether to allow players to reloot containers
# even if they don't have `chestrefill.allowReloot` permission.
# (default = false)
allow_reloot_without_permission = false

# Max refills per container, inclusive. -1 for unlimited.
# (default = 5)
max_refills = 5

# Whether to add loot even if container has some items already.
# (default = false)
refill_non_empty = false

# Minimum wait time to refill the loot, in seconds.
# (default = 14400 (=4 hours))
min_wait_time = 14400
```

## Per-loot-table customization

You can also set custom values for specified loot tables.
Do you want the end city loot table refilled more times than default?
Set the following options.

```json

"// Map to override above config for certain loot tables only.": "",
"lootModifierMap": {
  "minecraft:chests/end_city_treasure": {
    "randomize_loot_seed": true,
    "allow_reloot_without_permission": true,
    "max_refills": 100,
    "refill_non_empty": false,
    "min_wait_time": 60
  },
  "sample_mod:chests/custom_loot_table": {
    "randomize_loot_seed": true,
    "allow_reloot_without_permission": false,
    "max_refills": 5,
    "refill_non_empty": false,
    "min_wait_time": 14400
  }
}
```

This will cause the `minecraft:chests/end_city_treasure` to be
* relootable by same players
* refillanle `100` times
* refilled each `60` seconds


## Per-container customization

You can customize a chest using the `NBT` tags. These will override
the config defaults.

```nbtt
{
    ChestRefill: {
            RefillCounter: 0,
            SavedLootTable: "minecraft:chests/igloo_chest",
            CustomValues: {
                AllowReloot: 1b
                MaxRefills: -1
                MinWaitTime: 60
            }
    }
}
```

This will create a lootable container that can be
* relooted by same players
* refilled infinitely
* refilled every 60 seconds
