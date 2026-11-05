package dev.xardas.nekoroworldsetup;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import dev.xardas.nekoroworldsetup.worldgen.WorldSetupBiomeModifier;

/** NeoForge entry point: connects NeoForge's events to the shared code. */
@Mod(NekoroWorldSetup.MOD_ID)
public final class NekoroWorldSetupNeoForge {
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, NekoroWorldSetup.MOD_ID);

    static {
        BIOME_MODIFIERS.register("config", () -> WorldSetupBiomeModifier.CODEC);
    }

    public NekoroWorldSetupNeoForge(IEventBus modBus) {
        WsConfig.load();
        BIOME_MODIFIERS.register(modBus);
        modBus.addListener((AddPackFindersEvent event) -> {
            if (event.getPackType() != PackType.SERVER_DATA) return;
            RepositorySource source = GlobalDatapacks.source();
            if (source != null) event.addRepositorySource(source);
        });

        IEventBus bus = NeoForge.EVENT_BUS;
        // config files are read again whenever datapacks (re)load, before recipes are built
        bus.addListener((AddReloadListenerEvent event) -> WsConfig.load());
        bus.addListener((TagsUpdatedEvent event) -> {
            EnchantLevels.rebuild(event.getRegistryAccess(), event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED);
            // after /reload; on the very first load there is no server yet, ServerAboutToStart handles that one
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD && server != null) {
                RecipeTweaks.apply(server);
            }
        });
        bus.addListener((ServerAboutToStartEvent event) -> RecipeTweaks.apply(event.getServer()));
        bus.addListener((ServerStartedEvent event) -> StartSetup.onServerStarted(event.getServer()));
        bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) StartSetup.onPlayerJoin(player);
        });
        bus.addListener(EventPriority.LOWEST, (BlockEvent.BreakEvent event) -> {
            if (!event.isCanceled() && event.getLevel() instanceof net.minecraft.world.level.Level level) {
                WaterFill.onBreak(level, event.getPos(), event.getState());
            }
        });
        bus.addListener((LevelTickEvent.Post event) -> WaterFill.onLevelTick(event.getLevel()));
        bus.addListener((BlockEvent.FluidPlaceBlockEvent event) -> {
            if (FluidRules.blocked(event.getNewState())) event.setNewState(event.getOriginalState());
        });
        bus.addListener(EventPriority.LOW, (FinalizeSpawnEvent event) -> {
            if (event.getEntity() instanceof Mob mob && MobFilter.onFinalizeSpawn(mob, event.getSpawnType())) {
                event.setSpawnCancelled(true);
                event.setCanceled(true);
            }
        });
        bus.addListener((EntityJoinLevelEvent event) -> {
            if (MobFilter.onJoin(event.getEntity(), event.loadedFromDisk())) event.setCanceled(true);
        });
        bus.addListener((PlayerTickEvent.Post event) -> WorldEdges.onPlayerTick(event.getEntity()));
        bus.addListener((ServerTickEvent.Post event) -> RealTime.onServerTick(event.getServer()));
        bus.addListener((LivingFallEvent event) -> {
            if (WorldEdges.safeLanding(event.getEntity())) {
                event.setDistance(0);
                event.setDamageMultiplier(0);
            }
        });
        bus.addListener((BlockEvent.FarmlandTrampleEvent event) -> {
            if (EnchantExtras.protectsFarmland(event.getEntity())) event.setCanceled(true);
        });
        bus.addListener((LivingExperienceDropEvent event) ->
                event.setDroppedExperience(EnchantExtras.mobXp(event.getEntity(), event.getAttackingPlayer(), event.getDroppedExperience())));
        bus.addListener((BlockDropsEvent event) ->
                event.setDroppedExperience(EnchantExtras.blockXp(event.getLevel(), event.getTool(), event.getDroppedExperience())));
        bus.addListener((LivingDamageEvent.Post event) -> EnchantExtras.afterDamage(event.getEntity(), event.getSource()));
        bus.addListener((BonemealEvent event) -> {
            if (!event.isValidBonemealTarget() && BoneMeal.tryUse(event.getLevel(), event.getPos(), event.getState(), event.getStack())) {
                // canceled + successful: the item counts as used, and vanilla does nothing more
                event.setCanceled(true);
                event.setSuccessful(true);
            }
        });
    }
}
