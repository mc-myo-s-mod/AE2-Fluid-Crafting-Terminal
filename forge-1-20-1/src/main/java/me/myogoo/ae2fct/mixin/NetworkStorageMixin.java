package me.myogoo.ae2fct.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.me.storage.NetworkStorage;
import me.myogoo.ae2fct.storage.FluidRemainderService;
import me.myogoo.ae2fct.storage.RemainderAccess;
import org.spongepowered.asm.mixin.Unique;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import me.myogoo.ae2fct.init.AE2FCTItems;
import net.minecraftforge.fluids.FluidStack;

@Mixin(value = NetworkStorage.class, remap = false)
public abstract class NetworkStorageMixin implements RemainderAccess.Network {
    @Unique private FluidRemainderService ae2fct$service;

    @Override
    public void ae2fct$service(FluidRemainderService service) {
        ae2fct$service = service;
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void onInsert(AEKey what, long amount, Actionable type, IActionSource src,
            CallbackInfoReturnable<Long> cir) {
        if (what instanceof AEItemKey itemKey && itemKey.getItem() == AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
            VirtualFluid virtualFluid = VirtualFluidStorage.get(itemKey.toStack());
            if (virtualFluid != null) {
                FluidStack fluidStack = virtualFluid.fluid();
                if (!fluidStack.isEmpty()) {
                    int fluidAmountPerItem = fluidStack.getAmount();
                    if (amount <= 0) {
                        cir.setReturnValue(0L);
                        return;
                    }
                    AEFluidKey fluidKey = AEFluidKey.of(fluidStack);

                    if (fluidKey != null) {
                        cir.setReturnValue(ae2fct$service == null ? 0L : ae2fct$service.convert(
                                fluidKey, fluidAmountPerItem, amount, type, src, true));
                    }
                }
            }
        }
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void onExtract(AEKey what, long amount, Actionable mode, IActionSource source,
            CallbackInfoReturnable<Long> cir) {
        if (what instanceof AEItemKey itemKey && itemKey.getItem() == AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
            VirtualFluid virtualFluid = VirtualFluidStorage.get(itemKey.toStack());
            if (virtualFluid != null) {
                FluidStack fluidStack = virtualFluid.fluid();
                if (!fluidStack.isEmpty()) {
                    int fluidAmountPerItem = fluidStack.getAmount();
                    if (amount <= 0) {
                        cir.setReturnValue(0L);
                        return;
                    }
                    AEFluidKey fluidKey = AEFluidKey.of(fluidStack);

                    if (fluidKey != null) {
                        cir.setReturnValue(ae2fct$service == null ? 0L : ae2fct$service.convert(
                                fluidKey, fluidAmountPerItem, amount, mode, source, false));
                    }
                }
            }
        }
    }
}
