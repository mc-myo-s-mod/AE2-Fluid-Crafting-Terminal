package me.myogoo.ae2fct.mixin;

import me.myogoo.ae2fct.client.GuiGraphicsHooks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @SuppressWarnings("ConstantConditions")
    @Inject(method = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At("HEAD"), cancellable = true)
    private void ae2fct$renderVirtualFluidInGui(@Nullable LivingEntity livingEntity, @Nullable Level level,
            ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
        GuiGraphics self = (GuiGraphics) (Object) this;
        if (GuiGraphicsHooks.onRenderGuiItem(self, livingEntity, level, stack, x, y, seed, z)) {
            ci.cancel();
        }
    }
}
