<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# tinygo-plugin Changelog

## [Unreleased]

## [0.5.20]

### Changed

- Add `cores` and `threads` scheduler options.
- Fix some UI freezes.

## [0.5.19]

### Changed

- Make plugin compatible with 2025.2 EAP.

## [0.5.18]

### Changed

- Make plugin compatible with 2025.1 EAP.

## [0.5.17]

### Fixed

- Fix the empty target platform list
- Fix the empty target platform value

## [0.5.16]

### Changed

- Make plugin compatible with 2024.3 EAP.

## [0.5.15]

### Changed

- Fix various UI freezes.
- Make plugin compatible with 2024.2.

## [0.5.14]

### Fixed

- Fix various UI freezes.

## [0.5.13]

### Changed

- Make plugin compatible with 2024.1.
- Change the icon for the AVR Assembly file type.

## [0.5.12]

### Changed

- Make plugin compatible with 2024.1 EAP.

## [0.5.11]

### Changed

- Make plugin compatible with 2023.3 EAP.

## [0.5.10]

### Changed

- Make plugin compatible with 2023.2.

## [0.5.9]

### Fixed

- Fix resolving of the `machine` package for some targets on Windows.

## [0.5.8]

### Changed

- Make plugin compatible with 2023.2 EAP.

## [0.5.7]

### Changed

- Make plugin compatible with 2023.1.

## [0.5.6]

### Changed

- Make plugin compatible with 2022.3.

## [0.5.5]

### Changed

- Improve matching of build tags and `cached GOROOT` parameters while extracting TinyGo parameters.

## [0.5.4]

### Changed

- Escape ANSI control sequences not on process output explicitly,
  but by using a prepared process executor for Windows

## [0.5.3]

### Changed

- Provide more logs during parameters extraction

## [0.5.2]

### Changed

- Make the plugin compatible with 2022.3 EAP.

### Fixed

- More correct inspection results for SDK-related inspections (e.g. do not show errors in `builtin.go`).
- Disable debug button for TinyGo run configurations.

## [0.5.1]

### Fixed

- Ignore ANSI escape sequences while extracting TinyGo parameters

## [0.5.0]

### Added

- TinyGo Preview integration for Go scratch files in projects with enabled TinyGo.
- AVR assembly language injection in `avr.Asm` and `avr.AsmFull` functions from `device/avr` package.
- TinyGo Build run configuration for only compiling a binary to a concrete location.

### Changed

- Link TinyGo sources appeared in heap allocations window to the `cached GOROOT` library
  for correct symbols resolving and better library code readability.

### Fixed

- Add lists of unsupported packages for TinyGo versions 0.23.0, 0.24.0 and 0.25.0.
- Make inspection for unsupported libraries don't mark library as unsupported
  when only extracted by build constraints files point to an unsupported library.
- Fix link to a description of an unsupported package at the TinyGo website.

## [0.4.0]

### Added

- Field for editing run configuration's environment variables.
- Multi-file example projects creation.
- Link to target configuration from run configuration.

### Changed

- Use the `asyncify` scheduler parameter instead of the obsolete `coroutines`.
- Veto unsupported by TinyGo GoLand run configurations like `go build`, `go test`, etc.

### Fixed

- Capture run configuration's working directory during execution.
- Import of custom target platforms from a .json file.

## [0.3.5]

### Changed

- Migration of UI components to Kotlin UI DSL 2.
- Download native TinyGo SDK when both native and emulated available.

### Fixed

- `cached GOROOT` resolving.
- Auto imports for TinyGo projects.

## [0.3.4]

### Changed

- Make the plugin compatible with 2022.2.
- Detect heap allocations with a context run configuration.

### Fixed

- Download TinyGo SDK from plugin on Apple Silicon architecture.
- Handle another kind of Go and TinyGo incompatibility error.
- Reload TinyGo library dynamically after target platform change.
- Detect heap allocations in IDE that is installed to a read-only directory.

## [0.3.3]

### Changed

- Make the plugin compatible with 2022.1.

## [0.3.2]

### Changed

- Make plugin compatible with 2021.3.

### Fixed

- Hide Go SDK from Libraries as it's now a part of TinyGo SDK.

## [0.3.1]

### Added

- Allow creating a new TinyGo project in IntelliJ IDEA.

### Changed

- Update small TinyGo icon.

## [0.3.0]

### Added

- View window for heap allocations
- Export custom targets as `.json` file
- Live templates for inline assembly on AVR, ARM and 64-bit ARM boards
- Error on attempt to rewrite an existing file
- Help boxes about `auto` flag for garbage collector and scheduler

### Changed

- Highlight all usages of unsupported packages, not only its imports
- Cosmetic changes on project wizard UI layout

## [0.2.0]

### Added

- - "TinyGo Test ..." run configuration from test file/function context
  - Inspection for usage of `machine` package in tests
- "TinyGo Emulate ..." run configuration (`tinygo run`) from `main` function context

### Changed

- Use cached GOROOT as TinyGo library instead of TinyGo sources

## [0.1.2]

### Fixed

- Fix NPE on applying recently downloaded TinyGo SDK to a project
- Fix NPE on selecting local TinyGo SDK
- Fix project creation and TinyGo SDK verification on Windows

### Added

- Error handling on `tinygo` command call
- Dynamic settings UI updates when editing compiler flags

## [0.1.1]

### Fixed

- Fix compatibility with 2021.2 EAP.

## [0.1.0]

### Added

- Ability to specify or download TinyGo SDK.
- Settings that help to infer build tags from a target platform.
- - Usage of the `go` with a disabled scheduler.
  - Usage of unsupported package.
  - Usage of interface comparison.
- Ability to flash and run a TinyGo application.
- Support for code completion and autoimport.

[Unreleased]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.18...HEAD
[0.5.18]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.17...v0.5.18
[0.5.17]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.16...v0.5.17
[0.5.16]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.15...v0.5.16
[0.5.15]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.14...v0.5.15
[0.5.14]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.13...v0.5.14
[0.5.13]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.12...v0.5.13
[0.5.12]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.11...v0.5.12
[0.5.11]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.10...v0.5.11
[0.5.10]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.9...v0.5.10
[0.5.9]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.8...v0.5.9
[0.5.8]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.7...v0.5.8
[0.5.7]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.6...v0.5.7
[0.5.6]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.5...v0.5.6
[0.5.5]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.4...v0.5.5
[0.5.4]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.3...v0.5.4
[0.5.3]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.2...v0.5.3
[0.5.2]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.1...v0.5.2
[0.5.1]: https://github.com/JetBrains/tinygo-plugin/compare/v0.5.0...v0.5.1
[0.5.0]: https://github.com/JetBrains/tinygo-plugin/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/JetBrains/tinygo-plugin/compare/v0.3.5...v0.4.0
[0.3.5]: https://github.com/JetBrains/tinygo-plugin/compare/v0.3.4...v0.3.5
[0.3.4]: https://github.com/JetBrains/tinygo-plugin/compare/v0.3.3...v0.3.4
[0.3.3]: https://github.com/JetBrains/tinygo-plugin/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/JetBrains/tinygo-plugin/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/JetBrains/tinygo-plugin/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/JetBrains/tinygo-plugin/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/JetBrains/tinygo-plugin/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/JetBrains/tinygo-plugin/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/JetBrains/tinygo-plugin/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/JetBrains/tinygo-plugin/commits/v0.1.0
