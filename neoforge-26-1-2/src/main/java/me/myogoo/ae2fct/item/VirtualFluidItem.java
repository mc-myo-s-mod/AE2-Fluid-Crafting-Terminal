package me.myogoo.ae2fct.item;

import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.init.AE2FCTItems;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class VirtualFluidItem extends Item {
    public VirtualFluidItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!stack.has(AE2FCTDataComponent.VIRTUAL_FLUID)) {
            return super.getName(stack);
        }
        FluidStack fs = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID).fluid();
        if (fs == null || fs.isEmpty()) {
            return super.getName(stack);
        }
        return fs.getHoverName();
    }

    public static ItemStack createItemStack(Fluid fluid) {
        ItemStack stack = new ItemStack(AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
        stack.set(AE2FCTDataComponent.VIRTUAL_FLUID, new VirtualFluid(new FluidStack(fluid, 1000), 1000));
        return stack;
    }

    public static ResourceHandler<FluidResource> createFluidHandler(ItemStack stack) {
        var virtualFluid = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
        if (virtualFluid == null || virtualFluid.fluid().isEmpty()) {
            return EmptyResourceHandler.instance();
        }
        return new ReadOnlyFluidHandler(virtualFluid);
    }

    private static final class ReadOnlyFluidHandler extends FluidStacksResourceHandler {
        private ReadOnlyFluidHandler(VirtualFluid virtualFluid) {
            super(NonNullList.of(FluidStack.EMPTY, virtualFluid.fluid().copy()), virtualFluid.capacity());
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }
    }
}
