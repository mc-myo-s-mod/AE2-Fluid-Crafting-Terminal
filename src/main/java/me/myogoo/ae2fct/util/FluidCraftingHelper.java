package me.myogoo.ae2fct.util;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.menu.me.common.IClientRepo;
import appeng.util.prioritylist.IPartitionList;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.*;

public class FluidCraftingHelper {
    private static final ThreadLocal<Boolean> FLUID_CRAFTING_ENABLED = ThreadLocal.withInitial(() -> false);

    public static boolean isFluidCraftingEnabled() {
        return FLUID_CRAFTING_ENABLED.get();
    }

    public static void setFluidCraftingEnabled(boolean enabled) {
        FLUID_CRAFTING_ENABLED.set(enabled);
    }

    public static void clearFluidCraftingEnabled() {
        FLUID_CRAFTING_ENABLED.remove();
    }

    public static boolean isVirtualFluidItem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
    }

    /**
     * Extracts unique fluids from the items accepted by an ingredient.
     */
    public static Set<Fluid> getFluidsFromIngredient(Ingredient ingredient) {
        Set<Fluid> fluids = new HashSet<>();
        for (ItemStack stack : ingredient.getItems()) {
            FluidUtil.getFluidContained(stack).ifPresent(fs -> {
                if (!fs.isEmpty()) {
                    fluids.add(fs.getFluid());
                }
            });
            // Fallback for vanilla buckets if needed, though FluidUtil usually handles them
            if (stack.getItem() instanceof BucketItem bucketItem) {
                Fluid f = BuiltInRegistries.FLUID.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                if (f != null && f != Fluids.EMPTY) {
                    fluids.add(f);
                }
            }
        }
        return fluids;
    }

    /**
     * Finds fluid item keys from storage matching the ingredient.
     */
    public static List<AEItemKey> findBestMatchingFluidItemKeys(Ingredient ingredient, IPartitionList filter,
            KeyCounter storage) {
        Set<AEItemKey> enhancedResults = new LinkedHashSet<>();
        Set<Fluid> fluids = getFluidsFromIngredient(ingredient);

        for (Fluid fluid : fluids) {
            AEFluidKey fluidKey = AEFluidKey.of(fluid);
            if (fluidKey != null && storage.get(fluidKey) >= 1000) {
                AEItemKey vKey = AEItemKey.of(VirtualFluidItem.createItemStack(fluid));
                if (vKey != null && (filter == null || filter.isListed(vKey))) {
                    enhancedResults.add(vKey);
                }
            }
        }
        return new ArrayList<>(enhancedResults);
    }

    /**
     * Checks if any fluid in the ingredient is craftable and returns its VirtualFluidItem key.
     */
    public static Optional<AEItemKey> findCraftableFluidItemKey(Ingredient ingredient,
            ICraftingService craftingService) {
        Set<Fluid> fluids = getFluidsFromIngredient(ingredient);

        for (Fluid fluid : fluids) {
            AEFluidKey fluidKey = AEFluidKey.of(fluid);
            if (fluidKey != null) {
                var craftable = craftingService.getFuzzyCraftable(fluidKey, key -> key.equals(fluidKey));
                if (craftable != null) {
                    AEItemKey vKey = AEItemKey.of(VirtualFluidItem.createItemStack(fluid));
                    if (vKey != null) {
                        return Optional.of(vKey);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public record FluidAvailability(boolean available, boolean craftable) {
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

    /**
     * Checks if any fluid in the ingredient is available or craftable in the client repo.
     */
    public static FluidAvailability checkFluidAvailabilityInClientRepo(Ingredient ingredient, IClientRepo clientRepo) {
        if (clientRepo == null)
            return new FluidAvailability(false, false);

        boolean available = hasAvailableFluidInClientRepo(ingredient, clientRepo, 1000);
        boolean craftable = isFluidCraftableInClientRepo(ingredient, clientRepo);
        return new FluidAvailability(available, craftable);
    }

    /**
     * Attempts to resolve fluid from an ingredient's accepted items,
     * and if enough fluid is available in MEStorage, extracts it
     * and returns a "simulated bucket" representing the item.
     */
    public static ItemStack tryExtractFluidForIngredient(Ingredient ingredient, MEStorage networkStorage,
            IEnergySource energy, IActionSource src) {
        if (networkStorage == null)
            return ItemStack.EMPTY;

        for (ItemStack stack : ingredient.getItems()) {
            FluidStack requiredFluid = FluidStack.EMPTY;
            Optional<FluidStack> fluidOpt = FluidUtil.getFluidContained(stack);
            if (fluidOpt.isPresent() && !fluidOpt.get().isEmpty()) {
                requiredFluid = fluidOpt.get();
            } else if (stack.getItem() instanceof BucketItem bucketItem) {
                // Vanilla bucket fallback
                // we have to check if it's not the empty bucket
                Fluid f = BuiltInRegistries.FLUID.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                if (f != null && f != Fluids.EMPTY) {
                    requiredFluid = new FluidStack(f, 1000);
                }
            }

            if (!requiredFluid.isEmpty()) {
                AEFluidKey fluidKey = AEFluidKey.of(requiredFluid);
                if (fluidKey != null) {
                    long simulated = StorageHelper.poweredExtraction(energy, networkStorage, fluidKey,
                            requiredFluid.getAmount(), src, Actionable.SIMULATE);
                    if (simulated >= requiredFluid.getAmount()) {
                        long extracted = StorageHelper.poweredExtraction(energy, networkStorage, fluidKey,
                                requiredFluid.getAmount(), src, Actionable.MODULATE);
                        if (extracted >= requiredFluid.getAmount()) {
                            ItemStack simBucket = stack.copy();
                            simBucket.setCount(1);
                            simBucket.set(AE2FCTDataComponent.VIRTUAL_FLUID,
                                    new VirtualFluid(requiredFluid, requiredFluid.getAmount()));
                            return simBucket;
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack tryExtractFluidForTemplate(ItemStack providedTemplate, MEStorage networkStorage,
            IEnergySource energy, IActionSource src) {
        if (networkStorage == null)
            return ItemStack.EMPTY;

        FluidStack requiredFluid = FluidStack.EMPTY;
        Optional<FluidStack> fluidOpt = FluidUtil.getFluidContained(providedTemplate);
        if (fluidOpt.isPresent() && !fluidOpt.get().isEmpty()) {
            requiredFluid = fluidOpt.get();
        } else if (providedTemplate.getItem() instanceof BucketItem bucketItem) {
            Fluid f = BuiltInRegistries.FLUID.get(BuiltInRegistries.ITEM.getKey(providedTemplate.getItem()));
            if (f != null && f != Fluids.EMPTY) {
                requiredFluid = new FluidStack(f, 1000);
            }
        }

        if (!requiredFluid.isEmpty()) {
            AEFluidKey fluidKey = AEFluidKey.of(requiredFluid);
            if (fluidKey != null) {
                long simulated = StorageHelper.poweredExtraction(energy, networkStorage, fluidKey,
                        requiredFluid.getAmount(), src, Actionable.SIMULATE);
                if (simulated >= requiredFluid.getAmount()) {
                    long extracted = StorageHelper.poweredExtraction(energy, networkStorage, fluidKey,
                            requiredFluid.getAmount(), src, Actionable.MODULATE);
                    if (extracted >= requiredFluid.getAmount()) {
                        ItemStack simBucket = providedTemplate.copy();
                        simBucket.setCount(1);
                        simBucket.set(AE2FCTDataComponent.VIRTUAL_FLUID,
                                new VirtualFluid(requiredFluid, requiredFluid.getAmount()));
                        return simBucket;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
