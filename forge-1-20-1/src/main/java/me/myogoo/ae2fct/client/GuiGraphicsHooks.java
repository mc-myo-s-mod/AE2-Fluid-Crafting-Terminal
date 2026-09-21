package me.myogoo.ae2fct.client;

import appeng.client.gui.style.FluidBlitter;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class GuiGraphicsHooks {
    private GuiGraphicsHooks() {
    }

    public static boolean onRenderGuiItem(GuiGraphics guiGraphics, @Nullable LivingEntity livingEntity,
            @Nullable Level level, ItemStack stack, int x, int y, int seed, int z) {
        if (!stack.is(AE2FCTItems.VIRTUAL_FLUID_ITEM.get())) {
            return false;
        }

        VirtualFluid virtualFluid = VirtualFluidStorage.get(stack);
        if (virtualFluid == null || virtualFluid.fluid().isEmpty()) {
            return false;
        }

        FluidBlitter.create(virtualFluid.fluid())
                .dest(x, y, 16, 16)
                .blit(guiGraphics);
        return true;
    }
}
