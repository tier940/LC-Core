# Changelog

All notable changes to LC-Core are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [1.1.0]

### Fixed

- **LogisticsPipes routing through Additional Pipes teleport pipes** — Items and power can
  now be routed through AP teleport pipes inside a Logistics Pipes network. LP 0.10.4.28
  silently removed its built-in connection handler for AP teleport pipes, which caused LP to
  be completely unaware of routes that cross a teleportation boundary — providers on the far
  side of a teleport pipe became invisible to requesters, and items sent toward a full or
  missing destination were lost instead of being returned.
  LC-Core now registers the missing bridge itself, restoring full provider/requester
  visibility and proper re-routing when a destination cannot be reached.
  ([RS485/LogisticsPipes#348](https://github.com/RS485/LogisticsPipes/issues/348))

  > **Note:** Fluid teleport pipes are intentionally not supported. LP transports fluids
  > as FluidContainer items, which the fluid teleport pipe is not designed to carry.

### Changed

- Minimum required mod versions updated: LogisticsPipes 0.10.4.49, Additional Pipes 6.0.0.8.

* * *

## [1.0.0-beta]

Initial release.
