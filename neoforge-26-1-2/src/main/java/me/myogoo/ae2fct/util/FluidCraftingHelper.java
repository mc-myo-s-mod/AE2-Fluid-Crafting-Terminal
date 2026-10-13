package me.myogoo.ae2fct.util;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.menu.me.common.IClientRepo;
import appeng.util.prioritylist.IPartitionList;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;

public class FluidCraftingHelper {
    private static final ThreadLocal<Boolean> FLUID_CRAFTING_ENABLED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<ResourceKey<Recipe<?>>> FLUID_CRAFTING_RECIPE_ID = new ThreadLocal<>();
    private static final ThreadLocal<Level> FLUID_CRAFTING_LEVEL = new ThreadLocal<>();

    public static boolean isFluidCraftingEnabled() {
        return FLUID_CRAFTING_ENABLED.get() && !isCurrentRecipeBlacklisted();
    }

    public static void setFluidCraftingEnabled(boolean enabled) {
        setFluidCraftingEnabled(enabled, null, null);
    }

    public static void setFluidCraftingEnabled(boolean enabled, @Nullable ResourceKey<Recipe<?>> recipeId) {
        setFluidCraftingEnabled(enabled, recipeId, null);
    }

    public static void setFluidCraftingEnabled(boolean enabled, @Nullable ResourceKey<Recipe<?>> recipeId,
            @Nullable Level level) {
        FLUID_CRAFTING_ENABLED.set(enabled);
        if (recipeId == null) {
            FLUID_CRAFTING_RECIPE_ID.remove();
        } else {
            FLUID_CRAFTING_RECIPE_ID.set(recipeId);
        }
        if (level == null) {
            FLUID_CRAFTING_LEVEL.remove();
        } else {
            FLUID_CRAFTING_LEVEL.set(level);
        }
    }

    public static boolean isCurrentRecipeBlacklisted() {
        return FluidCraftingConfig.isVirtualFluidRecipeBlacklisted(FLUID_CRAFTING_RECIPE_ID.get());
    }

    public static void clearFluidCraftingEnabled() {
        FLUID_CRAFTING_ENABLED.remove();
        FLUID_CRAFTING_RECIPE_ID.remove();
        FLUID_CRAFTING_LEVEL.remove();
    }

    public static boolean withFluidCraftingEnabled(boolean enabled, @Nullable ResourceKey<Recipe<?>> recipeId,
            @Nullable Level level, BooleanSupplier action) {
        boolean previousEnabled = FLUID_CRAFTING_ENABLED.get();
        ResourceKey<Recipe<?>> previousRecipeId = FLUID_CRAFTING_RECIPE_ID.get();
        Level previousLevel = FLUID_CRAFTING_LEVEL.get();
        setFluidCraftingEnabled(enabled, recipeId, level);
        try {
            return action.getAsBoolean();
        } finally {
            setFluidCraftingEnabled(previousEnabled, previousRecipeId, previousLevel);
        }
    }

    public static boolean isVirtualFluidItem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
    }

    /**
     * Extracts unique fluids from the items accepted by an ingredient.
     */
    public static Set<Fluid> getFluidsFromIngredient(Ingredient ingredient, Level level) {
        Set<Fluid> fluids = new HashSet<>();
        for (ItemStack stack : getStacksFromIngredient(ingredient, level)) {
            FluidStack contained = getContainedFluid(stack);
            if (!contained.isEmpty()) {
                fluids.add(contained.getFluid());
            }
        }
        fluids.removeIf(fluid -> !ingredient.test(VirtualFluidItem.createItemStack(fluid)));
        return fluids;
    }

    /**
     * Finds fluid item keys from storage matching the ingredient.
     */
    public static List<AEItemKey> findBestMatchingFluidItemKeys(Ingredient ingredient, IPartitionList filter,
            KeyCounter storage) {
        Set<AEItemKey> enhancedResults = new LinkedHashSet<>();
        Level level = FLUID_CRAFTING_LEVEL.get();
        if (level == null) {
            return List.of();
        }

        Set<Fluid> fluids = getFluidsFromIngredient(ingredient, level);

        for (Fluid fluid : fluids) {
            AEFluidKey fluidKey = AEFluidKey.of(fluid);
            if (fluidKey != null && storage.get(fluidKey) >= FluidType.BUCKET_VOLUME) {
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
        Level level = FLUID_CRAFTING_LEVEL.get();
        if (level == null) {
            return Optional.empty();
        }

        Set<Fluid> fluids = getFluidsFromIngredient(ingredient, level);

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
            Level level, long requiredAmount) {
        return reserveAvailableFluidInClientRepo(ingredient, clientRepo, level, requiredAmount, null);
    }

    public static boolean reserveAvailableFluidInClientRepo(Ingredient ingredient, IClientRepo clientRepo,
            Level level, long requiredAmount, java.util.Map<Object, Integer> reservedAmounts) {
        if (!VirtualFluidRecipePolicy.isPreviewAllowed()) {
            return false;
        }

        if (clientRepo == null) {
            return false;
        }

        Set<Fluid> fluids = getFluidsFromIngredient(ingredient, level);
        if (fluids.isEmpty()) {
            return false;
        }

        for (var entry : clientRepo.getAllEntries()) {
            if (!(entry.getWhat() instanceof AEFluidKey entryFluidKey)) {
                continue;
            }

            if (entry.getStoredAmount() - (reservedAmounts == null ? 0 : reservedAmounts.getOrDefault(entryFluidKey, 0)) < requiredAmount) {
                continue;
            }

            for (Fluid fluid : fluids) {
                AEFluidKey fluidKey = AEFluidKey.of(fluid);
                if (fluidKey != null && entryFluidKey.equals(fluidKey)) {
                    if (reservedAmounts != null) reservedAmounts.merge(entryFluidKey, Math.toIntExact(requiredAmount), Integer::sum);
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isFluidCraftableInClientRepo(Ingredient ingredient, IClientRepo clientRepo, Level level) {
        if (!VirtualFluidRecipePolicy.isPreviewAllowed()) {
            return false;
        }

        if (clientRepo == null) {
            return false;
        }

        Set<Fluid> fluids = getFluidsFromIngredient(ingredient, level);
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
    public static FluidAvailability checkFluidAvailabilityInClientRepo(Ingredient ingredient, IClientRepo clientRepo,
            Level level) {
        if (clientRepo == null)
            return new FluidAvailability(false, false);

        boolean available = hasAvailableFluidInClientRepo(ingredient, clientRepo, level, FluidType.BUCKET_VOLUME);
        boolean craftable = isFluidCraftableInClientRepo(ingredient, clientRepo, level);
        return new FluidAvailability(available, craftable);
    }

    /**
     * @deprecated Use {@link FluidStorageTransfer#extractToItem} with an exact fluid key.
     * A partial extraction is returned on the template with its actual amount; callers must inspect it.
     */
    @Deprecated(forRemoval = true)
    public static ItemStack tryExtractFluidForIngredient(Ingredient ingredient, MEStorage networkStorage,
            IEnergySource energy, IActionSource src, Level level) {
        if (networkStorage == null)
            return ItemStack.EMPTY;

        for (ItemStack stack : getStacksFromIngredient(ingredient, level)) {
            ItemStack extracted = tryExtractFluidForTemplate(stack, networkStorage, energy, src);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * @deprecated Use {@link FluidStorageTransfer#extractToItem} with an exact fluid key.
     * Preserves template data and returns partial fluid rather than discarding an incomplete extraction.
     */
    @Deprecated(forRemoval = true)
    public static ItemStack tryExtractFluidForTemplate(ItemStack providedTemplate, MEStorage networkStorage,
            IEnergySource energy, IActionSource src) {
        if (networkStorage == null)
            return ItemStack.EMPTY;

        FluidStack requiredFluid = getContainedFluid(providedTemplate);
        if (!requiredFluid.isEmpty()) {
            AEFluidKey fluidKey = AEFluidKey.of(requiredFluid);
            if (fluidKey != null) {
                ItemStack extracted = FluidStorageTransfer.extractToItem(networkStorage, energy, fluidKey,
                        requiredFluid.getAmount(), src);
                if (!extracted.isEmpty()) {
                    ItemStack simBucket = providedTemplate.copy();
                    simBucket.setCount(1);
                    simBucket.set(AE2FCTDataComponent.VIRTUAL_FLUID,
                            new VirtualFluid(extracted.get(AE2FCTDataComponent.VIRTUAL_FLUID).fluid(),
                                    requiredFluid.getAmount()));
                    return simBucket;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<ItemStack> getStacksFromIngredient(Ingredient ingredient, Level level) {
        return ingredient.display().resolveForStacks(SlotDisplayContext.fromLevel(level));
    }

    private static FluidStack getContainedFluid(ItemStack stack) {
        FluidStack fluid = FluidUtil.getFirstStackContained(stack);
        if (!fluid.isEmpty()) {
            return fluid;
        }

        if (stack.getItem() instanceof BucketItem bucketItem) {
            Fluid f = bucketItem.getContent();
            if (f != null && f != Fluids.EMPTY) {
                return new FluidStack(f, FluidType.BUCKET_VOLUME);
            }
        }
        return FluidStack.EMPTY;
    }
}
