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
