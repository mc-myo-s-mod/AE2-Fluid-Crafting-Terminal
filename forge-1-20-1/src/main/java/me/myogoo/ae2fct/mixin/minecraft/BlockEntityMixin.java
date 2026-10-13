package me.myogoo.ae2fct.mixin.minecraft;

import me.myogoo.ae2fct.storage.FluidRemainders;
import me.myogoo.ae2fct.storage.RemainderAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements RemainderAccess.Host {
    @Unique private FluidRemainders ae2fct$remainders;

    @Override
    public FluidRemainders ae2fct$remainders() {
        if (ae2fct$remainders == null) ae2fct$remainders = new FluidRemainders((BlockEntity) (Object) this);
        return ae2fct$remainders;
    }
    @Inject(method = "load", at = @At("RETURN"))
    private void ae2fct$reload(CallbackInfo ci) {
        if (ae2fct$remainders != null) ae2fct$remainders.reload();
    }
}
