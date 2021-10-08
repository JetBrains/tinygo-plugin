<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# tinygo-plugin Changelog

## [0.3.1]
### Added
- Allow creating a new TinyGo project in IntelliJ IDEA.

### Changed
- Update small TinyGo icon.

### Deprecated

### Removed

### Fixed

### Security

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
- Partial support of TinyGo unit testing (tests that use `machine` package are yet not supported by TinyGo)
  - "TinyGo Test ..." run configuration from test file/function context
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
- Code inspections:
  - Usage of the `go` with a disabled scheduler.
  - Usage of unsupported package.
  - Usage of interface comparison.
- Ability to flash and run a TinyGo application.
- Support for code completion and autoimport.
