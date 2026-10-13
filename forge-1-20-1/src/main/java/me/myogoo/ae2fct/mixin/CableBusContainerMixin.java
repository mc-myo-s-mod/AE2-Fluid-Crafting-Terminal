package me.myogoo.ae2fct.mixin;

import appeng.parts.CableBusContainer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.nbt.CompoundTag;
import me.myogoo.ae2fct.storage.RemainderAccess;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CableBusContainer.class, remap = false)
public abstract class CableBusContainerMixin {
    @Shadow public abstract BlockEntity getBlockEntity();
    @Unique private boolean ae2fct$reading;

    @WrapMethod(method = "readFromNBT")
    private void ae2fct$read(CompoundTag data, Operation<Void> original) {
        boolean wasReading = ae2fct$reading;
        ae2fct$reading = true;
        try {
            original.call(data);
        } finally {
            ae2fct$reading = wasReading;
        }
    }

    @Inject(method = "removePartWithoutUpdates", at = @At("HEAD"))
    private void ae2fct$remove(Direction side, CallbackInfo ci) {
        if (!ae2fct$reading) {
            ((RemainderAccess.Host) getBlockEntity()).ae2fct$remainders()
                    .retire(side == null ? "center" : side.getName());
        }
    }
}
