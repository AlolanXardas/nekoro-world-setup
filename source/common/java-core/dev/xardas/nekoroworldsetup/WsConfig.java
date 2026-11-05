package dev.xardas.nekoroworldsetup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;

/**
 * All settings, read from config/Nekoro/WorldSetup/*.json (config/Nekoro holds the configs of all Nekoro mods). Each load builds a new immutable snapshot,
 * so code on other threads (world generation) always sees one consistent version.
 */
public final class WsConfig {
    private static volatile WsConfig current;

    public final Start start;
    public final World world;
    public final Mobs mobs;
    public final Worldgen worldgen;
    public final Recipes recipes;

    private WsConfig(Start start, World world, Mobs mobs, Worldgen worldgen, Recipes recipes) {
        this.start = start;
        this.world = world;
        this.mobs = mobs;
        this.worldgen = worldgen;
        this.recipes = recipes;
    }

    public static WsConfig get() {
        if (current == null) load();
        return current;
    }

    public static Path dir() {
        return Platform.INSTANCE.configDir().resolve("Nekoro").resolve("WorldSetup");
    }

    public static synchronized void load() {
        Path dir = dir();
        current = new WsConfig(
                new Start(JsonFile.read(dir.resolve("start.json"), DEFAULT_START)),
                new World(JsonFile.read(dir.resolve("world.json"), DEFAULT_WORLD)),
                new Mobs(JsonFile.read(dir.resolve("mobs.json"), DEFAULT_MOBS)),
                new Worldgen(JsonFile.read(dir.resolve("worldgen.json"), DEFAULT_WORLDGEN)),
                new Recipes(JsonFile.read(dir.resolve("recipes.json"), DEFAULT_RECIPES)));
    }

    // ---- start.json

    public enum When { WORLD_CREATED, SERVER_START, PLAYER_FIRST_JOIN, PLAYER_JOIN }

    public record StartCommand(When when, String command) { }

    public static final class Start {
        public final boolean gamerules;
        public final boolean gamerulesEveryStart;
        public final Map<String, String> rules = new LinkedHashMap<>();
        public final boolean commands;
        public final List<StartCommand> commandList = new ArrayList<>();
        public final boolean globalDatapacks;

        Start(JsonObject o) {
            JsonObject g = JsonFile.obj(o, "gamerules");
            gamerules = JsonFile.bool(g, "enabled", false);
            gamerulesEveryStart = JsonFile.bool(g, "every_start", false);
            for (Map.Entry<String, JsonElement> e : JsonFile.obj(g, "rules").entrySet()) {
                rules.put(e.getKey(), e.getValue().getAsString());
            }
            JsonObject c = JsonFile.obj(o, "commands");
            commands = JsonFile.bool(c, "enabled", false);
            if (c.has("list")) {
                for (JsonElement e : c.getAsJsonArray("list")) {
                    try {
                        JsonObject cmd = e.getAsJsonObject();
                        When when = When.valueOf(JsonFile.string(cmd, "when", "world_created").toUpperCase(Locale.ROOT));
                        commandList.add(new StartCommand(when, JsonFile.string(cmd, "command", "")));
                    } catch (RuntimeException ex) {
                        NekoroWorldSetup.LOGGER.warn("Skipping invalid command in start.json: {}", ex.getMessage());
                    }
                }
            }
            globalDatapacks = JsonFile.bool(o, "global_datapacks", false);
        }
    }

    // ---- world.json

    public static final class World {
        public final boolean enchantments;
        public final int maxLevel;
        public final boolean skipSingleLevel;
        public final Map<ResourceLocation, Integer> levelOverrides = new HashMap<>();
        public final int anvilCostLimit;
        /** enchantment tags whose members may be combined; allCompatible = every enchantment */
        public final List<ResourceLocation> compatibleTags = new ArrayList<>();
        public final boolean allCompatible;
        public final Map<ResourceLocation, IdList> extraItems = new HashMap<>();
        public final boolean featherFallingFarmland;
        public final boolean infinityWithoutArrows;
        public final boolean lootingFortuneXp;
        public final int xpBonusMax;
        public final boolean noProjectileImmunity;
        public final boolean enchantableShears;
        public final boolean enchantableShields;
        public final double mendingBonusPerLevel;

        public final boolean waterFill;
        public final IdList waterFillBlocks;
        public final int sourcesNeeded;
        public final boolean floodHoles;

        public final boolean fluidBlocks;
        public final boolean noCobblestone;
        public final boolean noStone;
        public final boolean noBasalt;
        public final boolean noObsidian;

        public final boolean borderWrap;
        public final List<String> borderDimensions;
        public final double centerX;
        public final double centerZ;
        public final double radius;
        public final double moveInside;

        public final boolean voidTeleport;
        public final List<String> voidDimensions;
        public final int voidDepth;
        public final boolean noFallDamage;

        public final boolean boneMeal;
        public final boolean boneMealSugarCane;
        public final boolean boneMealCactus;
        public final boolean boneMealVines;
        public final boolean boneMealNetherWart;
        public final boolean boneMealFruitStems;
        public final boolean boneMealLilyPad;
        public final boolean boneMealSmallFlowers;
        public final boolean boneMealDeadBush;
        public final boolean boneMealChorus;
        public final boolean boneMealCorals;
        public final boolean boneMealSporeBlossom;
        public final boolean boneMealPodzol;
        public final boolean boneMealMycelium;
        public final boolean boneMealDirt;

        public final boolean realTime;
        public final double realTimeOffsetHours;
        public final int realTimeCheckSeconds;

        World(JsonObject o) {
            JsonObject e = JsonFile.obj(o, "enchantments");
            enchantments = JsonFile.bool(e, "enabled", false);
            maxLevel = Math.max(1, Math.min(255, JsonFile.integer(e, "max_level", 10)));
            skipSingleLevel = JsonFile.bool(e, "skip_single_level", false);
            for (Map.Entry<String, JsonElement> entry : JsonFile.obj(e, "overrides").entrySet()) {
                try {
                    levelOverrides.put(ResourceLocation.parse(entry.getKey()), Math.max(1, Math.min(255, entry.getValue().getAsInt())));
                } catch (RuntimeException ex) {
                    NekoroWorldSetup.LOGGER.warn("Skipping invalid enchantment override {}: {}", entry.getKey(), ex.getMessage());
                }
            }
            anvilCostLimit = JsonFile.integer(e, "anvil_cost_limit", 40);
            boolean all = false;
            for (String group : JsonFile.strings(e, "compatible")) {
                String g = group.trim();
                try {
                    if (g.equalsIgnoreCase("all")) all = true;
                    else if (g.startsWith("#")) compatibleTags.add(ResourceLocation.parse(g.substring(1)));
                    else compatibleTags.add(ResourceLocation.withDefaultNamespace("exclusive_set/" + g.toLowerCase(Locale.ROOT)));
                } catch (RuntimeException ex) {
                    NekoroWorldSetup.LOGGER.warn("Skipping invalid compatible group \"{}\": {}", group, ex.getMessage());
                }
            }
            allCompatible = all;
            for (Map.Entry<String, JsonElement> entry : JsonFile.obj(e, "extra_items").entrySet()) {
                try {
                    List<String> items = new ArrayList<>();
                    if (entry.getValue().isJsonArray()) entry.getValue().getAsJsonArray().forEach(i -> items.add(i.getAsString()));
                    else items.add(entry.getValue().getAsString());
                    extraItems.put(ResourceLocation.parse(entry.getKey()), IdList.of(items));
                } catch (RuntimeException ex) {
                    NekoroWorldSetup.LOGGER.warn("Skipping invalid extra_items entry {}: {}", entry.getKey(), ex.getMessage());
                }
            }
            JsonObject x = JsonFile.obj(e, "extras");
            featherFallingFarmland = JsonFile.bool(x, "feather_falling_protects_farmland", false);
            infinityWithoutArrows = JsonFile.bool(x, "infinity_without_arrows", false);
            lootingFortuneXp = JsonFile.bool(x, "looting_and_fortune_boost_xp", false);
            xpBonusMax = Math.max(0, JsonFile.integer(x, "xp_bonus_max", 50));
            noProjectileImmunity = JsonFile.bool(x, "no_projectile_immunity", false);
            enchantableShears = JsonFile.bool(x, "enchantable_shears", false);
            enchantableShields = JsonFile.bool(x, "enchantable_shields", false);
            mendingBonusPerLevel = Math.max(0, JsonFile.number(x, "mending_bonus_per_level", 0));

            JsonObject w = JsonFile.obj(o, "water_fill");
            waterFill = JsonFile.bool(w, "enabled", false);
            waterFillBlocks = IdList.of(JsonFile.strings(w, "blocks"));
            sourcesNeeded = Math.max(1, Math.min(5, JsonFile.integer(w, "sources_needed", 1)));
            floodHoles = JsonFile.bool(w, "flood_holes", true);

            JsonObject f = JsonFile.obj(o, "fluid_blocks");
            fluidBlocks = JsonFile.bool(f, "enabled", false);
            noCobblestone = JsonFile.bool(f, "no_cobblestone", true);
            noStone = JsonFile.bool(f, "no_stone", true);
            noBasalt = JsonFile.bool(f, "no_basalt", false);
            noObsidian = JsonFile.bool(f, "no_obsidian", false);

            JsonObject b = JsonFile.obj(o, "border_wrap");
            borderWrap = JsonFile.bool(b, "enabled", false);
            borderDimensions = JsonFile.strings(b, "dimensions");
            centerX = JsonFile.number(b, "center_x", 0);
            centerZ = JsonFile.number(b, "center_z", 0);
            radius = Math.max(16, JsonFile.number(b, "radius", 4000));
            moveInside = Math.max(1, Math.min(radius, JsonFile.number(b, "move_inside", 100)));

            JsonObject v = JsonFile.obj(o, "void_teleport");
            voidTeleport = JsonFile.bool(v, "enabled", false);
            voidDimensions = JsonFile.strings(v, "dimensions");
            voidDepth = Math.max(1, JsonFile.integer(v, "depth", 10));
            noFallDamage = JsonFile.bool(v, "no_fall_damage", true);

            JsonObject m = JsonFile.obj(o, "bone_meal");
            boneMeal = JsonFile.bool(m, "enabled", false);
            boneMealSugarCane = JsonFile.bool(m, "sugar_cane", true);
            boneMealCactus = JsonFile.bool(m, "cactus", true);
            boneMealVines = JsonFile.bool(m, "vines", true);
            boneMealNetherWart = JsonFile.bool(m, "nether_wart", true);
            boneMealFruitStems = JsonFile.bool(m, "fruit_stems", true);
            boneMealLilyPad = JsonFile.bool(m, "lily_pad", true);
            boneMealSmallFlowers = JsonFile.bool(m, "small_flowers", true);
            boneMealDeadBush = JsonFile.bool(m, "dead_bush", true);
            boneMealChorus = JsonFile.bool(m, "chorus", true);
            boneMealCorals = JsonFile.bool(m, "corals", true);
            boneMealSporeBlossom = JsonFile.bool(m, "spore_blossom", true);
            boneMealPodzol = JsonFile.bool(m, "podzol", true);
            boneMealMycelium = JsonFile.bool(m, "mycelium", true);
            boneMealDirt = JsonFile.bool(m, "dirt", true);

            JsonObject t = JsonFile.obj(o, "real_time");
            realTime = JsonFile.bool(t, "enabled", false);
            realTimeOffsetHours = JsonFile.number(t, "offset_hours", 0);
            realTimeCheckSeconds = Math.max(1, Math.min(600, JsonFile.integer(t, "check_seconds", 10)));
        }
    }

    // ---- mobs.json

    public static final class Mobs {
        public final boolean enabled;
        public final boolean allowMode;
        public final IdList mobs;
        public final Set<MobSpawnType> spawnTypes = EnumSet.noneOf(MobSpawnType.class);
        public final boolean removeExisting;
        public final boolean blockOtherSpawns;

        Mobs(JsonObject o) {
            enabled = JsonFile.bool(o, "enabled", false);
            allowMode = JsonFile.string(o, "mode", "block").equalsIgnoreCase("allow");
            mobs = IdList.of(JsonFile.strings(o, "mobs"));
            for (String type : JsonFile.strings(o, "spawn_types")) {
                try {
                    spawnTypes.add(MobSpawnType.valueOf(type.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    NekoroWorldSetup.LOGGER.warn("Unknown spawn type \"{}\" in mobs.json", type);
                }
            }
            removeExisting = JsonFile.bool(o, "remove_existing", false);
            blockOtherSpawns = JsonFile.bool(o, "block_other_spawns", false);
        }
    }

    // ---- worldgen.json

    public static final class Worldgen {
        public final boolean flatBedrockFloor;
        public final boolean flatBedrockRoof;
        public final boolean removeOres;
        public final IdList keepOres;
        public final boolean removeLavaLakes;
        public final boolean removeLavaSprings;
        public final boolean removeDeepLava;
        public final boolean removeCarvers;
        public final boolean removeSpaghettiCaves;
        public final boolean removeGeodes;
        public final boolean removeCheeseCaves;
        public final boolean fixFeatureOrder;

        Worldgen(JsonObject o) {
            JsonObject bedrock = JsonFile.obj(o, "flat_bedrock");
            flatBedrockFloor = JsonFile.bool(bedrock, "floor", false);
            flatBedrockRoof = JsonFile.bool(bedrock, "roof", false);
            removeOres = JsonFile.bool(o, "remove_ores", false);
            keepOres = IdList.of(JsonFile.strings(o, "keep_ores"));
            removeLavaLakes = JsonFile.bool(o, "remove_lava_lakes", false);
            removeLavaSprings = JsonFile.bool(o, "remove_lava_springs", false);
            removeDeepLava = JsonFile.bool(o, "remove_deep_lava", false);
            removeCarvers = JsonFile.bool(o, "remove_carvers", false);
            removeSpaghettiCaves = JsonFile.bool(o, "remove_spaghetti_caves", false);
            removeGeodes = JsonFile.bool(o, "remove_geodes", false);
            removeCheeseCaves = JsonFile.bool(o, "remove_big_caves", false);
            fixFeatureOrder = JsonFile.bool(o, "fix_feature_order", true);
        }
    }

    // ---- recipes.json

    public static final class Recipes {
        public final boolean remove;
        public final IdList removeOutputs;
        public final IdList removeIds;
        public final boolean slabsToBlocks;
        public final boolean stairsToBlocks;
        public final boolean logsToSticks;
        public final boolean logsToChests;

        Recipes(JsonObject o) {
            JsonObject r = JsonFile.obj(o, "remove");
            remove = JsonFile.bool(r, "enabled", false);
            removeOutputs = IdList.of(JsonFile.strings(r, "outputs"));
            removeIds = IdList.of(JsonFile.strings(r, "ids"));
            JsonObject a = JsonFile.obj(o, "add");
            slabsToBlocks = JsonFile.bool(a, "slabs_to_blocks", false);
            stairsToBlocks = JsonFile.bool(a, "stairs_to_blocks", false);
            logsToSticks = JsonFile.bool(a, "logs_to_sticks", false);
            logsToChests = JsonFile.bool(a, "logs_to_chests", false);
        }
    }

    // ---- default files

    private static final String DEFAULT_START = """
            // Nekoro World Setup: what happens when a world starts and when players join.
            // Everything is off until you turn it on. Changes need a server restart.
            {
              // Game rules, set the first time a world starts with this mod. Any game rule works, modded ones too.
              // "every_start": true sets them again on every server start (also if someone changed them in game).
              "gamerules": {
                "enabled": false,
                "every_start": false,
                "rules": {
                  "doInsomnia": false,
                  "doPatrolSpawning": false,
                  "doTraderSpawning": false
                }
              },

              // Commands run by the server with operator permissions. Write them without the leading /.
              // "when": "world_created"      the first time a world starts with this mod
              //         "server_start"       every time the server starts
              //         "player_first_join"  the first time a player joins this world (@s is the player)
              //         "player_join"        every time a player joins (@s is the player)
              "commands": {
                "enabled": false,
                "list": [
                  { "when": "world_created", "command": "worldborder set 8001" },
                  { "when": "player_first_join", "command": "give @s minecraft:bread 8" }
                ]
              },

              // Datapacks (zip files or folders) placed in config/Nekoro/WorldSetup/datapacks are loaded in every world,
              // new and existing ones. The folder is created when this is turned on.
              "global_datapacks": false
            }
            """;

    private static final String DEFAULT_WORLD = """
            // Nekoro World Setup: rules that change how the world behaves.
            // Everything is off until you turn it on. /reload applies changes to this file.
            {
              // Higher maximum enchantment levels: enchanting table, anvil, books and /enchant.
              "enchantments": {
                "enabled": false,
                // new maximum for every enchantment; enchantments that already go higher keep their own maximum
                "max_level": 10,
                // true keeps enchantments that only have level I (Mending, Silk Touch, Infinity...) at I
                "skip_single_level": false,
                // a different maximum for single enchantments, e.g. { "minecraft:sharpness": 15, "minecraft:mending": 1 }
                "overrides": {},
                // the anvil refuses anything that costs this many levels or more ("Too Expensive!").
                // Vanilla is 40. Higher enchantment levels make combining books expensive; 0 means no limit.
                "anvil_cost_limit": 40,
                // enchantments that normally exclude each other but may now be combined (anvil and enchanting table).
                // Groups: "damage" (Sharpness/Smite/Bane), "armor" (the Protections), "bow" (Infinity/Mending),
                // "crossbow" (Multishot/Piercing), "mining" (Fortune/Silk Touch), "boots" (Frost Walker/Depth Strider),
                // "riptide" (Riptide/Loyalty/Channeling), any enchantment tag ("#mymod:my_set"), or "all"
                "compatible": [],
                // items an enchantment may go on besides its normal ones, e.g. { "minecraft:looting": ["#minecraft:axes"] }
                "extra_items": {},
                // small enchantment rules (these work even with "enabled" above off)
                "extras": {
                  // any Feather Falling stops you from trampling farmland
                  "feather_falling_protects_farmland": false,
                  // a bow with Infinity shoots without any arrow in the inventory
                  "infinity_without_arrows": false,
                  // Looting raises the XP mobs drop and Fortune the XP from ores: level * (level + 1) / 10 times more
                  // (Looting III: +120%, X: +1100%), but at most xp_bonus_max more points (like Universal Enchants)
                  "looting_and_fortune_boost_xp": false,
                  "xp_bonus_max": 50,
                  // no short immunity after a projectile hit, so all arrows of Multishot hit
                  "no_projectile_immunity": false,
                  // shears and shields can be enchanted in the enchanting table, not only with an anvil
                  "enchantable_shears": false,
                  "enchantable_shields": false,
                  // every Mending level above I repairs this much more per XP point, so it also uses less XP:
                  // 0.5 makes II repair 1.5x and X 5.5x as much, 1.0 makes X repair 10x. 0 = vanilla
                  "mending_bonus_per_level": 0
                }
              },

              // Digging a block next to water turns its place into water right away,
              // so beaches and lakes do not end up with water flowing everywhere.
              "water_fill": {
                "enabled": false,
                // only these blocks: ids, whole mods ("minecraft:*") or tags ("#minecraft:sand").
                // Built blocks like planks or bricks are left out on purpose, so houses next to water cannot get flooded.
                "blocks": ["#minecraft:sand", "#c:sands", "#minecraft:dirt", "minecraft:gravel", "#c:gravels",
                           "minecraft:clay", "#minecraft:base_stone_overworld", "#c:stones"],
                // how many water source blocks (on the sides or above) must touch the dug block, 1 to 5
                "sources_needed": 1,
                // also fill small empty holes right next to it, at the same height or lower
                "flood_holes": true
              },

              // Blocks that lava and water make when they meet. true = that block is not made, the fluids stay as they are.
              "fluid_blocks": {
                "enabled": false,
                "no_cobblestone": true,
                "no_stone": true,
                "no_basalt": false,
                // obsidian is needed for Nether portals
                "no_obsidian": false
              },

              // Walking out of a square area takes players to the opposite side, like on a round planet.
              // This does not change the vanilla world border: make that one bigger than this area, or leave it off.
              "border_wrap": {
                "enabled": false,
                "dimensions": ["minecraft:overworld"],
                "center_x": 0,
                "center_z": 0,
                // half the width of the area: 4000 means from 4000 blocks before the center to 4000 after it
                "radius": 4000,
                // how far inside the opposite edge players land, so they do not bounce back and forth
                // (far enough that walking over the edge again and again cannot be used to load lots of chunks)
                "move_inside": 100
              },

              // Players who fall into the void appear at the top of the same dimension and fall back down.
              "void_teleport": {
                "enabled": false,
                // empty = every dimension
                "dimensions": [],
                // how many blocks below the bottom of the world it happens
                "depth": 10,
                "no_fall_damage": true
              },
              // Bone meal also works on plants and blocks vanilla ignores (like Universal Bone Meal). Each one can be turned off.
              "bone_meal": {
                "enabled": false,
                // one block more, up to the height they reach on their own (3)
                "sugar_cane": true,
                "cactus": true,
                // vines grow one block further down
                "vines": true,
                "nether_wart": true,
                // a fully grown melon or pumpkin stem makes its fruit right away
                "fruit_stems": true,
                // lily pads spread over still water around them
                "lily_pad": true,
                // small flowers and dead bushes spread around them
                "small_flowers": true,
                "dead_bush": true,
                // the nearest chorus flower of the plant grows (up to 48 blocks of stem above)
                "chorus": true,
                // coral in a warm ocean grows into a coral tree (needs some free water above)
                "corals": true,
                // drops one more spore blossom
                "spore_blossom": true,
                // grows ferns, sometimes large ferns and sweet berry bushes on podzol around it
                "podzol": true,
                // grows brown and red mushrooms on mycelium around it
                "mycelium": true,
                // dirt (also coarse and rooted) next to grass or mycelium turns into it
                "dirt": true
              },
              // The time of day follows the clock of the computer the server runs on (in single player: yours), and
              // every real day is one Minecraft day. The daylight cycle game rule stays off, so sleeping does not skip the night.
              "real_time": {
                "enabled": false,
                // hours added to the clock (e.g. -2 or 1.5), for a server in a different time zone than its players
                "offset_hours": 0,
                // how often the time is set, in seconds (the sun moves about 3 ticks of game time in 10 seconds)
                "check_seconds": 10
              }
            }
            """;

    private static final String DEFAULT_MOBS = """
            // Nekoro World Setup: which mobs may spawn. Only living mobs are checked;
            // items, boats, armor stands and similar entities are never touched. /reload applies changes.
            {
              "enabled": false,
              // "block": the mobs listed below cannot spawn
              // "allow": only the mobs listed below can spawn, every other mob is blocked
              "mode": "block",
              // ids ("minecraft:zombie"), whole mods ("minecraft:*") or entity tags ("#minecraft:raiders")
              "mobs": ["minecraft:phantom"],
              // the kinds of spawning that are checked; kinds not listed are always allowed.
              // All kinds: natural, chunk_generation, structure, spawner, trial_spawner, patrol, event, reinforcement,
              // jockey, mob_summoned, conversion, breeding, triggered, bucket, spawn_egg, command, dispenser
              "spawn_types": ["natural", "chunk_generation", "structure", "spawner", "trial_spawner", "patrol",
                              "event", "reinforcement", "jockey", "mob_summoned"],
              // also remove blocked mobs that already exist when their chunk loads (never named, tamed or leashed ones)
              "remove_existing": false,
              // also block mobs that appear without a normal spawn: bees leaving their hive, babies from breeding,
              // mob conversions, built golems, /summon with extra data. Spawn eggs and plain /summon stay allowed.
              "block_other_spawns": false
            }
            """;

    private static final String DEFAULT_WORLDGEN = """
            // Nekoro World Setup: world generation. Changes only affect chunks generated afterwards and need a restart.
            // Everything is off until you turn it on.
            {
              // only the lowest (floor) or highest (roof, Nether) layer of bedrock, instead of the bumpy 5 layers
              "flat_bedrock": {
                "floor": false,
                "roof": false
              },
              // ore veins from vanilla and mods (blocks in #c:ores or with "ore" in their id), and the big
              // copper and iron veins from 1.18. Granite, diorite, andesite, tuff, dirt and gravel blobs stay.
              "remove_ores": false,
              // ores to keep anyway: placed feature ids ("minecraft:ore_coal_upper"), block ids or block tags
              "keep_ores": [],
              "remove_lava_lakes": false,
              // lava springs: lava falling in caves and the hidden lava pockets in the Nether
              "remove_lava_springs": false,
              // the lava layer in caves below Y -54 (becomes air) and lava in deep aquifers (becomes water)
              "remove_deep_lava": false,
              // classic cave tunnels and canyons. The big caves from 1.18 are part of the terrain and stay.
              "remove_carvers": false,
              // the long winding spaghetti caves, thin noodle caves and cave entrances from the surface.
              // The big open caves stay. Fewer tunnels also means faster generation and fewer hidden mob spawns.
              "remove_spaghetti_caves": false,
              // the big open caves too (vanilla cheese caves, Terralith's mega caves). With this and the two above
              // almost nothing is hollow underground: the fastest generation and no hidden mob spawns.
              "remove_big_caves": false,
              // amethyst geodes and other geodes from mods
              "remove_geodes": false,
              // when biome mods put the same features (often flowers and grass) in opposite orders, vanilla stops
              // world generation with "Feature order cycle found". On (the default), the world generates anyway:
              // only the placing order of those features changes. The features involved are written to the log.
              "fix_feature_order": true
            }
            """;

    private static final String DEFAULT_RECIPES = """
            // Nekoro World Setup: recipe changes. They are applied every time datapacks load, so /reload works.
            {
              "remove": {
                "enabled": false,
                // removes every recipe that makes these items: ids, whole mods ("projecte:*") or item tags ("#minecraft:boats")
                "outputs": ["minecraft:tnt"],
                // removes recipes by recipe id, or all recipes of a mod ("projecte:*")
                "ids": []
              },
              "add": {
                // 2 slabs on top of each other make 1 block, for every slab that has a 3 blocks -> 6 slabs recipe
                "slabs_to_blocks": false,
                // 4 stairs in a square make 3 blocks, for every stairs that has a 6 blocks -> 4 stairs recipe
                "stairs_to_blocks": false,
                // 2 logs on top of each other make 16 sticks
                "logs_to_sticks": false,
                // 8 logs in a ring make 4 chests
                "logs_to_chests": false
              }
            }
            """;
}
