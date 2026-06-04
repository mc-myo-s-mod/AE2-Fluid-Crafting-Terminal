package me.myogoo.ae2fct.mixin;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.network.serverbound.FillCraftingGridFromRecipePacket;
import appeng.menu.me.common.MEStorageMenu;
import appeng.util.prioritylist.IPartitionList;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = FillCraftingGridFromRecipePacket.class, remap = false)
public class FillCraftingGridFromRecipePacketMixin {
    @WrapMethod(method = "handleOnServer")
    private void ae2fct$withFluidCraftingState(ServerPlayer player, Operation<Void> original) {
        boolean enabled = false;
        if (player.containerMenu instanceof MEStorageMenu menu) {
            enabled = FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(menu);
        }

        FluidCraftingHelper.setFluidCraftingEnabled(enabled);
        try {
            original.call(player);
        } finally {
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }
    }

    @Inject(method = "findBestMatchingItemStack", at = @At("RETURN"), cancellable = true)
    private void optimizedFindBestMatchingItemStack(Ingredient ingredient, IPartitionList filter, KeyCounter storage,
            CallbackInfoReturnable<List<AEItemKey>> cir) {
        if (!FluidCraftingHelper.isFluidCraftingEnabled()) {
            return;
        }

        List<AEItemKey> fluidResults = FluidCraftingHelper.findBestMatchingFluidItemKeys(ingredient, filter, storage);

        if (!fluidResults.isEmpty()) {
            List<AEItemKey> originalResults = cir.getReturnValue();
            if (originalResults == null || originalResults.isEmpty()) {
                cir.setReturnValue(fluidResults);
            } else {
                Set<AEItemKey> enhancedResults = new LinkedHashSet<>(fluidResults);
                enhancedResults.addAll(originalResults);
                cir.setReturnValue(new ArrayList<>(enhancedResults));
            }
        }
    }

    /**
     * findCraftableKey가 아이템으로 craftable한 것을 찾지 못한 경우,
     * ingredient에 포함된 fluid가 craftable한지 확인하여 VirtualFluidItem의 AEItemKey를 반환합니다.
     */
    @Inject(method = "findCraftableKey", at = @At("RETURN"), cancellable = true)
    private void checkFluidCraftableKey(Ingredient ingredient, ICraftingService craftingService,
            CallbackInfoReturnable<Optional<AEItemKey>> cir) {
        if (!FluidCraftingHelper.isFluidCraftingEnabled()) {
            return;
        }

        if (cir.getReturnValue().isPresent()) {
            return; // 이미 아이템으로 craftable한 것을 찾았으면 추가 검사 불필요
        }

        FluidCraftingHelper.findCraftableFluidItemKey(ingredient, craftingService)
                .ifPresent(vKey -> cir.setReturnValue(Optional.of(vKey)));
    }
}
