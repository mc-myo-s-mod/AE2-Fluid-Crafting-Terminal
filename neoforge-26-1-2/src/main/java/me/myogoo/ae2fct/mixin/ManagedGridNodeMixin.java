package me.myogoo.ae2fct.mixin;

import appeng.me.GridNode;
import appeng.me.ManagedGridNode;
import me.myogoo.ae2fct.storage.FluidRemainders;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ManagedGridNode.class, remap = false)
public abstract class ManagedGridNodeMixin {
    @Shadow private GridNode node;
    @Shadow private String tagName;
    @Unique private Level ae2fct$level;
    @Unique private BlockPos ae2fct$pos;

    @Inject(method = "create", at = @At("HEAD"))
    private void ae2fct$location(Level level, BlockPos pos, CallbackInfo ci) {
        ae2fct$level = level;
        ae2fct$pos = pos;
    }

    @Inject(method = "createNode", at = @At(value = "INVOKE", target = "Lappeng/me/GridNode;markReady()V"))
    private void ae2fct$bind(CallbackInfo ci) {
        FluidRemainders.bind(node, ae2fct$level, ae2fct$pos, tagName);
    }
}
