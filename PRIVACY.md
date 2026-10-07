# MagicAddons privacy policy

Last updated: 7 October 2026.

MagicAddons is a Minecraft mod for Hypixel SkyBlock. Most of what it does happens on your own
game client and never leaves it. This policy covers the optional parts that talk to the MagicAddons
server, which is run by Magic (MagicHappened on GitHub). To contact Magic about your data, email
magicaddons@proton.me.

## 1. Connection to the server is on an opt-in basis

The mod sends no data anywhere until you enable **Connect to MagicAddons Server** in its settings.
Greenhouse data is only sent once you also enable **Discord integration**. Both are off by default,
and turning them off stops any further sending at once.

## 2. What the server stores

When the server features are on, the server stores:

- **Your Minecraft account id (UUID) and player name.** The UUID is proven at login with the key
  Mojang issues to your game client, so that nobody can impersonate you and contact the server on
  your behalf. The name is sent by the mod so that coop messages in Discord can name who did what in
  the greenhouse.
- **Your Discord user id**, only after you link it with `/link` in Discord. It is used to send you
  direct messages and to answer your Discord commands.
- **Your SkyBlock profile id and profile name.**
- **Your greenhouse data**: the plots, the plants on them with their growth stage, water, readings
  from the Plant Diagnostics Tool and whether you placed them, your planner marks, and the mod
  settings that decide what you are warned about (harvest stages, warning types, chorus tolerance,
  the Greenhouse Coop choice).
- **Activity counts**: how many plants you harvested, placed and watered since your previous upload,
  and which plots that was in.
- **Presence**: whether you are online and whether you are in your garden, refreshed about every
  15 minutes while the features are on, the times of your recent uploads and new profiles, to limit
  abuse, and when you last left the game, so reminders know how long you have been away.
- **Your IP address**, only to limit login attempts. It is kept for 10 minutes and is not tied to
  your account.

The server never sees your Minecraft password, your Mojang or Microsoft login, your chat, your
inventory or anything outside the greenhouse features described here.

## 3. What it is used for

- Discord direct messages about your greenhouse while the game is closed (reminders and reports).
- Keeping a greenhouse in sync between members of the same SkyBlock coop profile, and sending
  Discord notifications about what their coop members did.
- Limiting abuse of the server (login and upload rate limits).

## 4. Who else sees it

- **Cloudflare** hosts the server and stores the data above on your behalf. Cloudflare also keeps
  its own request logs, which include IP addresses, for its usual retention period. Cloudflare's
  privacy policy can be found here: https://www.cloudflare.com/privacypolicy/
- **Discord** receives the direct messages and slash command replies, and keeps them under its own
  policy: https://discord.com/privacy
- **Hypixel** receives your UUID and your profile id when the server checks that you are a member of
  a coop profile. This check only happens when two accounts upload the same profile. Hypixel's
  privacy policy: https://hypixel.net/privacy-policy
- **Your coop members.** If you and another player both use the mod on the same coop profile, each
  of you can see game information relevant to the profile you are playing together: the plants and
  readings on the shared plots, placed flags, the activity counts, the Minecraft name, and whether
  the other is online or in the garden. Your planner marks, plot names, settings and Discord link
  are not shared. The **Greenhouse Coop** setting and `/reminders coop` in Discord control what you
  receive, not what is shared; to stop sharing, turn off Discord integration.

Nothing is sold, and nothing is used for advertising or profiling.

## 5. How long it is kept

- Greenhouse data and activity counts: deleted 14 days after your last upload.
- Login attempts (IP address): 10 minutes.
- Link codes shown by `/ma link`: 10 minutes.
- Your Discord link, your Minecraft UUID and name, your settings and your reminder state: until you
  unlink.

## 6. How to delete it

- Run `/unlink` in Discord. This deletes everything the server stores for your Minecraft account at
  once: the Discord link, all greenhouse data, settings and reminder state. Your coop members'
  copies of shared plots are not affected, since those are their own uploads.
- Run `/ma privacy DeleteMe` in game. It does the same as `/unlink`, from the game instead of
  Discord, and asks you to run it a second time within 20 seconds to confirm.
- Turning off **Connect to MagicAddons Server** stops all sending; stored data then expires as
  listed above.
- If you cannot use Discord, email magicaddons@proton.me with your Minecraft name and the data
  will be deleted by hand.

## 7. Changes

This policy lives at https://github.com/MagicHappened/MagicAddons/blob/beta/PRIVACY.md. Changes are listed in the release notes
of the version that makes them.
