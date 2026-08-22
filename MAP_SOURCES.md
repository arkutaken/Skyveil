# Skyveil map sources

Generation date: 2026-08-11  
Forum source: https://hypixel.net/threads/hypixel-skyblock-world-downloads-for-every-single-island.5140617/  
Forum author/download creator: KimiD2007  
World DataVersion: 2584  
Generator: `tools/map-generator/src/SkyveilMapGenerator.java`

The source post is from 2022 and users requested newer Hub and additional islands in 2026. These generated maps reproduce the available downloads; they must not be represented as guaranteed current Hypixel builds without live verification.

## Fixed worlds converted

| Map | Download | ZIP SHA-256 | Generated size | Special generation settings |
|---|---|---|---:|---|
| Hub | https://www.mediafire.com/file/02wd269n5mdz1zw/The_Hub.zip/file | `D3B964F97D8A366837B4F5630D99ADE49925744E29829DB4F47EF8E533E666A5` | 493×451 | none |
| Gold Mine | https://www.mediafire.com/file/szo8chwmbgaubt5/The_Gold_Mine.zip/file | `6E96148D7FDACCCE2AF423B144C10703A78AED0C15BB3D15A7E671F25B2F9E07` | 140×139 | none |
| Deep Caverns | https://www.mediafire.com/file/cj5re7ft12h9oem/The_Deep_Caverns.zip/file | `5914E9C0D892D19A0BEF654B8B48C1D188335E10CCA899F8AA1F7A40783C9375` | 169×165 | none |
| Dwarven Mines | https://www.mediafire.com/file/srryyljgbo8r6mz/The_Dwarven_Mines.zip/file | `EB24B008CA9AF931CD8861C990FD9F6A302239068553A696E32852CC524CCFAB` | 522×469 | `--ignore-blocks stone` removes the enclosing stone shell |
| The Park | https://www.mediafire.com/file/ewi4wfddvvcm9yf/The_Park.zip/file | `3A0865A493DC974BFDB74DF39DDB368BCFC07FAC824A1C83C420B08B985F52DE` | 217×236 | none |
| Spider's Den | https://www.mediafire.com/file/ezq0qfbljcmtxge/The_Spider%27s_Den.zip/file | `CD23F5EF0D4A78E481DE1F8F158EB5EB7DA784F29D36FEBA7BD8A66D1C42CA7F` | 276×244 | none |
| The End | https://www.mediafire.com/file/z21npcxcqlzld2k/The_End.zip/file | `8AC0A030ED1A922B121B3FAAAF09109A587C23720D2913CB4D8A79B464A615CB` | 344×289 | none |
| Crimson Isle | https://www.mediafire.com/file/b0k3ax1dnytheog/The_Crimson_Isle.zip/file | `443CAAD149FA83389A0DED589957D3FBC1C3FF5EFA8DF423872B22280FE9D8D4` | 843×712 | `DIM-1`, `--ignore-blocks bedrock`, reviewed shoreline mask below |
| Farming Islands | https://www.mediafire.com/file/8xvbs7s2ji5q3th/The_Farming_Islands.zip/file | `C194437D9B420E9ABA1FC025A932057F7F25F172A2AB8BB17200CF007DBF0396` | 325×429 | none |
| Dungeon Hub | https://www.mediafire.com/file/wg1zuuqi392evvb/The_Dungeon_Hub.zip/file | `EBFC13404F9ADF8264E1ECA78E4F284081ACA1DFBEF991610681A078FE36D7ED` | 172×244 | `--ignore-blocks stone,gray_wool,gray_terracotta` removes the enclosing roof |

Crimson Isle world-coordinate mask:

`-765:-1065;-520:-1100;-340:-1090;-160:-1080;20:-1050;55:-970;50:-770;-70:-680;-180:-610;-260:-395;-470:-450;-650:-570;-755:-720;-780:-900`

All generated images use one pixel per block. Minimum X/Z is inclusive; maximum X/Z is exclusive. Image top is north/minimum Z and image right is east/maximum X. Void/outside-mask pixels are transparent.

## Explicitly excluded random worlds

- Crystal Hollows sample: described by the post as randomly generated and partially missing.
- Catacombs Entrance and Floors I–VI: described by the post as randomly generated.

These downloads are not universal live-instance maps and are not registered in Skyveil.

## Unavailable in the source post

- Spooky Hub
- Winter Hub
- Jerry's Workshop
- Catacombs Floor VII

Galatea, Bayou, the Rift, and other newer locations have no fixed download in this post and remain unsupported.

## Rebuilding

Keep extracted worlds outside runtime resources, preferably under ignored `dev/map_sources/`. Compile and invoke the generator as documented in `tools/map-generator/README.md`, visually review the PNG and its dominant surface report, then copy only the approved PNG plus runtime JSON into `src/client/resources/assets/skyveil/`.
