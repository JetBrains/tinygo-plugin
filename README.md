TinyGo plugin
===

[comment]: <> (![Build]&#40;https://github.com/pleomaxx3002/tinygo-plugin/workflows/Build/badge.svg&#41;)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [x] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
This Fancy IntelliJ Platform Plugin is going to be your implementation of the brilliant ideas that you have.

This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

To keep everything working, do not remove `<!-- ... -->` sections. 
<!-- Plugin description end -->

## Dependencies
This plugin requires gradle-intellij-plugin with GoLand-IDE support
1. Clone [gradle-intellij-plugin patch](https://github.com/pleomaxx3002/gradle-intellij-plugin)
1. Install the patch into local maven repository

Alternatively you can remove pluginManagement section in [settings file](./settings.gradle.kts)
and modify project to be consistent with [GoLand plugin development guide](https://plugins.jetbrains.com/docs/intellij/goland.html#configuring-plugin-projects-targeting-goland)

## Installation

Only development version is available at the moment.

[comment]: <> (- Using IDE built-in plugin system:)
  
[comment]: <> (  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "tinygo-plugin"</kbd> >)

[comment]: <> (  <kbd>Install Plugin</kbd>)
  
[comment]: <> (- Manually:)

[comment]: <> (  Download the [latest release]&#40;https://github.com/pleomaxx3002/tinygo-plugin/releases/latest&#41; and install it manually using)

[comment]: <> (  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>)


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
