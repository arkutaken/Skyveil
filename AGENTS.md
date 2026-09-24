# Skyveil repository instructions

## Canonical project layout

- Production code lives only in `src/main` and `src/client`, using their existing Java/Kotlin package structure.
- Production resources live only in `src/main/resources` and `src/client/resources`.
- Developer utilities live in `tools`; documentation lives in `docs`.
- Gradle's canonical generated-output directory is `build`, and the only player-facing release is `build/release/Skyveil-<version>.jar`.

## Development policy

- Modify the existing canonical implementation when adding features or fixing bugs.
- Never clone a source tree or feature into versioned, `old`, `new`, `backup`, `fixed`, `rewrite`, dated, or similar repository folders.
- Do not create source-code backups in the repository. Use version control for history.
- Do not redirect Gradle output into alternate repository directories such as `build-codex-*`, `build-v2`, or `build-final`. Resolve a lock safely, use the canonical `build` directory, or place truly temporary diagnostics outside the repository and remove them afterward.
- Keep semantic versions in Gradle/Fabric metadata and JAR filenames; never encode release versions in source-directory names.
- Before refactoring, trace Gradle source sets, entrypoints, mixins, and resources. Merge required behavior into the canonical files before removing obsolete code.
- Preserve the existing `release` task and its single-JAR verification instead of adding another packaging path.

## Release version policy

- For every completed batch of mod changes delivered to the user, bump the semantic version before building the release. Never deliver changed mod behavior under a previously delivered version.
- Use the existing Gradle bump tasks (normally bumpPatch; bumpMinor or bumpMajor when appropriate), then build with release. Do not bump again for retries of the same release.
- Update CHANGELOG.md and the bundled release_notes.txt for that version; keep Gradle version, Fabric metadata, release notes, and the release JAR filename consistent.
