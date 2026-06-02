package me.myogoo.ae2fct.mixin;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.core.AELog;
import appeng.helpers.ICraftingGridMenu;
import appeng.menu.MenuOpener;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.locator.MenuHostLocator;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.init.AE2FCTItems;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = CraftConfirmMenu.class, remap = false)
public abstract class CraftConfirmMenuMixin {

    /**
     * openWithCraftingList에서 AutoCraftEntry의 what이 VirtualFluidItem인 경우
     * planJob에 AEFluidKey를 전달하여 fluid 자동 조합을 요청합니다.
     */
    @Inject(method = "openWithCraftingList", at = @At("HEAD"), cancellable = true)
    private static void convertVirtualFluidToCraft(@Nullable IActionHost terminal, ServerPlayer player,
                                                   @Nullable MenuHostLocator locator, List<ICraftingGridMenu.AutoCraftEntry> stacksToCraft,
                                                   CallbackInfo ci) {
        if (terminal == null || locator == null || stacksToCraft == null || stacksToCraft.isEmpty()) {
            return;
        }

        // VirtualFluidItem이 포함되어 있는지 확인
        boolean hasVirtualFluid = false;
        for (var entry : stacksToCraft) {
            if (entry.what() != null && entry.what().getItem() == AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
                hasVirtualFluid = true;
                break;
            }
        }

        if (!hasVirtualFluid) {
            return; // VirtualFluidItem이 없으면 원본 로직 실행
        }
        if (player.containerMenu instanceof MEStorageMenu menu) {
            if (!FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(menu)) {
                ci.cancel();
                return;
            }
        }

        // 원본 로직을 직접 수행하되, VirtualFluidItem에 대해 AEFluidKey를 사용
        ci.cancel();

        var firstToCraft = stacksToCraft.get(0);
        var subsequentCrafts = stacksToCraft.subList(1, stacksToCraft.size());

        try {
            MenuOpener.open(CraftConfirmMenu.TYPE, player, locator);

            if (player.containerMenu instanceof CraftConfirmMenu ccc) {
                // VirtualFluidItem인 경우 AEFluidKey로 변환
                AEKey craftKey = resolveAutoCraftKey(firstToCraft);
                int craftAmount;
                if (craftKey instanceof AEFluidKey fluidKey) {
                    // fluid는 1 bucket(1000mB) 단위 × 슬롯 수
                    int totalNeeded = firstToCraft.slots().size() * 1000;
                    // 네트워크에 이미 저장된 fluid 양을 차감
                    long storedAmount = 0;
                    var node = terminal.getActionableNode();
                    if (node != null) {
                        var cachedStorage = node.getGrid().getStorageService().getCachedInventory();
                        storedAmount = cachedStorage.get(fluidKey);
                    }
                    craftAmount = (int) Math.max(0, totalNeeded - storedAmount);
                    if (craftAmount <= 0) {
                        // 이미 충분한 양이 있으면 auto-crafting 불필요
                        return;
                    }
                } else {
                    craftAmount = firstToCraft.slots().size();
                }

                if (!ccc.planJob(
                        craftKey,
                        craftAmount,
                        CalculationStrategy.CRAFT_LESS)) {
                    ccc.setValidMenu(false);
                    return;
                }

                // 리플렉션으로 private 필드 설정
                try {
                    var queueField = CraftConfirmMenu.class.getDeclaredField("autoCraftingQueue");
                    queueField.setAccessible(true);
                    queueField.set(ccc, subsequentCrafts);

                    var slotsField = CraftConfirmMenu.class.getDeclaredField("requestedSlots");
                    slotsField.setAccessible(true);
                    slotsField.set(ccc, firstToCraft.slots());
                } catch (Exception e) {
                    AELog.info(e);
                }

                ccc.broadcastChanges();
            }
        } catch (Throwable e) {
            AELog.info(e);
        }
    }

    /**
     * AutoCraftEntry에서 VirtualFluidItem이면 AEFluidKey로,
     * 아니면 원본 AEItemKey를 반환합니다.
     */
    private static AEKey resolveAutoCraftKey(ICraftingGridMenu.AutoCraftEntry entry) {
        if (entry.what() != null && entry.what().getItem() == AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
            var itemStack = entry.what().toStack();
            VirtualFluid vf = itemStack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
            if (vf != null && !vf.fluid().isEmpty()) {
                AEFluidKey fluidKey = AEFluidKey.of(vf.fluid());
                if (fluidKey != null) {
                    return fluidKey;
                }
            }
        }
        return entry.what();
    }
}
