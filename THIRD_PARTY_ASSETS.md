# Third-party map assets

Skyveil's bundled map PNGs are development-time renders derived from Minecraft world downloads linked by KimiD2007 in [Hypixel SkyBlock World Downloads for every single island](https://hypixel.net/threads/hypixel-skyblock-world-downloads-for-every-single-island.5140617/).

The raw ZIP files and extracted worlds are not part of this repository or release JAR. `tools/map-generator/src/SkyveilMapGenerator.java` reads their Anvil chunks and emits only calibrated RGBA PNGs. Full source URLs, archive hashes, generation settings, exclusions, and limitations are recorded in `MAP_SOURCES.md`.

## Publication warning

The forum author says they made the downloads, but the post does not state an explicit license granting redistribution of the worlds or derived images. Attribution is included, but attribution alone is not a license. Obtain written permission from the world-download author and confirm Hypixel/Mojang asset rights before publicly distributing a build containing these derived PNGs.
