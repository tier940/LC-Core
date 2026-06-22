# LegacyCraft-Core

**A compatibility patch mod for Minecraft 1.12.2 that fixes crashes and broken connections between mods in the [LegacyCraft](https://www.curseforge.com/minecraft/modpacks/legacycraft) modpack.**

---

## What It Fixes

### AkutoEngine + BuildCraft Remastered

Without this mod, the game crashes every time on startup when both AkutoEngine and BuildCraft Remastered are installed together.

**Game crashes on startup**

> AkutoEngine was built against an older version of BuildCraft Remastered. Two internal APIs it relies on were later changed, causing the game to crash before the world even loads. LC-Core silently patches the incompatible calls at startup so both mods can coexist.

---

### Logistics Pipes + BuildCraft Remastered

Installing LP 0.10.4.28 or later breaks the physical pipe connection between Logistics Pipes and BuildCraft Lasers or any Forge Energy machine.

**RF Power Supplier Upgrade stops connecting to Lasers and Forge Energy machines**

> After upgrading LP, a pipe fitted with the RF Power Supplier Upgrade will no longer show a connection stub to any BuildCraft Laser or Forge Energy block — power delivery stops entirely. LC-Core restores the connection so the pipe attaches properly. Fully-charged batteries are not wastefully topped off; LP's own power logic still governs actual delivery.

---

### Logistics Pipes + IC2 Classic

The same LP 0.10.4.28 update also broke connections to IC2 Classic machines, stopping EU power delivery.

**IC2 EU Power Supplier Upgrade stops connecting to IC2 machines**

> A pipe with the EU Power Supplier Upgrade installed will no longer attach to IC2 Classic machines after upgrading LP. LC-Core restores the missing connection check so EU power upgrades work normally again.

---

### Logistics Pipes + EnderIO

LP 0.10.4.28 broke the connection between Logistics Pipes routing pipes and EnderIO item conduits.

**Routing pipes no longer attach to EnderIO item conduits**

> LP pipes adjacent to EnderIO item conduits lose their connection stub after LP 0.10.4.28. LC-Core re-applies the missing check and handles the difference between LP 0.10.4.27–0.10.4.48 and LP 0.10.4.49+ automatically — no crashes across LP versions.

---

### Additional Pipes

AP 6.0.0.8 ships the power teleport pipe but never completes its item registration, making it impossible to obtain or place.

**Power teleport pipe missing from the creative tab**

> The power teleport pipe behavior and texture exist in AP but the item is never registered with the game. Additionally, `PipeBehaviorTeleportPower.requestPower()` always returns 0 due to an incomplete upstream implementation, which prevents any energy from flowing through the pipe. LC-Core registers the missing item and replaces the behavior so the pipe both appears in the creative tab and actually teleports power.

---

### Logistics Pipes + Additional Pipes

LP 0.10.4.28 removed its built-in support for routing through Additional Pipes teleport pipes, making anything on the other side of a teleport pipe effectively unreachable.

**Items and power cannot be routed through AP teleport pipes**

> After the LP update, the routing network has no awareness of teleport pipe connections. Providers placed beyond a teleport pipe become invisible to requesters, and items sent toward a full or missing destination are lost rather than being returned.
>
> LC-Core registers a replacement connection handler, restoring the ability to route items, fluids, and power across AP teleport pipes. Routing information is also correctly carried over when items arrive at the destination through teleportation, so they are properly re-routed if a destination is full or unreachable.
>
> **Supported:** Item teleport pipes · Fluid teleport pipes · Power teleport pipes

---

## About LegacyCraft

[LegacyCraft](https://www.curseforge.com/minecraft/modpacks/legacycraft) is a lightweight Minecraft 1.12.2 modpack that recreates the atmosphere of early tech-mod gameplay (circa 1.2.5 era) with BuildCraft, Logistics Pipes, IC2 Classic, ProjectE, and Project Red. LC-Core is the glue that makes those mods coexist without crashes.
