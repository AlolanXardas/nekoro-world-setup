package dev.xardas.nekoroworldsetup;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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

/** The small enchantment rules from world.json "enchantments.extras" that work through events. */
public final class EnchantExtras {
    private EnchantExtras() { }

    static int level(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Holder<Enchantment> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key).orElse(null);
        return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    /** True when this entity would trample farmland but its Feather Falling protects it. */
    public static boolean protectsFarmland(Entity trampler) {
        if (!WsConfig.get().world.featherFallingFarmland || !(trampler instanceof LivingEntity entity)) return false;
        Holder<Enchantment> featherFalling = entity.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.FEATHER_FALLING).orElse(null);
        return featherFalling != null && EnchantmentHelper.getEnchantmentLevel(featherFalling, entity) > 0;
    }

    /** XP a killed mob drops, raised by the killer's Looting. */
    public static int mobXp(LivingEntity mob, Player killer, int xp) {
        WsConfig.World cfg = WsConfig.get().world;
        if (!cfg.lootingFortuneXp || killer == null || xp <= 0) return xp;
        int looting = level(killer.level(), Enchantments.LOOTING, killer.getMainHandItem());
        if (looting <= 0) return xp;
        int after = boosted(xp, looting, cfg);
        NekoroWorldSetup.LOGGER.debug("Looting {}: {} -> {} XP from {}", looting, xp, after, mob.getType());
        return after;
    }

    /** XP a mined block drops, raised by the tool's Fortune. */
    public static int blockXp(Level level, ItemStack tool, int xp) {
        WsConfig.World cfg = WsConfig.get().world;
        if (!cfg.lootingFortuneXp || xp <= 0 || tool == null) return xp;
        int fortune = level(level, Enchantments.FORTUNE, tool);
        return fortune > 0 ? boosted(xp, fortune, cfg) : xp;
    }

    /** Extra XP of level * (level + 1) / 10 times the XP (III: +120%, X: +1100%), at most xp_bonus_max more. */
    private static int boosted(int xp, int level, WsConfig.World cfg) {
        double factor = level * (level + 1) / 10.0;
        return xp + (int) Math.min(cfg.xpBonusMax, Math.floor(xp * factor));
    }

    /** After a projectile hit the target is normally immune for half a second; with this, the next arrow hits too. */
    public static void afterDamage(LivingEntity entity, DamageSource source) {
        if (WsConfig.get().world.noProjectileImmunity && source.getDirectEntity() instanceof Projectile) {
            entity.invulnerableTime = 0;
        }
    }
}
