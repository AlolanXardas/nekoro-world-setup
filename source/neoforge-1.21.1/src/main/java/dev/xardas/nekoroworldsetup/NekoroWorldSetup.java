package dev.xardas.nekoroworldsetup;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import dev.xardas.nekoroworldsetup.worldgen.WorldSetupBiomeModifier;

@Mod(NekoroWorldSetup.MOD_ID)
public final class NekoroWorldSetup {
    public static final String MOD_ID = "nekoro_world_setup";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MOD_ID);

    static {
        BIOME_MODIFIERS.register("config", () -> WorldSetupBiomeModifier.CODEC);
    }

    public NekoroWorldSetup(IEventBus modBus) {
        WsConfig.load();
        BIOME_MODIFIERS.register(modBus);
        modBus.addListener(GlobalDatapacks::onAddPackFinders);

        IEventBus bus = NeoForge.EVENT_BUS;
        // config files are read again whenever datapacks (re)load, before recipes are built
        bus.addListener((AddReloadListenerEvent event) -> WsConfig.load());
        bus.addListener((TagsUpdatedEvent event) -> {
            EnchantLevels.rebuild(event);
            // after /reload; on the very first load there is no server yet, ServerAboutToStart handles that one
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD && server != null) {
                RecipeTweaks.apply(server);
            }
        });
        bus.addListener((ServerAboutToStartEvent event) -> RecipeTweaks.apply(event.getServer()));
        bus.addListener((ServerStartedEvent event) -> StartSetup.onServerStarted(event));
        bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> StartSetup.onPlayerJoin(event));
        bus.addListener(EventPriority.LOWEST, (BlockEvent.BreakEvent event) -> WaterFill.onBreak(event));
        bus.addListener((LevelTickEvent.Post event) -> WaterFill.onLevelTick(event));
        bus.addListener((BlockEvent.FluidPlaceBlockEvent event) -> FluidRules.onFluidPlace(event));
        bus.addListener(EventPriority.LOW, (FinalizeSpawnEvent event) -> MobFilter.onFinalizeSpawn(event));
        bus.addListener((EntityJoinLevelEvent event) -> MobFilter.onJoin(event));
        bus.addListener((PlayerTickEvent.Post event) -> WorldEdges.onPlayerTick(event));
        bus.addListener((LivingFallEvent event) -> WorldEdges.onFall(event));
        bus.addListener((BlockEvent.FarmlandTrampleEvent event) -> EnchantExtras.onFarmlandTrample(event));
        bus.addListener((LivingExperienceDropEvent event) -> EnchantExtras.onMobXp(event));
        bus.addListener((BlockDropsEvent event) -> EnchantExtras.onBlockXp(event));
        bus.addListener((LivingDamageEvent.Post event) -> EnchantExtras.onDamaged(event));
        bus.addListener((BonemealEvent event) -> BoneMeal.onBonemeal(event));
    }
}
