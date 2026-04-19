package me.myogoo.ae2fct.mixin;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.sync.packets.FillCraftingGridFromRecipePacket;
import appeng.helpers.IMenuCraftingPacket;
import appeng.menu.me.common.MEStorageMenu;
import appeng.util.prioritylist.IPartitionList;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(value = FillCraftingGridFromRecipePacket.class, remap = false)
public class FillCraftingGridFromRecipePacketMixin {
    @WrapMethod(method = "serverPacketData")
    private void ae2fct$withFluidCraftingState(ServerPlayer player, Operation<Void> original) {
        FluidCraftingTerminalIntegration.beginFillGridContext(player,
                menu -> menu instanceof IMenuCraftingPacket
                        && FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(menu));
        try {
            original.call(player);
        } finally {
            FluidCraftingTerminalIntegration.endFillGridContext();
        }
    }

    @Inject(method = "findBestMatchingItemStack", at = @At("RETURN"), cancellable = true)
    private void optimizedFindBestMatchingItemStack(Ingredient ingredient, IPartitionList filter, KeyCounter storage,
            CallbackInfoReturnable<List<AEItemKey>> cir) {
        if (!FluidCraftingTerminalIntegration.isFillGridContextEnabled()) {
            return;
        }

        List<AEItemKey> result = cir.getReturnValue();
        List<AEItemKey> updated = FluidCraftingTerminalIntegration.mergeBestMatchingFluidItemKeys(ingredient, filter,
                storage, result);
        if (updated != result) {
            cir.setReturnValue(updated);
        }
    }

    @Inject(method = "findCraftableKey", at = @At("RETURN"), cancellable = true)
    private void checkFluidCraftableKey(Ingredient ingredient, ICraftingService craftingService,
            CallbackInfoReturnable<Optional<AEItemKey>> cir) {
        if (!FluidCraftingTerminalIntegration.isFillGridContextEnabled() || cir.getReturnValue().isPresent()) {
            return;
        }

        FluidCraftingTerminalIntegration.findCraftableFluidItemKey(ingredient, craftingService, cir.getReturnValue())
                .ifPresent(key -> cir.setReturnValue(Optional.of(key)));
    }
}
