package me.myogoo.ae2fct.item;

import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VirtualFluidItem extends Item {
    private static final int BUCKET_VOLUME = 1000;

    public VirtualFluidItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        VirtualFluid virtualFluid = VirtualFluidStorage.get(stack);
        if (virtualFluid == null || virtualFluid.fluid().isEmpty()) {
            return super.getName(stack);
        }
        return virtualFluid.fluid().getDisplayName();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        VirtualFluid virtualFluid = VirtualFluidStorage.get(stack);
        if (virtualFluid != null && !virtualFluid.fluid().isEmpty()) {
            tooltip.add(Component.literal(virtualFluid.fluid().getAmount() + " mB").withStyle(ChatFormatting.GRAY));
        }
    }

    public static ItemStack createItemStack(Fluid fluid) {
        ItemStack stack = new ItemStack(AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
        stack.setCount(1);
        VirtualFluidStorage.set(stack, new VirtualFluid(new FluidStack(fluid, BUCKET_VOLUME), BUCKET_VOLUME));
        return stack;
    }
}
