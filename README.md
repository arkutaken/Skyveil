# Skyveil

Skyveil is a client-side Fabric mod for Minecraft 26.1.2.

## Installing

Install Fabric Loader and the external dependencies declared in `fabric.mod.json`, then place only:

`Skyveil-<version>.jar`

in the Minecraft `mods` folder. Do not install source, development, intermediary, or audit artifacts. Installing duplicate Skyveil jars can prevent Fabric from starting correctly.

## Building a release

For a bug-fix release, run:

```powershell
.\gradlew.bat releasePatch
```

Use `releaseMinor` for a new backward-compatible feature and `releaseMajor` for an intentional breaking release. These tasks update `mod_version` in `gradle.properties`, add an empty version heading to `CHANGELOG.md`, and build the release. `release` rebuilds the current version without incrementing it.

The standalone `bumpPatch`, `bumpMinor`, and `bumpMajor` tasks only update the version and changelog. A normal `build` never changes the version.

The single user-facing artifact is written to:

`build/release/Skyveil-<version>.jar`

The release task builds Loom's production jar, cleans the release directory, copies only that jar, and verifies that its filename and embedded Fabric metadata use the same semantic version.

## Development

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

Skyveil has one canonical implementation: shared code/resources are under `src/main`, and client code/resources are under `src/client`. Keep normal package subdivisions inside those trees. Do not create versioned or backup copies of source directories for releases or feature iterations; semantic versions belong in project metadata and the generated JAR name.

Gradle output belongs only in `build`. Temporary build caches or diagnostics needed to work around a local IDE lock must stay outside the repository and be removed afterward.

For a guided tour of startup, event flow, thread boundaries, caches, persistence, and the main feature packages, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## License

Skyveil's source is released under CC0-1.0.
