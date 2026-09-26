package me.myogoo.ae2fct.integration.rei;

import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReiFluidRecipeLookup {
    private ReiFluidRecipeLookup() {
    }

    public static Optional<Boolean> openVirtualFluidTarget(EntryStack<?> entry, boolean showRecipes) {
        if (entry == null || entry.isEmpty() || !(entry.getValue() instanceof ItemStack stack)
                || !FluidCraftingHelper.isVirtualFluidItem(stack)) {
            return Optional.empty();
        }

        var virtualFluid = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
        return virtualFluid == null || virtualFluid.fluid().isEmpty()
                ? Optional.empty()
                : openFluidTarget(virtualFluid.fluid(), true, showRecipes);
    }

    public static Optional<Boolean> openFluidTarget(FluidStack fluid, boolean virtualFluid, boolean showRecipes) {
        List<EntryStack<?>> entries = new ArrayList<>();
        boolean showBucket = virtualFluid
                ? FluidCraftingConfig.showBucketRecipesForVirtualFluids()
                : FluidCraftingConfig.showBucketRecipesForAe2FluidKeys();
        boolean showFluid = !virtualFluid || FluidCraftingConfig.showFluidRecipesForVirtualFluids();

        if (showBucket) {
            ItemStack bucket = fluid.getFluidType().getBucket(fluid);
            if (!bucket.isEmpty()) {
                entries.add(EntryStacks.of(bucket));
            }
        }
        if (showFluid) {
            entries.add(EntryStacks.of(dev.architectury.fluid.FluidStack.create(
                    fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch())));
        }
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        ViewSearchBuilder builder = ViewSearchBuilder.builder();
        for (EntryStack<?> entry : entries) {
            if (showRecipes) {
                builder.addRecipesFor(entry);
            } else {
                builder.addUsagesFor(entry);
            }
        }
        return Optional.of(builder.open());
    }
}
