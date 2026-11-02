package dev.xardas.nekoroworldsetup;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** The small enchantment rules from world.json "enchantments.extras" that work through events. */
final class EnchantExtras {
    private EnchantExtras() { }

    static int level(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Holder<Enchantment> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null);
        return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!WsConfig.get().world.featherFallingFarmland || !(event.getEntity() instanceof LivingEntity entity)) return;
        Holder<Enchantment> featherFalling = entity.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.FEATHER_FALLING).orElse(null);
        if (featherFalling != null && EnchantmentHelper.getEnchantmentLevel(featherFalling, entity) > 0) event.setCanceled(true);
    }

    static void onMobXp(LivingExperienceDropEvent event) {
        WsConfig.World cfg = WsConfig.get().world;
        Player player = event.getAttackingPlayer();
        if (!cfg.lootingFortuneXp || player == null || event.getDroppedExperience() <= 0) return;
        int looting = level(player.level(), Enchantments.LOOTING, player.getMainHandItem());
        if (looting > 0) {
            int before = event.getDroppedExperience();
            event.setDroppedExperience(boosted(before, looting, cfg));
            NekoroWorldSetup.LOGGER.debug("Looting {}: {} -> {} XP from {}", looting, before, event.getDroppedExperience(), event.getEntity().getType());
        }
    }

    static void onBlockXp(BlockDropsEvent event) {
        WsConfig.World cfg = WsConfig.get().world;
        if (!cfg.lootingFortuneXp || event.getDroppedExperience() <= 0) return;
        int fortune = level(event.getLevel(), Enchantments.FORTUNE, event.getTool());
        if (fortune > 0) event.setDroppedExperience(boosted(event.getDroppedExperience(), fortune, cfg));
    }

    /** Extra XP of level * (level + 1) / 10 times the XP (III: +120%, X: +1100%), at most xp_bonus_max more. */
    private static int boosted(int xp, int level, WsConfig.World cfg) {
        double factor = level * (level + 1) / 10.0;
        return xp + (int) Math.min(cfg.xpBonusMax, Math.floor(xp * factor));
    }

    /** After a projectile hit the target is normally immune for half a second; with this, the next arrow hits too. */
    static void onDamaged(LivingDamageEvent.Post event) {
        if (WsConfig.get().world.noProjectileImmunity && event.getSource().getDirectEntity() instanceof Projectile) {
            event.getEntity().invulnerableTime = 0;
        }
    }
}
