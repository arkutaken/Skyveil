# Skyveil map generator

This development-only Java 25 utility converts an extracted Java Edition Anvil world into a calibrated RGBA top-down PNG and metadata JSON. It has no third-party dependencies.

```powershell
javac -d dev/map-generator-classes tools/map-generator/src/SkyveilMapGenerator.java
java -cp dev/map-generator-classes SkyveilMapGenerator `
  --world dev/map_sources/The_Hub `
  --id hub `
  --name "Hub" `
  --output dev/generated_maps
```

The generator reads the selected dimension's `region/*.mca` files, resolves modern paletted block states, selects the highest visible block per X/Z column, composites glass over the block below, colors water distinctly, applies subtle local height shading, and leaves columns without blocks transparent.

By default the tool selects the overworld, Nether, or End dimension with the largest Anvil-region payload. Use `--dimension overworld`, `--dimension nether`, or `--dimension end` to override this when reviewing an unusual download.

For an underground world enclosed by a solid development shell, `--max-y N` selects the highest visible block at or below the verified playable ceiling. Record and visually review every such override; it must not be guessed silently.

If an enclosing shell spans the playable area's full height, `--ignore-blocks stone,bedrock` can treat only those explicitly reviewed shell materials as transparent while selecting the next visible block below. This is intended for downloaded development shells such as Dwarven Mines' stone enclosure and Crimson Isle's bedrock enclosure; record the exact per-map list in `MAP_SOURCES.md`.

For a pasted island surrounded by unrelated generated terrain, `--component-seed X,Z` retains only the diagonally connected rendered surface containing the reviewed island coordinate. It must be anchored to a known island coordinate and documented; it is not automatic terrain guessing.

If source terrain is physically joined to the island, `--crop minX,maxX,minZ,maxZ` applies a reviewed world-coordinate crop with exclusive maxima. The resulting PNG remains exactly calibrated; the chosen boundary must be recorded with the source entry.

`--mask-polygon X:Z;X:Z;...` can remove unrelated terrain inside that crop using a reviewed world-coordinate shoreline polygon. Use this only when the download embeds an island into ordinary generated terrain, and preserve the complete polygon in source documentation.

The reported maximum X/Z bounds are exclusive. With the default one pixel per block, `imageWidth == maxX - minX` and `imageHeight == maxZ - minZ`. Image top is minimum Z (north), and image right is maximum X (east).

Raw downloaded worlds belong under `dev/map_sources/`, which is ignored by Git and must never be packaged in the mod. Review every generated image, source permission, and coordinate calibration before copying the PNG and a completed runtime definition into `src/client/resources/assets/skyveil/`.
