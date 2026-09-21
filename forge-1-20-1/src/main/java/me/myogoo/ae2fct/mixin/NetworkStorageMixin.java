package me.myogoo.ae2fct.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.me.storage.NetworkStorage;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetworkStorage.class, remap = false)
public abstract class NetworkStorageMixin {
    @Shadow
    public abstract long insert(AEKey what, long amount, Actionable type, IActionSource src);

    @Shadow
    public abstract long extract(AEKey what, long amount, Actionable mode, IActionSource source);

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void onInsert(AEKey what, long amount, Actionable type, IActionSource src,
            CallbackInfoReturnable<Long> cir) {
        if (!(what instanceof AEItemKey itemKey) || itemKey.getItem() != AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
            return;
        }

        VirtualFluid virtualFluid = VirtualFluidStorage.get(itemKey.toStack());
        if (virtualFluid == null) {
            return;
        }

        FluidStack fluidStack = virtualFluid.fluid();
        if (fluidStack.isEmpty()) {
            return;
        }

        long fluidAmountPerItem = fluidStack.getAmount();
        long totalFluidToInsert = amount * fluidAmountPerItem;
        AEFluidKey fluidKey = AEFluidKey.of(fluidStack);
        if (fluidKey == null) {
            return;
        }

        long simulatedFluidInserted = this.insert(fluidKey, totalFluidToInsert, Actionable.SIMULATE, src);
        long itemsToInsert = simulatedFluidInserted / fluidAmountPerItem;
        if (itemsToInsert > 0 && type == Actionable.MODULATE) {
            this.insert(fluidKey, itemsToInsert * fluidAmountPerItem, Actionable.MODULATE, src);
        }
        cir.setReturnValue(itemsToInsert);
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void onExtract(AEKey what, long amount, Actionable mode, IActionSource source,
            CallbackInfoReturnable<Long> cir) {
        if (!(what instanceof AEItemKey itemKey) || itemKey.getItem() != AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
            return;
        }

        VirtualFluid virtualFluid = VirtualFluidStorage.get(itemKey.toStack());
        if (virtualFluid == null) {
            return;
        }

        FluidStack fluidStack = virtualFluid.fluid();
        if (fluidStack.isEmpty()) {
            return;
        }

        long fluidAmountPerItem = fluidStack.getAmount();
        long totalFluidToExtract = amount * fluidAmountPerItem;
        AEFluidKey fluidKey = AEFluidKey.of(fluidStack);
        if (fluidKey == null) {
            return;
        }

        long simulatedFluidExtracted = this.extract(fluidKey, totalFluidToExtract, Actionable.SIMULATE, source);
        long itemsToExtract = simulatedFluidExtracted / fluidAmountPerItem;
        if (itemsToExtract > 0 && mode == Actionable.MODULATE) {
            this.extract(fluidKey, itemsToExtract * fluidAmountPerItem, Actionable.MODULATE, source);
        }
        cir.setReturnValue(itemsToExtract);
    }
}
