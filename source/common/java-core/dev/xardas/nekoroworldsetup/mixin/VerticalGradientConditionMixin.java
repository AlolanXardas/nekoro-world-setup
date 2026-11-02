package dev.xardas.nekoroworldsetup.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.xardas.nekoroworldsetup.WsConfig;

/**
 * Flat bedrock. The bedrock layers come from surface rules with a vertical gradient named
 * "bedrock_floor" (always bedrock at the bottom, random up to 5 blocks above) and "bedrock_roof"
 * (the Nether roof, the same from the top down). Narrowing the gradient to a single block leaves one flat layer.
 * The changes are idempotent, so doing them on every call is safe.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$VerticalGradientConditionSource")
public abstract class VerticalGradientConditionMixin {
    @Shadow @Final private ResourceLocation randomName;
    @Shadow @Final @Mutable private VerticalAnchor trueAtAndBelow;
    @Shadow @Final @Mutable private VerticalAnchor falseAtAndAbove;

    @Inject(method = "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;",
            at = @At("HEAD"))
    private void nekoroWorldSetup$flatBedrock(CallbackInfoReturnable<?> cir) {
        String name = randomName.getPath();
        WsConfig.Worldgen cfg = WsConfig.get().worldgen;
        if (cfg.flatBedrockFloor && name.equals("bedrock_floor")) {
            falseAtAndAbove = shift(trueAtAndBelow, 1);
        } else if (cfg.flatBedrockRoof && name.equals("bedrock_roof")) {
            trueAtAndBelow = shift(falseAtAndAbove, -1);
        }
    }

    private static VerticalAnchor shift(VerticalAnchor anchor, int up) {
        if (anchor instanceof VerticalAnchor.AboveBottom a) return VerticalAnchor.aboveBottom(a.offset() + up);
        if (anchor instanceof VerticalAnchor.BelowTop b) return VerticalAnchor.belowTop(b.offset() - up);
        if (anchor instanceof VerticalAnchor.Absolute abs) return VerticalAnchor.absolute(abs.y() + up);
        return anchor;
    }
}
