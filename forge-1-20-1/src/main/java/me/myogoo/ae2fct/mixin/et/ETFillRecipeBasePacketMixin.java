package me.myogoo.ae2fct.mixin.et;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.util.prioritylist.IPartitionList;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Pseudo
@Mixin(targets = "me.myogoo.extendedterminal.network.serverbound.FillRecipeBasePacket", remap = false)
public abstract class ETFillRecipeBasePacketMixin {
    @Inject(method = "findBestMatchingItemStack", at = @At("RETURN"), cancellable = true)
    private void ae2fct$optimizedFindBestMatchingItemStack(Ingredient ingredient, IPartitionList filter,
            KeyCounter storage, CallbackInfoReturnable<List<AEItemKey>> cir) {
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
    private void ae2fct$checkFluidCraftableKey(Ingredient ingredient, ICraftingService craftingService,
            CallbackInfoReturnable<Optional<AEItemKey>> cir) {
        if (!FluidCraftingTerminalIntegration.isFillGridContextEnabled() || cir.getReturnValue().isPresent()) {
            return;
        }

        FluidCraftingTerminalIntegration.findCraftableFluidItemKey(ingredient, craftingService, cir.getReturnValue())
                .ifPresent(vKey -> cir.setReturnValue(Optional.of(vKey)));
    }
}
