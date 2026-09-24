# MagicAddons

MagicAddons is a Fabric mod for Hypixel SkyBlock that currently has many greenhouse features, with a few other niche ones.
Made for Minecraft 26.1.2 and 26.2, with plans to update to newer versions as Hypixel does too.

## Farming

- **Greenhouse Planner**: shows the plants of a saved preset in the world, colored by whether each spot is right, missing, or obstructed.
- **Greenhouse Presets**: up to three plots per preset, with an in-game editor, target markings for the mutations you are growing, and import and export in several formats.
- **Break Protection**: several customizable options for preventing breaking crops in your greenhouse.
- **Greenhouse Prediction**: lets you visually see what your greenhouse will look like up to 10 ticks ahead of time (doesn't include spawned mutations since they are chance based).
- **Water Model**: predicts each plant's water tick by tick, including retain and drain effects, and how long a plant has until it fully grows or dies (assumes no player interaction).
- **Highlights**: water and harvest highlights in the world.
- **Reminders**: ready to harvest, needs water, decay, snoozling asleep, noctilume time switch, chorus collision, with reminders before and at the growth tick (with support for multiple profiles).
  Some reminders might not behave correctly, as I did not have the crops to test them myself; please open an issue if something is off.

## Foraging

- **Safari Helper**: highlights and tracks which mobs are still uncaught in each zone and which player is assigned to it in the HUD, with optional messages to party chat for zone completions, and a warning when a sparkling is detected.

## Combat

- **Mob Highlight**: highlights mobs by name, by a preset for an area, or picked from the Hypixel and vanilla mob lists.
- **Custom Rend Sound**: detects when a player has rended and plays a configurable sound.

## Mining

- **Pickaxe Ability Cooldown**: shows the cooldown of your pickaxe ability as a HUD element, with an optional warning when it is ready.
- **Powder Coating Hider**: hides powder coating particles while wearing Divan armor.

## Misc

- **Smol People**: draws other players visually smaller.
- **Appearance**: customize the mod's UI to your own liking, with presets you can save and share.

## Commands

- `/ma` opens the config; `/ma toggle <feature>` and `/ma edit <feature>` toggle a feature or go to it in the menu.
- `/ma GreenhouseScreen` opens the greenhouse planner and preset editor (`/ma gh` for short).
- `/ma hud` opens the HUD editor.
- `/ma version` shows the running version and whether a newer one exists.

## Extension features

Some features are only available with the extension jar (`magicaddons-extension-<version>.jar`) in your mods folder next to the main one.
These features are used at your own risk, which is why they are gated this way.
Currently the extra features are Through Walls in both Mob Highlight and Safari Helper, and Highlight Markers.
