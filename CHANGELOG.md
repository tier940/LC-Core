# Changelog

All notable changes to LC-Core are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
