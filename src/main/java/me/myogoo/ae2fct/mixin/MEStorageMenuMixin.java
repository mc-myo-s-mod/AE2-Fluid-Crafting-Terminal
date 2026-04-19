package me.myogoo.ae2fct.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.common.MEStorageMenu;
import me.myogoo.ae2fct.api.IMEStorageMenu;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.myotus.menu.TerminalUpgradeHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.myogoo.ae2fct.util.FluidCraftingHelper.isVirtualFluidItem;

@Mixin(value = MEStorageMenu.class, remap = false)
public abstract class MEStorageMenuMixin extends AEBaseMenu implements IMEStorageMenu {

    private static final long FLUID_UNIT = 1000;

    @Shadow
    protected MEStorage storage;

    @Shadow
    protected IEnergySource energySource;

    public MEStorageMenuMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Unique
    private ItemStack ae2fct$createVirtualFluidItem(AEFluidKey fluidKey, long amount) {
        ItemStack stack = new ItemStack(AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
        stack.set(AE2FCTDataComponent.VIRTUAL_FLUID,
                new VirtualFluid(new FluidStack(fluidKey.getFluid(), (int) amount), (int) amount));
        return stack;
    }

    @Unique
    private boolean tryExtractFluidAndCarry(MEStorageMenu menu, AEFluidKey fluidKey) {
        long extracted = StorageHelper.poweredExtraction(
                this.energySource, this.storage, fluidKey, FLUID_UNIT, menu.getActionSource());
        if (extracted == FLUID_UNIT) {
            menu.setCarried(ae2fct$createVirtualFluidItem(fluidKey, extracted));
            return true;
        }
        return false;
    }

    @Unique
    private void returnVirtualFluidToStorage(ItemStack stack, long amount) {
        long inserted = StorageHelper.poweredInsert(
                this.energySource, this.storage, AEItemKey.of(stack),
                amount, this.getActionSource(), Actionable.MODULATE);
        if (inserted > 0) {
            stack.shrink((int) inserted);
        }
    }

    @Inject(method = "handleNetworkInteraction", at = @At("HEAD"), cancellable = true)
    private void interceptFluidClick(ServerPlayer player, @Nullable AEKey clickedKey, InventoryAction action,
            CallbackInfo ci) {
        if (!(clickedKey instanceof AEFluidKey fluidKey)) {
            return;
        }

        MEStorageMenu menu = (MEStorageMenu) (Object) this;
        if (!TerminalUpgradeHelper.hasUpgrade(menu, AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get())) {
            return;
        }

        boolean carriedIsVirtual = isVirtualFluidItem(menu.getCarried());

        switch (action) {
            case PICKUP_OR_SET_DOWN -> {
                if (menu.getCarried().isEmpty()) {
                    if (tryExtractFluidAndCarry(menu, fluidKey)) {
                        ci.cancel();
                    }
                } else if (carriedIsVirtual) {
                    returnVirtualFluidToStorage(menu.getCarried(), menu.getCarried().getCount());
                    if (menu.getCarried().isEmpty()) {
                        menu.setCarried(ItemStack.EMPTY);
                    }
                    ci.cancel();
                }
            }
            case SPLIT_OR_PLACE_SINGLE -> {
                if (menu.getCarried().isEmpty()) {
                    long simulated = this.storage.extract(
                            fluidKey, FLUID_UNIT, Actionable.SIMULATE, menu.getActionSource());
                    if (simulated != FLUID_UNIT) {
                        return;
                    }
                    if (tryExtractFluidAndCarry(menu, fluidKey)) {
                        ci.cancel();
                    }
                } else if (carriedIsVirtual) {
                    returnVirtualFluidToStorage(menu.getCarried(), 1);
                    if (menu.getCarried().isEmpty()) {
                        menu.setCarried(ItemStack.EMPTY);
                    }
                    ci.cancel();
                }
            }
            default -> {
            }
        }
    }

    @Unique
    public void ae2fct$returnVirtualFluidItems(Player player) {
        if (player.level().isClientSide() || this.storage == null || this.energySource == null) {
            return;
        }

        ItemStack carried = this.getCarried();
        if (isVirtualFluidItem(carried)) {
            returnVirtualFluidToStorage(carried, carried.getCount());
            if (carried.isEmpty()) {
                this.setCarried(ItemStack.EMPTY);
            }
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isVirtualFluidItem(stack)) {
                returnVirtualFluidToStorage(stack, stack.getCount());
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        for (Slot slot : this.slots) {
            if (slot == null || this.isPlayerSideSlot(slot)) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (!isVirtualFluidItem(stack)) {
                continue;
            }

            returnVirtualFluidToStorage(stack, stack.getCount());
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }
        }
    }
}
