# MagicAddons privacy policy

Last updated: 4 November 2026.

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
- **Your greenhouse data**: your greenhouse plots including plants and their data, your mod's greenhouse settings.
  When a greenhouse runs a preset plot with steps, the server stores the preset plot's
  name, the current step, the step's exit condition, the progress towards the exit condition, and a prediction
  of when it will be done.
- **Coop plans**, on a coop profile, when a member shares a plan with the **Share to coop** button: the plan
  itself (its layout, preset, plot and step names), the Minecraft name and UUID of the member who
  shared it, when, the current step, who last changed the step, and how many plants each member
  harvested on that step.
- **A log of coop plan changes**: your Minecraft UUID, the time, and what you did (shared,
  cleared, changed a step, or moved a plan to a new greenhouse), with the plot, plan and step names.
  It's used by the Discord coop report and limits how often a member can change plans.
- **Activity and presence**: how many plants you harvested, placed and watered, whether you are
  online and in your garden, and when you last uploaded or left the game.
- **Your IP address**, only to limit login attempts, and not tied to your account.

The mod is open source, so exactly what is sent can be read in its code:
https://github.com/MagicHappened/MagicAddons

The server never sees your Minecraft password, your Mojang or Microsoft login, or your chat.

## 3. What it is used for

- Discord direct messages about your greenhouse while the game is closed (reminders and reports),
  including when a plan step is probably done.
- Keeping a greenhouse and its coop plan in sync between members of the same SkyBlock coop
  profile, and sending Discord notifications about what their coop members did.
- Limiting abuse of the server (login, upload and coop plan rate limits).

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
  (or UUID if no name is stored), and whether the other is online or in the garden. Coop members
  also see every coop plan in full, who shared it, who changed its step, and the harvest totals on
  it, and their mods keep a copy of the coop plans on their own computer. Your own presets, plot
  names, planner marks and Discord link are not shared, except a plan you choose to share with
  **Share to coop**. Your settings are not shared either, except **Greenhouse Coop**: the strictest
  setting in the coop applies to everyone in it, including how often the other members' mods
  upload. To stop sharing, turn off Discord integration.

Nothing is sold, and nothing is used for advertising or profiling.

## 5. How long it is kept

- Greenhouse and activity data: deleted 14 days after your last upload from that profile.
- Your name, presence and Greenhouse Coop choice in a profile: deleted 14 days after the mod last
  contacted the server from that profile.
- Coop plans: kept while the coop uses them. A plan is deleted when a member clears it, or 7 days
  after its greenhouse last appeared in any member's upload.
- The log of coop plan changes: 14 days.
- Login attempts (IP address) and link codes shown by `/ma link`: up to 20 minutes.
- Your Discord link, your Minecraft UUID and name, your settings and your reminder state: until you
  unlink. After unlinking, your UUID stays only in the log of coop plan changes and a note of when
  you left, both deleted at the next midnight UTC, to prevent rate limit abuse.

## 6. How to delete it

- Run `/unlink` in Discord. This deletes everything the server stores for your Minecraft account,
  with these exceptions:
  - Coop plans you shared stay for your coop, with your name and UUID removed; they show "a coop
    member" instead. Your coop members' copies of shared plots are not affected either, since those
    are their own uploads.
  - Your entries in the log of coop plan changes are deleted at the next midnight UTC, so that
    unlinking and linking again cannot get around the coop plan rate limits. Until then the server
    also keeps a note of when you left, all deleted at the same time.
  - Copies outside the server can't be deleted by Magic. A coop member's mod saves the coop plans
    on their own computer, including the Minecraft name of whoever shared a plan or changed its
    step. The server removes your name from its copy when you unlink, and their mod replaces it the
    next time it connects, but a coop member who never connects again keeps your name in that
    file. Chat lines their game already showed, their game logs, and Discord messages already sent
    to them stay with them too.
- Run `/ma privacy DeleteMe` in game. It does the same as `/unlink`, from the game instead of
  Discord, and asks you to run it a second time within 20 seconds to confirm.
- Turning off **Connect to MagicAddons Server** stops all sending; stored data then expires as
  listed above.
- If you cannot use Discord, email magicaddons@proton.me with your Minecraft name and the data
  will be deleted manually.

## 7. Changes

This policy is on https://magichappened.github.io/MagicAddons/privacy/, which shows the policy of
the main and the beta version, each loaded from GitHub with a link to its full history.
