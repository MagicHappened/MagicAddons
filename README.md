# MagicAddons

MagicAddons is a Fabric mod for Hypixel SkyBlock, currently has a lot of greenhouse features, with some other ones. 
It runs on Minecraft 26.1.2 and 26.2, and will update as Hypixel updates to newer versions.

## Greenhouse

- **Planner**: shows a saved preset's plants in the world, colored by whether each spot is right, missing
  or in the way. Items the planner needs get a highlight in inventories, and a helpful message to get items from sacks/search in storage.
- **Presets**: up to three plots per preset, an in-game editor with undo and redo, target markings for the
  mutations you grow, soft import that fits a preset around what is already in that preset, and import and export
  as MagicAddons, SkyLayouts, SkyMutations and SkyShards codes.
- **Prediction**: see your greenhouse up to 10 ticks ahead, including which plants decay. Spawned
  mutations are chance based and are not predicted.
- **Water**: each plant's water tick by tick, including retain and drain effects, how long until it fully
  grows or halts at -100% water, and a water highlight in the world.
- **Decay**: tracks how many mutations each plant helped spawn, when it can decay, and which plants are
  worth checking with the Plant Diagnostics Tool (highlighted while you hold it).
- **Chorus Collision**: the chance that a teleporting chorus breaks one of your plants at the next tick,
  with a break order telling you which chorus to harvest or break to get under the loss you accept.
- **Break Protection**: blocks breaking mutations under a fortune threshold, while pest debuff is active, before
  they are harvestable, or young chorus outside the suggested break order.
- **Reminders**: ready to harvest, likely spawns of single stage mutations, water, decay, Snoozling asleep,
  Noctilume time, Fleshtrap meat, Thunderling charge, Glasscorn reset and Chorus collision, before and at
  the growth tick, for other profiles too.
- **Growth tick timer and HUD**: set your garden upgrades once and the HUD shows the next tick.
- **Crop Preview** (`/ma gh preview`): a preview of how each crop looks at each stage.
- **Shorter Cane Crops**: Sugar Cane and Magic Jellybean drawn as their bottom block, growth stage above.
  Hitboxes do not change.

## Discord Reminders and Coop

These need **Connect to MagicAddons Server** and **Discord integration**, both off by default. Install the
[MagicAddons Discord app](https://discord.com/oauth2/authorize?client_id=1554839668313427968), then link
with `/ma link` in game and `/link` in Discord.

- **Discord Reminders**: while the game is closed, the app messages you about what is ready to harvest,
  running out of water, about to decay or needs attention, in the next 4 to 24 hours (`/reminders` in
  Discord). `/greenhouse` in Discord shows your greenhouse report at any time.
- **Greenhouse Coop**: coop members running the mod share their plots, and the Greenhouse Coop setting
  decides how reminders behave while a coop member is online.

What the server stores and how to delete it is in the [privacy policy](PRIVACY.md). `/unlink` in Discord
or `/ma privacy DeleteMe` in game deletes everything.

## Other features

- **Foraging**
  - **Safari Helper**: tracks which mobs are still uncaught in each zone and who is assigned where, with
    optional party chat messages and a sparkling warning.
- **Combat**
  - **Mob Highlight**: highlights mobs by name, by an area preset, or picked from the Hypixel and vanilla
    mob lists.
  - **Custom Rend Sound**: plays a configurable sound when a player has rended.
- **Mining**
  - **Pickaxe Ability Cooldown**: a HUD element for the ability cooldown, with an optional ready warning.
  - **Powder Coating Hider**: hides powder coating particles while wearing Divan armor.
  - **XP Orb Hider**: hides experience orbs on the ground, optionally only on mining islands.
- **Misc**
  - **Smol People**: draws other players smaller.
  - **Skip Resource Reload On Close**: a faster shutdown when closing the game on Hypixel.
  - **Appearance**: color palettes, fonts from your computer, backgrounds, and presets you can save and share.
  - **HUD Editor**: drag, resize, fade and group HUD elements, with a layout per location.

## Commands

- `/ma` opens the config; `/ma feature toggle <feature>` and `/ma feature edit <feature>` toggle a
  feature or jump to it.
- `/ma gh` opens the greenhouse screen; `/ma gh preview [crop]` the crop preview; `/ma gh SyncData` sends
  your greenhouse data to the server by hand.
- `/ma hud` opens the HUD editor.
- `/ma safari` for the Safari Helper.
- `/ma link` links your Discord account; `/ma privacy DeleteMe` deletes your server data.
- `/ma version` shows the running version and whether a newer one exists.

## Extension features

Some features only load with `magicaddons-extension-<version>.jar` in your mods folder next to the main
jar. They are used at your own risk, which is why they are separated.

## Beta

The `beta` branch is early testing, expect bugs and very recent version changes. Every push builds jars on
[Actions](https://github.com/MagicHappened/MagicAddons/actions), named `magicaddons-<version>+<minecraft>.beta.<commit>.jar`;
the mod tells you when a newer beta build exists.
