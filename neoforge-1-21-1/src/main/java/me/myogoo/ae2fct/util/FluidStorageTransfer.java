package me.myogoo.ae2fct.util;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.init.AE2FCTItems;
import net.minecraft.world.item.ItemStack;
import java.util.function.Consumer;

public final class FluidStorageTransfer {
    private FluidStorageTransfer() {
    }

    public static ItemStack createItem(AEFluidKey key, int amount) {
        ItemStack stack = new ItemStack(AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
        stack.set(AE2FCTDataComponent.VIRTUAL_FLUID, new VirtualFluid(key.toStack(amount), amount));
        return stack;
    }

    public static ItemStack extractToItem(MEStorage storage, IEnergySource energy, AEFluidKey key,
            int amount, IActionSource source) {
        if (StorageHelper.poweredExtraction(energy, storage, key, amount, source, Actionable.SIMULATE) < amount) {
            return ItemStack.EMPTY;
        }
        long extracted = StorageHelper.poweredExtraction(energy, storage, key, amount, source, Actionable.MODULATE);
        return extracted > 0 ? createItem(key, (int) extracted) : ItemStack.EMPTY;
    }

    public static void returnToStorage(ItemStack stack, long count, MEStorage storage, IEnergySource energy,
            IActionSource source, Consumer<ItemStack> returnRemainder) {
        VirtualFluid fluid = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
        if (fluid == null || fluid.fluid().isEmpty()) {
            return;
        }
        AEFluidKey key = AEFluidKey.of(fluid.fluid());
        int perItem = fluid.fluid().getAmount();
        long requested = Math.min(count, stack.getCount());
        for (long i = 0; i < requested; i++) {
            long inserted = StorageHelper.poweredInsert(energy, storage, key, perItem, source, Actionable.MODULATE);
            if (inserted == 0) {
                return;
            }
            if (inserted < perItem) {
                var remaining = new VirtualFluid(key.toStack(perItem - (int) inserted), fluid.capacity());
                if (stack.getCount() == 1) {
                    stack.set(AE2FCTDataComponent.VIRTUAL_FLUID, remaining);
                } else {
                    stack.shrink(1);
                    ItemStack remainder = new ItemStack(AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
                    remainder.set(AE2FCTDataComponent.VIRTUAL_FLUID, remaining);
                    returnRemainder.accept(remainder);
                }
                return;
            }
            stack.shrink(1);
        }
    }
}
