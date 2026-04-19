package me.myogoo.ae2fct.util;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.menu.me.common.IClientRepo;
import appeng.util.prioritylist.IPartitionList;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class FluidCraftingHelper {
    private FluidCraftingHelper() {
    }

    public static boolean isVirtualFluidItem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
    }

    public static Set<Fluid> getFluidsFromIngredient(Ingredient ingredient) {
        Set<Fluid> fluids = new HashSet<>();
        for (ItemStack stack : ingredient.getItems()) {
            FluidUtil.getFluidContained(stack).ifPresent(fluidStack -> {
                if (!fluidStack.isEmpty()) {
                    fluids.add(fluidStack.getFluid());
                }
            });
        }
        return fluids;
    }

    public static ArrayList<AEItemKey> findBestMatchingFluidItemKeys(Ingredient ingredient, IPartitionList filter,
            KeyCounter storage) {
        Set<AEItemKey> results = new LinkedHashSet<>();
        for (Fluid fluid : getFluidsFromIngredient(ingredient)) {
            AEFluidKey fluidKey = AEFluidKey.of(fluid);
            if (fluidKey == null || storage.get(fluidKey) < AEFluidKey.AMOUNT_BUCKET) {
                continue;
            }

            AEItemKey itemKey = AEItemKey.of(VirtualFluidItem.createItemStack(fluid));
            if (itemKey != null && (filter == null || filter.isListed(itemKey))) {
                results.add(itemKey);
            }
        }
        return new ArrayList<>(results);
    }

    public static Optional<AEItemKey> findCraftableFluidItemKey(Ingredient ingredient,
            ICraftingService craftingService) {
        for (Fluid fluid : getFluidsFromIngredient(ingredient)) {
            AEFluidKey fluidKey = AEFluidKey.of(fluid);
            if (fluidKey == null) {
                continue;
            }

            AEKey craftable = craftingService.getFuzzyCraftable(fluidKey, key -> key.equals(fluidKey));
            if (craftable != null) {
                AEItemKey itemKey = AEItemKey.of(VirtualFluidItem.createItemStack(fluid));
                if (itemKey != null) {
                    return Optional.of(itemKey);
                }
            }
        }
        return Optional.empty();
    }

    public static boolean hasAvailableFluidInClientRepo(Ingredient ingredient, IClientRepo clientRepo,
            long requiredAmount) {
        if (clientRepo == null) {
            return false;
        }

        Set<Fluid> fluids = getFluidsFromIngredient(ingredient);
        if (fluids.isEmpty()) {
            return false;
        }

        for (var entry : clientRepo.getAllEntries()) {
            if (!(entry.getWhat() instanceof AEFluidKey entryFluidKey)) {
                continue;
            }

            if (entry.getStoredAmount() < requiredAmount) {
                continue;
            }

            for (Fluid fluid : fluids) {
                AEFluidKey fluidKey = AEFluidKey.of(fluid);
                if (fluidKey != null && entryFluidKey.equals(fluidKey)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isFluidCraftableInClientRepo(Ingredient ingredient, IClientRepo clientRepo) {
        if (clientRepo == null) {
            return false;
        }

        Set<Fluid> fluids = getFluidsFromIngredient(ingredient);
        if (fluids.isEmpty()) {
            return false;
        }

        for (var entry : clientRepo.getAllEntries()) {
            if (!(entry.getWhat() instanceof AEFluidKey entryFluidKey)) {
                continue;
            }

            if (!entry.isCraftable() && entry.getRequestableAmount() <= 0) {
                continue;
            }

            for (Fluid fluid : fluids) {
                AEFluidKey fluidKey = AEFluidKey.of(fluid);
                if (fluidKey != null && entryFluidKey.equals(fluidKey)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static FluidAvailability checkFluidAvailabilityInClientRepo(Ingredient ingredient, IClientRepo clientRepo) {
        if (clientRepo == null) {
            return new FluidAvailability(false, false);
        }

        boolean available = hasAvailableFluidInClientRepo(ingredient, clientRepo, AEFluidKey.AMOUNT_BUCKET);
        boolean craftable = isFluidCraftableInClientRepo(ingredient, clientRepo);
        return new FluidAvailability(available, craftable);
    }

    public record FluidAvailability(boolean available, boolean craftable) {
    }
}
