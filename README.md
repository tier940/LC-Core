# LegacyCraft-Core

**A compatibility patch mod for Minecraft 1.12.2 that fixes crashes and broken connections between mods in the [LegacyCraft](https://www.curseforge.com/minecraft/modpacks/legacycraft) modpack.**

---

## What It Fixes

- AkutoEngine + BuildCraft Remastered
  - fixes the startup crash when both are installed

- BuildCraft Remastered (Assembly Table)
  - crafted items now leave through a consistent output side instead of a random adjacent pipe each time

- Logistics Pipes + BuildCraft Remastered
  - restores RF Power Supplier Upgrade connections to Lasers and Forge Energy machines

- Logistics Pipes + IC2 Classic
  - restores EU Power Supplier Upgrade connections to IC2 machines

- Logistics Pipes + EnderIO
  - restores routing pipe attachment to EnderIO item conduits

- Logistics Pipes (world save/load)
  - in-transit item counts are now persisted across world reloads, preventing duplicate deliveries or re-sent crafting ingredients when the world is closed mid-transport

- Additional Pipes (Power Teleport Pipe)
  - adds the missing item, a crafting recipe (Item Teleport Pipe + Redstone Dust), and working energy logic; all orientations work correctly

- Logistics Pipes + Additional Pipes
  - restores item, fluid, and power routing across all three AP teleport pipe types
  - items on 1-to-many teleport channels now route to the correct receiver rather than scattering randomly or dropping on the ground
  - Provider Pipes next to a teleport pipe now dispatch the full requested amount in one tick instead of dribbling one stack per tick
  - Request Pipes and Request Tables can now reach providers on the far side of an item teleport pipe

---

## About LegacyCraft

[LegacyCraft](https://www.curseforge.com/minecraft/modpacks/legacycraft) is a lightweight Minecraft 1.12.2 modpack that recreates the atmosphere of early tech-mod gameplay (circa 1.2.5 era) with BuildCraft, Logistics Pipes, IC2 Classic, ProjectE, and Project Red. LC-Core is the glue that makes those mods coexist without crashes.
