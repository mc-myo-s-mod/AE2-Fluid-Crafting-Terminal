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
import appeng.menu.me.common.MEStorageMenu;
import appeng.util.prioritylist.IPartitionList;
import me.myogoo.ae2fct.api.IMEStorageMenu;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class FluidCraftingHelper {
    private static final ThreadLocal<Boolean> FLUID_CRAFTING_ENABLED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<ResourceLocation> FLUID_CRAFTING_RECIPE_ID = new ThreadLocal<>();
    private static final ThreadLocal<MEStorageMenu> FLUID_CRAFTING_MENU = new ThreadLocal<>();

    public static boolean isFluidCraftingEnabled() {
        return FLUID_CRAFTING_ENABLED.get() && !isCurrentRecipeBlacklisted();
    }

    public static void setFluidCraftingEnabled(boolean enabled) {
        setFluidCraftingEnabled(enabled, null);
    }

    public static void setFluidCraftingEnabled(boolean enabled, @Nullable ResourceLocation recipeId) {
        setFluidCraftingEnabled(enabled, recipeId, null);
    }

    public static void setFluidCraftingEnabled(boolean enabled, @Nullable ResourceLocation recipeId,
            @Nullable MEStorageMenu menu) {
        FLUID_CRAFTING_ENABLED.set(enabled);
        if (recipeId == null) {
            FLUID_CRAFTING_RECIPE_ID.remove();
        } else {
            FLUID_CRAFTING_RECIPE_ID.set(recipeId);
        }
        if (menu == null) {
            FLUID_CRAFTING_MENU.remove();
        } else {
            FLUID_CRAFTING_MENU.set(menu);
        }
    }

    public static boolean isCurrentRecipeBlacklisted() {
        return isVirtualFluidRecipeBlacklisted(FLUID_CRAFTING_MENU.get(), FLUID_CRAFTING_RECIPE_ID.get());
    }

    public static boolean isRecipePreviewAllowed() {
        return FLUID_CRAFTING_MENU.get() != null && FLUID_CRAFTING_RECIPE_ID.get() != null
                && !isCurrentRecipeBlacklisted();
    }

    public static void clearFluidCraftingEnabled() {
        FLUID_CRAFTING_ENABLED.remove();
        FLUID_CRAFTING_RECIPE_ID.remove();
        FLUID_CRAFTING_MENU.remove();
    }

    public static boolean isVirtualFluidRecipeBlacklisted(@Nullable MEStorageMenu menu,
            @Nullable ResourceLocation recipeId) {
        if (recipeId == null) {
            return false;
        }

        if (menu instanceof IMEStorageMenu ae2fctMenu && menu.getPlayer().level().isClientSide()) {
            return ae2fctMenu.ae2fct$isVirtualFluidRecipeBlacklisted(recipeId);
        }

        return FluidCraftingConfig.isVirtualFluidRecipeBlacklisted(recipeId);
    }

    public static <T> T withVirtualFluidRecipePolicy(@Nullable MEStorageMenu menu,
            @Nullable ResourceLocation recipeId,
            Supplier<T> action) {
        Boolean previousEnabled = FLUID_CRAFTING_ENABLED.get();
        ResourceLocation previousRecipeId = FLUID_CRAFTING_RECIPE_ID.get();
        MEStorageMenu previousMenu = FLUID_CRAFTING_MENU.get();

        FLUID_CRAFTING_RECIPE_ID.set(recipeId);
        if (menu == null) {
            FLUID_CRAFTING_MENU.remove();
        } else {
            FLUID_CRAFTING_MENU.set(menu);
        }

        try {
            return action.get();
        } finally {
            FLUID_CRAFTING_ENABLED.set(previousEnabled);
            if (previousRecipeId == null) {
                FLUID_CRAFTING_RECIPE_ID.remove();
            } else {
                FLUID_CRAFTING_RECIPE_ID.set(previousRecipeId);
            }
            if (previousMenu == null) {
                FLUID_CRAFTING_MENU.remove();
            } else {
                FLUID_CRAFTING_MENU.set(previousMenu);
            }
        }
    }

    public static boolean matchesCraftingRecipeWithVirtualFluidPolicy(@Nullable MEStorageMenu menu,
            RecipeHolder<CraftingRecipe> recipe,
            CraftingInput input,
            Level level) {
        return matchesCraftingRecipeWithVirtualFluidPolicy(menu, recipe.id(), recipe.value(), input, level);
    }

    public static boolean matchesCraftingRecipeWithVirtualFluidPolicy(@Nullable MEStorageMenu menu,
            ResourceLocation recipeId,
            CraftingRecipe recipe,
            CraftingInput input,
            Level level) {
        if (!containsVirtualFluidItem(input)) {
            return recipe.matches(input, level);
        }

        // Outside a terminal the client has no synchronized server policy (e.g. pattern tooltips).
        ResourceLocation policyRecipeId = level.isClientSide() && menu == null ? null : recipeId;
        if (isVirtualFluidRecipeBlacklisted(menu, policyRecipeId)) {
            return false;
        }

        return withVirtualFluidRecipePolicy(menu, policyRecipeId,
                () -> recipe.matches(input, level));
    }

    public static Optional<RecipeHolder<CraftingRecipe>> findCraftingRecipeWithVirtualFluidPolicy(
            @Nullable MEStorageMenu menu,
            CraftingInput input,
            Level level) {
        return level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> matchesCraftingRecipeWithVirtualFluidPolicy(menu, recipe, input, level))
                .findFirst();
    }

    public static boolean isVirtualFluidItem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
    }

    public static boolean containsVirtualFluidItem(CraftingInput input) {
        return input.items().stream().anyMatch(FluidCraftingHelper::isVirtualFluidItem);
    }

    public static boolean containsVirtualFluidItem(RecipeInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            if (isVirtualFluidItem(input.getItem(slot))) {
                return true;
            }
        }
        return false;
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
        if (!isFluidCraftingEnabled()) {
            return List.of();
        }

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
        if (!isFluidCraftingEnabled()) {
            return Optional.empty();
        }

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
        if (!isRecipePreviewAllowed()) {
            return false;
        }

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
        if (!isRecipePreviewAllowed()) {
            return false;
        }

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
