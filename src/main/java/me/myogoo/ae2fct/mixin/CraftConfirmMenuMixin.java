package me.myogoo.ae2fct.mixin;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.core.AELog;
import appeng.helpers.IMenuCraftingPacket;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.me.crafting.CraftConfirmMenu;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import me.myogoo.myotus.menu.TerminalUpgradeHelper;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = CraftConfirmMenu.class, remap = false)
public abstract class CraftConfirmMenuMixin {
    @Inject(method = "openWithCraftingList", at = @At("HEAD"), cancellable = true)
    private static void convertVirtualFluidToCraft(@Nullable IActionHost terminal, ServerPlayer player,
            @Nullable MenuLocator locator, List<IMenuCraftingPacket.AutoCraftEntry> stacksToCraft, CallbackInfo ci) {
        if (terminal == null || locator == null || stacksToCraft == null || stacksToCraft.isEmpty()) {
            return;
        }

        boolean hasVirtualFluid = false;
        for (var entry : stacksToCraft) {
            if (entry.what() != null && entry.what().getItem() == AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
                hasVirtualFluid = true;
                break;
            }
        }

        if (!hasVirtualFluid) {
            return;
        }

        if (player.containerMenu instanceof appeng.menu.me.common.MEStorageMenu menu
                && !TerminalUpgradeHelper.hasUpgrade(menu, AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get())) {
            ci.cancel();
            return;
        }

        ci.cancel();

        var firstToCraft = stacksToCraft.get(0);
        var subsequentCrafts = stacksToCraft.subList(1, stacksToCraft.size());

        try {
            MenuOpener.open(CraftConfirmMenu.TYPE, player, locator);

            if (player.containerMenu instanceof CraftConfirmMenu craftConfirmMenu) {
                AEKey craftKey = resolveAutoCraftKey(firstToCraft);
                int craftAmount;

                if (craftKey instanceof AEFluidKey fluidKey) {
                    int totalNeeded = firstToCraft.slots().size() * AEFluidKey.AMOUNT_BUCKET;
                    long storedAmount = 0;
                    var node = terminal.getActionableNode();
                    if (node != null) {
                        storedAmount = node.getGrid().getStorageService().getCachedInventory().get(fluidKey);
                    }
                    craftAmount = (int) Math.max(0, totalNeeded - storedAmount);
                    if (craftAmount <= 0) {
                        return;
                    }
                } else {
                    craftAmount = firstToCraft.slots().size();
                }

                if (!craftConfirmMenu.planJob(craftKey, craftAmount, CalculationStrategy.CRAFT_LESS)) {
                    craftConfirmMenu.setValidMenu(false);
                    return;
                }

                try {
                    var queueField = CraftConfirmMenu.class.getDeclaredField("autoCraftingQueue");
                    queueField.setAccessible(true);
                    queueField.set(craftConfirmMenu, subsequentCrafts);

                    var slotsField = CraftConfirmMenu.class.getDeclaredField("requestedSlots");
                    slotsField.setAccessible(true);
                    slotsField.set(craftConfirmMenu, firstToCraft.slots());
                } catch (ReflectiveOperationException e) {
                    AELog.debug("Failed to update CraftConfirmMenu private fields", e);
                }

                craftConfirmMenu.broadcastChanges();
            }
        } catch (Throwable e) {
            AELog.debug("Failed to open fluid autocrafting confirmation", e);
        }
    }

    private static AEKey resolveAutoCraftKey(IMenuCraftingPacket.AutoCraftEntry entry) {
        if (entry.what() != null && entry.what().getItem() == AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
            VirtualFluid virtualFluid = VirtualFluidStorage.get(entry.what().toStack());
            if (virtualFluid != null && !virtualFluid.fluid().isEmpty()) {
                AEFluidKey fluidKey = AEFluidKey.of(virtualFluid.fluid());
                if (fluidKey != null) {
                    return fluidKey;
                }
            }
        }
        return entry.what();
    }
}
