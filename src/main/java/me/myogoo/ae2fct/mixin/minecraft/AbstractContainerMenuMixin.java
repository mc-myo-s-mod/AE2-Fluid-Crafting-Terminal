package me.myogoo.ae2fct.mixin.minecraft;

import appeng.menu.me.common.MEStorageMenu;
import me.myogoo.ae2fct.api.IMEStorageMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.myogoo.ae2fct.util.FluidCraftingHelper.isVirtualFluidItem;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Shadow
    public abstract ItemStack getCarried();

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Inject(method = "removed", at = @At("HEAD"))
    void ae2fct$onRemoved(Player player, CallbackInfo ci) {
        if(player.containerMenu instanceof MEStorageMenu memenu) {
            if(memenu instanceof IMEStorageMenu menu) {
                menu.ae2fct$returnVirtualFluidItems(player);
            }
        }
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    void ae2fct$throwVirtualFluid(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (clickType == ClickType.THROW) {
            if (slotId >= 0 && slotId < this.slots.size()) {
                Slot slot = this.slots.get(slotId);
                if (isVirtualFluidItem(slot.getItem())) {
                    ci.cancel();
                }
            } else if (slotId == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE && isVirtualFluidItem(this.getCarried())) {
                ci.cancel();
            }
        } else if (clickType == ClickType.PICKUP
                && slotId == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE
                && isVirtualFluidItem(this.getCarried())) {
            ci.cancel();
        }
    }

}
