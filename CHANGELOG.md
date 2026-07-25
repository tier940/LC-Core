# Changelog

All notable changes to LC-Core are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [1.3.1]

### Fixed

- **Logistics Pipes: MK2/MK3 pipes displayed incorrect textures**
    - Provider MK2, Crafting MK2, and Crafting MK3 pipes were missing `getCenterTexture()` overrides, causing them to render with the same texture as their base variants.

- **Logistics Pipes: MK2/MK3 crafting recipes did not match the existing Logistics Pipes recipe system**
    - Recipes now use the Programmer item and `RecipeManager` system, consistent with all other Logistics Pipes recipes.
    - MK2 recipes use Advanced Chip; MK3 recipes use both FPGA Chip and Advanced Chip.
    - Pipes and modules are registered with the Program Compiler categories so they can be researched.

* * *

## [1.3.0]

### Added

- **Provider Module MK2 and Provider Logistics Pipe MK2**
    - 128 items per tick (16x), 8 stacks per tick (8x), Fast item send mode, 2x energy cost.
    - Includes the per-tick inventory cache and teleport pipe adjacency boost that were previously applied to all providers via Mixin.

- **Crafting Module MK2 and Crafting Logistics Pipe MK2**
    - 64 items per tick (64x), Fast item send mode, 1.5x energy cost.

- **Crafting Module MK3 and Crafting Logistics Pipe MK3**
    - 128 items per tick, 8 stacks per tick, Fast item send mode, 2x energy cost.
    - 16-slot internal buffer (127 items each) that holds crafted output when the destination is full.
    - Buffered items are automatically pushed back into the adjacent crafter when new orders arrive, or routed into the network when idle.
    - Buffer contents are dropped when the pipe is removed and persisted across world saves.

### Changed

- **BuildCraft, Additional Pipes, AkutoLib, and AkutoEngine are now optional dependencies**
    - The game can now start with only Logistics Pipes and MixinBooter installed. Fixes for optional mods are applied only when those mods are present.

- **Logistics Pipes: Speed Upgrade is significantly more effective**
    - The speed coefficient per upgrade has been increased from 0.02 to 0.10 (5x). With 20 upgrades, item speed goes from 0.28 to 0.60; 40 upgrades reaches the maximum speed of 1.0.
    - The energy cost coefficient has been increased proportionally (0.03 to 0.15) to match.

- **Logistics Pipes: Extractor Module is significantly faster**
    - Base extraction interval reduced from 80 ticks (~4 seconds) to 20 ticks (1 second), a 4x improvement.
    - Base items extracted per cycle increased from 1 to 8.
    - With 4 Action Speed Upgrades installed, the extractor now operates every tick.
    - The Advanced Extractor Module receives the same improvements.

* * *

## [1.2.5]

### Fixed

- **BuildCraft Assembly Table: crafted items went to the wrong pipe even when only one output pipe was connected**
    - The table picked an output pipe at random each time crafting completed, so items could leave through any adjacent pipe regardless of how the automation was arranged.
    - Items now leave through the first available pipe in a consistent direction, making the output side predictable and controllable through pipe placement alone.

- **Logistics Pipes re-sent ingredients to the assembly table after reloading the world mid-craft, causing duplicate crafting or items stuck in pipes**
    - In-transit item counts were not saved when the world closed, so Logistics Pipes incorrectly believed no ingredients were on the way and dispatched a second batch on the next load. The in-transit state is now persisted and restored on load, and each restored entry is given a fresh 640-tick delivery window, so Logistics Pipes correctly waits for ingredients already in the pipe network to arrive.

- **Game crashed when Logistics Pipes routed items through an item teleport pipe in certain setups**

### Changed

- **Reduced server lag caused by Logistics Pipes provider modules placed next to item teleport pipes**

* * *

## [1.2.4]

### Changed

- **Internal stability improvements**
    - Mod integration code has been reorganised to reduce the chance of conflicts with other mods.

### Added

- **MixinBooter is now a required dependency**
    - MixinBooter 10.7 or later must be installed alongside LC-Core.

* * *

## [1.2.3]

### Fixed

- **AP teleport pipes took up to 30 seconds to start working after placement**
    - Placing an AP teleport pipe next to an LP routing pipe, or changing the teleport frequency, now takes effect almost immediately instead of after a delay of up to 30 seconds.

- **Game crashed on startup**
    - A crash that could occur during startup in certain environments has been fixed. The Power Teleport Pipe may not appear in the creative tab in some environments, but works correctly in all other respects.

- **Logistics Pipes can request items through an item teleport pipe**
    - A Request Pipe or Request Table reaches providers on the far side of an item teleport pipe. When several teleport pipes share a channel, duplicate remote pipes are suppressed so requests do not over-dispatch.

- **Items no longer dribble out one stack per tick when sent through an item teleport pipe**
    - A Provider Pipe placed next to an item teleport pipe now dispatches the full requested amount in one tick instead of one stack per tick. Without the teleport, the per-tick throttle was masked by transport delay; with the teleport each stack arrived instantly at the destination, making the chunking visible.

- **Items teleported over a 1-to-many channel no longer scatter across receivers, vanish on the ground, or fixate on the wrong destination**
    - When several receivers shared the same channel, each transported item picked a receiver at random — splitting a single request across multiple destinations and, if no receiver had a pipe attached, dropping the item on the floor. Items now route to the receiver whose adjacent Logistics Pipes graph has the shortest routing distance to the requested destination router, so a request to Request Pipe B is delivered to the receiver that actually neighbours Router B, even when Router A and Router B happen to be near the same teleport receiver. When no receiver is reachable at all, the item is consumed cleanly instead of being dropped.

* * *

## [1.2.2]

### Fixed

- **Item Teleport Pipe: game crashed when the receiver pipe was connected on both ends (straight-pipe layout)**
    - Placing the receiver in a line (e.g. pipes coming from the north and leaving to the south) caused an immediate crash when items arrived.
    - Items now arrive correctly regardless of how the receiver pipe is oriented.

- **Fluid Teleport Pipe: fluids backed up when multiple sender pipes shared the same channel**
    - With two or more senders on the same channel, each sender tried to push the full tank capacity at once. All but one would fail, stalling the pipes behind them.
    - Senders now split the available capacity evenly, so all of them can transfer fluids at the same time without blocking each other.


* * *

## [1.2.1]

### Fixed

- **Power Teleport Pipe appeared as a missing-texture item in the inventory**
    - The item now correctly displays its pipe texture.

- **Power Teleport Pipe: energy flowed backwards into the network when the receiver faced up or down**
    - When a receiver pipe had a kinesis pipe connected on the same face used for injection, BuildCraft routed the incoming energy back into the network instead of keeping it at the receiver.
    - All orientations, including upward and downward connections, now receive energy correctly.

- **Logistics Pipes could not route through AP teleport pipes unless an LP pipe was placed directly next to the teleport pipe**
    - The routing connection previously only checked blocks immediately adjacent to the teleport pipe itself.
    - It now follows the teleport link to the remote end and discovers LP pipes connected there, so Logistics Pipes networks work correctly across any teleport pipe placement.

### Changed

- **Power Teleport Pipe item name no longer shows "[WIP]"**

* * *

## [1.2.0]

### Added

- **Power Teleport Pipe**
    - The Additional Pipes Power Teleport Pipe can now be crafted placed in the world, and actually teleports power between linked pipes.
    - The pipe was always part of Additional Pipes 6.0.0.8 in name only the item never existed and power never flowed even when obtained via commands.
    - LC-Core completes the missing registration, adds a crafting recipe, and fixes the energy flow.

- **Fluid teleport pipes in Logistics Pipes routing**
    - Fluid Teleport Pipes are now recognised by the Logistics Pipes routing network.
    - All three AP teleport pipe types are fully supported.

* * *

## [1.1.1]

### Fixed

- **Crash when placing a Logistics Pipe next to an AP teleport pipe**
    - Placing an LP routing pipe immediately beside any AP teleport pipe no longer crashes the game.

* * *

## [1.1.0]

### Fixed

- **Logistics Pipes could not route through Additional Pipes teleport pipes**
    - Items and power routed through a Logistics Pipes network now correctly cross teleport pipe boundaries.
    - Providers on the far side of a teleport pipe are visible to requesters, and items are properly returned if a destination is full or missing.
    - ([RS485/LogisticsPipes#348](https://github.com/RS485/LogisticsPipes/issues/348))

### Changed

- Minimum required mod versions updated: LogisticsPipes 0.10.4.49, Additional Pipes 6.0.0.8.

* * *

## [1.0.0]

Initial release.
