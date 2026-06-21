# LegacyCraft-Core

**A Forge CoreMod for Minecraft 1.12.2 that silently fixes runtime crashes and missing connections caused by API mismatches between the mods in the [LegacyCraft](https://www.curseforge.com/minecraft/modpacks/legacycraft) modpack.**

All patches are applied transparently at class-load time using ASM bytecode transformation. No config needed, no gameplay changes — it simply makes incompatible mods work together.

---

## What It Fixes

### AkutoEngine + BuildCraft Remastered

flyingperson23's BuildCraft Remastered changed two internal APIs after AkutoEngine 2.0.13 was released. Without this CoreMod, the game crashes immediately on startup with `NoSuchMethodError`.

**Gate recipe crash on startup**

> AkutoEngine calls `new GateVariant(logic, material, modifier)` with 3 arguments, but BuildCraft Remastered dropped the `EnumGateModifier` parameter from the constructor. LC-Core rewrites the call site in `ObjHandler.registerRecipes()` to discard the unused modifier and invoke the correct 2-argument constructor instead.

**Engine MJ power crash**

> AkutoEngine calls `getReceiverToPower(TileEntity, EnumFacing)` with 2 arguments, but BuildCraft Remastered removed the `TileEntity` parameter. LC-Core rewrites every call site in `TileEntityAkutoEngineBase` to drop the `TileEntity` from the stack and invoke the correct 1-argument version — allowing engines to push MJ power normally.

---

### Logistics Pipes 0.10.4.28+ + BuildCraft Remastered / IC2 Classic

LP 0.10.4.28 quietly removed three pipe-connection checks that existed in 0.10.4.27. As a result, pipes with power-related upgrades installed lose the ability to connect to adjacent machines entirely.

**RF Power Supplier Upgrade does not connect to BC Laser or Forge Energy machines**

> LP 0.10.4.28+ removed the Forge Energy receiver check from `canPipeConnect_internal()`, so a Logistics Pipe with the RF Power Supplier Upgrade installed never forms a physical connection to BuildCraft Lasers or any `IEnergyStorage`-capable block.
>
> LC-Core restores this check using `tile.hasCapability(CapabilityEnergy.ENERGY, facing)`. Crucially, it checks for *capability presence* rather than battery state, so the pipe stub is always shown on Forge Energy blocks regardless of whether the battery is currently full. Actual power delivery is still gated by LP's own logic (`requestRFPower()` → `isEnergyReceiver()`), so no power is wasted to already-full batteries.

**IC2 EU Power Supplier Upgrade does not connect to IC2 machines**

> The same LP 0.10.4.28 change also removed the IC2 energy-sink check. LC-Core restores `IC2Proxy.isEnergySink()` connectivity, guarded by the pipe's `getIC2PowerLevel() > 0` condition, so EU power upgrades reconnect to IC2 Classic machines.

**EnderIO conduit connection (LP 0.10.4.27 – 0.10.4.48)**

> LP 0.10.4.28 removed the `enderIOProxy.isItemConduit()` check. LP 0.10.4.49 went further and deleted the `SimpleServiceLocator.enderIOProxy` field entirely. LC-Core detects at transform time whether the field is present and emits the EnderIO conduit check only when it exists — no crashes across LP versions.

---

## Additional Robustness

**Automatic MCP/SRG method name detection**

> In production Minecraft jars, `EnumFacing.getOpposite()` is obfuscated to `func_176734_d`. In development environments (after `rfg.deobf()`), it appears as `getOpposite`. LC-Core scans the Logistics Pipes bytecode at transform time to detect which name is in use and emits the matching call automatically — no environment-specific configuration required.

---

## About LegacyCraft

[LegacyCraft](https://www.curseforge.com/minecraft/modpacks/legacycraft) is a lightweight Minecraft 1.12.2 modpack that recreates the atmosphere of early tech-mod gameplay (circa 1.2.5 era) with BuildCraft, Logistics Pipes, IC2 Classic, ProjectE, and Project Red. LC-Core is the glue that makes those mods coexist without crashes.
