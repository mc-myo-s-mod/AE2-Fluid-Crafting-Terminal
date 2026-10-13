package me.myogoo.ae2fct.mixin.minecraft;

import me.myogoo.ae2fct.storage.FluidRemainders;
import me.myogoo.ae2fct.storage.RemainderAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Unique
    private void ae2fct$promote(BlockPos pos) {
        var chunk = (LevelChunk) (Object) this;
        if (!chunk.getLevel().isClientSide() && FluidRemainders.hasSavedBalance(chunk.getBlockEntityNbt(pos))) {
            chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        }
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void ae2fct$beforeState(BlockPos pos, BlockState state, boolean moving,
            CallbackInfoReturnable<BlockState> cir) {
        ae2fct$promote(pos);
    }

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    private void ae2fct$beforeRemove(BlockPos pos, CallbackInfo ci) {
        ae2fct$promote(pos);
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void ae2fct$beforeReplacement(BlockEntity host, CallbackInfo ci) {
        ae2fct$promote(host.getBlockPos());
    }

    // Capture the actual displaced entity; HEAD cannot distinguish rejected replacements or unloading.
    @WrapOperation(method = {"removeBlockEntity", "setBlockEntity"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V"))
    private void ae2fct$destroy(BlockEntity host, Operation<Void> original) {
        ((RemainderAccess.Host) host).ae2fct$remainders().retire(null);
        original.call(host);
    }
}
