# Nekoro World Setup

World rules for modpacks and servers, all set in config files: start commands and game rules, global datapacks, higher enchantment levels, mob spawn filters, recipe changes, water and lava behaviour, a wrapping world edge, void teleport, bone meal on more plants and world generation changes.

Made for my Nekoro Pokemon modpack, but it works in any world. It replaces several mods I used there (World Start Commands and Global Gamerules, Paxi, Universal Enchants, Pretty Beaches, Cobble Gen Haters, Bad Mobs, Universal Bone Meal, Flat Bedrock, No Caves Mod and the part of CraftTweaker I used). Many of those mods may be better than mine.

Download: [CurseForge](https://www.curseforge.com/minecraft/search?search=nekoro+world+setup)

## Versions

| Minecraft | Loaders |
|---|---|
| 1.21.1 | NeoForge, Fabric |

Fabric needs Fabric API.

## Config

Five files in `config/Nekoro/WorldSetup/` are created on first start: `start.json`, `world.json`, `mobs.json`, `worldgen.json` and `recipes.json`. Every option is explained in the file. Apart from a fix for the "Feature order cycle found" world generation crash, everything is off until you turn it on.

The default files are in [config-default](config-default/Nekoro/WorldSetup), and the setup of my modpack is in [pack-config](pack-config/Nekoro/WorldSetup).

## Building

The shared code is in `source/common` (`java-core`, plus `java-neoforge` and `java-fabric` for what differs between the loaders), and each loader has a small Gradle project next to it.

```
cd source
.uild-all.ps1
```

The jars are copied to `Gotowe/<version>`. Java 21 is needed.

## License

MIT, see [LICENSE](LICENSE).
