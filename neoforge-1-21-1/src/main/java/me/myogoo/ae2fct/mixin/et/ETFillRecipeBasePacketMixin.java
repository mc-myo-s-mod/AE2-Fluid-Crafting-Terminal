package me.myogoo.ae2fct.mixin.et;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.util.prioritylist.IPartitionList;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Pseudo
@Mixin(targets = "me.myogoo.extendedterminal.network.serverbound.FillRecipeBasePacket", remap = false)
public abstract class ETFillRecipeBasePacketMixin {

    @Inject(method = "findBestMatchingItemStack", at = @At("RETURN"), cancellable = true)
    private void ae2fct$optimizedFindBestMatchingItemStack(Ingredient ingredient, IPartitionList filter, KeyCounter storage,
            CallbackInfoReturnable<List<AEItemKey>> cir) {
        if (!FluidCraftingHelper.isFluidCraftingEnabled()) {
            return;
        }

        List<AEItemKey> fluidResults = FluidCraftingHelper.findBestMatchingFluidItemKeys(ingredient, filter, storage);
        if (fluidResults.isEmpty()) {
            return;
        }

        List<AEItemKey> originalResults = cir.getReturnValue();
        if (originalResults == null || originalResults.isEmpty()) {
            cir.setReturnValue(fluidResults);
            return;
        }

        Set<AEItemKey> enhancedResults = new LinkedHashSet<>(fluidResults);
        enhancedResults.addAll(originalResults);
        cir.setReturnValue(new ArrayList<>(enhancedResults));
    }

    @Inject(method = "findCraftableKey", at = @At("RETURN"), cancellable = true)
    private void ae2fct$checkFluidCraftableKey(Ingredient ingredient, ICraftingService craftingService,
            CallbackInfoReturnable<Optional<AEItemKey>> cir) {
        if (!FluidCraftingHelper.isFluidCraftingEnabled() || cir.getReturnValue().isPresent()) {
            return;
        }

        FluidCraftingHelper.findCraftableFluidItemKey(ingredient, craftingService)
                .ifPresent(vKey -> cir.setReturnValue(Optional.of(vKey)));
    }
}
