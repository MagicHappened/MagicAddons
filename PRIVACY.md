# MagicAddons privacy policy

Last updated: 16 October 2026.

MagicAddons is a Minecraft mod for Hypixel SkyBlock. Most of what it does happens on your own
game client and never leaves it. This policy covers the optional parts that send data to the MagicAddons
server, which is run by Magic (MagicHappened on GitHub). To contact Magic about your data, email
magicaddons@proton.me.

## 1. Connection to the server is on an opt-in basis

The mod sends nothing to the MagicAddons server until you enable **Connect to MagicAddons Server**
in its settings. Greenhouse data is only sent once you also enable **Discord integration**. Both are
off by default, and turning them off stops any future data from being sent.

Separately, the mod checks GitHub for a newer version when you change islands. GitHub sees your IP
address and, on the beta version, which commit you are running.

## 2. What the server stores

When the server features are on, the mod sends some of your greenhouse data, your Minecraft UUID
and name, and your SkyBlock profile id and name. The server stores:

- **Your Minecraft UUID and name.** The UUID is proven at login with the key Mojang issues to your
  game client, so nobody can pretend to be you. The name lets coop messages
  in Discord say who did what.
- **Your Discord user id**, once you link it with `/link` in Discord, to send you direct messages
  and answer your Discord commands. A wrong `/link` code also stores it for up to 20 minutes, to
  limit guessing.
- **Your SkyBlock profile id and name.**
- **Your greenhouse data**: your plots and the plants on them, and the mod settings that decide
  what you are reminded about.
- **Activity and presence**: how many plants you harvested, placed and watered, whether you are
  online and in your garden, and when you last uploaded or left the game.
- **Your IP address**, only to limit login attempts, and not tied to your account.

The mod is open source, so exactly what is sent can be read in its code:
https://github.com/MagicHappened/MagicAddons

The server never sees your Minecraft password, your Mojang or Microsoft login, or your chat.

## 3. What it is used for

- Discord direct messages about your greenhouse while the game is closed (reminders and reports).
- Keeping a greenhouse in sync between members of the same SkyBlock coop profile, and sending
  Discord notifications about what their coop members did.
- Limiting abuse of the server (login and upload rate limits).

## 4. Who else sees it

- **Cloudflare** hosts the server and stores the data above on your behalf. Cloudflare also keeps
  request logs, which include IP addresses and which Magic can look at to fix problems. Cloudflare's
  privacy policy: https://www.cloudflare.com/privacypolicy/
- **Discord** receives the direct messages and slash command replies, and keeps them under its own
  policy: https://discord.com/privacy
- **Hypixel** receives your profile id when the server checks that two accounts uploading the same
  profile are members of it. The result is kept for 3 days. Hypixel's privacy policy:
  https://hypixel.net/privacy-policy
- **Your coop members.** If you and another player both use the mod on the same coop profile, each
  of you can see the shared plots and their plants, the activity counts, the other's Minecraft name
  (or UUID if no name is stored), and whether the other is online or in the garden. Your plot names,
  planner marks and Discord link are not shared. Your settings are not shared either, except
  **Greenhouse Coop**: the strictest choice in the coop applies to everyone in it, including how
  often the other members' mods upload. To stop sharing, turn off Discord integration.

Nothing is sold, and nothing is used for advertising or profiling.

## 5. How long it is kept

- Greenhouse and activity data: deleted 14 days after your last upload from that profile.
- Your name, presence and Greenhouse Coop choice in a profile: deleted 14 days after the mod last
  contacted the server from that profile.
- Login attempts (IP address) and link codes shown by `/ma link`: up to 20 minutes.
- Your Discord link, your Minecraft UUID and name, your settings and your reminder state: until you
  unlink.

## 6. How to delete it

- Run `/unlink` in Discord. This deletes everything the server stores for your Minecraft account at
  once: the Discord link, all greenhouse data, settings, reminder state and pending link codes.
  Your coop members' copies of shared plots are not affected, since those are their own uploads.
- Run `/ma privacy DeleteMe` in game. It does the same as `/unlink`, from the game instead of
  Discord, and asks you to run it a second time within 20 seconds to confirm.
- Turning off **Connect to MagicAddons Server** stops all sending; stored data then expires as
  listed above.
- If you cannot use Discord, email magicaddons@proton.me with your Minecraft name and the data
  will be deleted by hand.

## 7. Changes

This policy lives at https://github.com/MagicHappened/MagicAddons/blob/master/PRIVACY.md. Changes
are listed in the release notes of the version that makes them.
