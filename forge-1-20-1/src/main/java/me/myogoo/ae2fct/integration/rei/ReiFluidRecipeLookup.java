package me.myogoo.ae2fct.integration.rei;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.client.gui.AEBaseScreen;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import me.shedaniel.math.Point;
import me.shedaniel.math.impl.PointHelper;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.impl.client.ClientHelperImpl.LegacyWrapperViewSearchBuilder;
import me.shedaniel.rei.impl.display.DisplaySpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ReiFluidRecipeLookup {
    private ReiFluidRecipeLookup() {
    }

    public static Optional<Boolean> handleKeyPressed(int keyCode, int scanCode) {
        Optional<LookupMode> mode = getLookupMode(keyCode, scanCode);
        if (mode.isEmpty()) {
            return Optional.empty();
        }

        return findTargetUnderMouse().flatMap(target -> openTarget(target, mode.get()));
    }

    public static Optional<Boolean> handleEntryKeyPressed(EntryStack<?> entry, int keyCode, int scanCode) {
        Optional<LookupMode> mode = getLookupMode(keyCode, scanCode);
        if (mode.isEmpty() || entry.isEmpty() || !(entry.getValue() instanceof ItemStack stack)) {
            return Optional.empty();
        }

        return getVirtualFluidTarget(stack).flatMap(target -> openTarget(target, mode.get()));
    }

    private static Optional<LookupMode> getLookupMode(int keyCode, int scanCode) {
        if (ConfigObject.getInstance().getRecipeKeybind().matchesKey(keyCode, scanCode)) {
            return Optional.of(LookupMode.RECIPES);
        }
        if (ConfigObject.getInstance().getUsageKeybind().matchesKey(keyCode, scanCode)) {
            return Optional.of(LookupMode.USAGES);
        }
        return Optional.empty();
    }

    private static Optional<Boolean> openTarget(FluidTarget target, LookupMode mode) {
        List<EntryStack<?>> entries = createTargetEntries(target);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(createViewSearchBuilder(entries, mode).open());
    }

    public static ViewSearchBuilder createViewSearchBuilder(List<EntryStack<?>> entries, boolean recipes) {
        return createViewSearchBuilder(entries, recipes ? LookupMode.RECIPES : LookupMode.USAGES);
    }

    private static ViewSearchBuilder createViewSearchBuilder(List<EntryStack<?>> entries, LookupMode mode) {
        var builder = new LegacyWrapperViewSearchBuilder(buildUnionMap(entries, mode));
        if (!entries.isEmpty()) {
            // ponytail: REI's legacy builder highlights one focus; both notices avoid its null-output bug.
            builder.addInputNotice(entries.get(0));
            builder.addOutputNotice(entries.get(0));
        }
        return builder;
    }

    private static Map<DisplayCategory<?>, List<DisplaySpec>> buildUnionMap(List<EntryStack<?>> entries,
            LookupMode mode) {
        Map<DisplayCategory<?>, Set<DisplaySpec>> merged = new LinkedHashMap<>();
        // REI 12's cached lookup only checks the first focus, so query each target separately.
        for (EntryStack<?> entry : entries) {
            ViewSearchBuilder single = ViewSearchBuilder.builder();
            if (mode == LookupMode.RECIPES) {
                single.addRecipesFor(entry);
            } else {
                single.addUsagesFor(entry);
            }

            for (var category : single.buildMapInternal().entrySet()) {
                merged.computeIfAbsent(category.getKey(), ignored -> new LinkedHashSet<>()).addAll(category.getValue());
            }
        }

        Map<DisplayCategory<?>, List<DisplaySpec>> result = new LinkedHashMap<>();
        for (var entry : merged.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    private static Optional<FluidTarget> findTargetUnderMouse() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AEBaseScreen<?> aeScreen) {
            var window = minecraft.getWindow();
            double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
            double mouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
            Optional<FluidTarget> target = findAeFluidTarget(aeScreen, mouseX, mouseY);
            if (target.isPresent()) {
                return target;
            }
        }

        return findFocusedVirtualFluidTarget(minecraft.screen, PointHelper.ofMouse());
    }

    private static Optional<FluidTarget> findAeFluidTarget(AEBaseScreen<?> aeScreen, double mouseX, double mouseY) {
        var stackWithBounds = aeScreen.getStackUnderMouse(mouseX, mouseY);
        if (stackWithBounds == null) {
            return Optional.empty();
        }

        Object what = stackWithBounds.stack().what();
        if (what instanceof AEFluidKey fluidKey) {
            return Optional.of(new FluidTarget(fluidKey.toStack((int) AEFluidKey.AMOUNT_BUCKET), false));
        }
        if (what instanceof AEItemKey itemKey) {
            return getVirtualFluidTarget(itemKey.toStack());
        }
        return Optional.empty();
    }

    private static Optional<FluidTarget> findFocusedVirtualFluidTarget(@Nullable Screen screen, Point mouse) {
        if (screen == null) {
            return Optional.empty();
        }

        EntryStack<?> focused = ScreenRegistry.getInstance().getFocusedStack(screen, mouse);
        if (focused == null || focused.isEmpty() || !(focused.getValue() instanceof ItemStack stack)) {
            return Optional.empty();
        }
        return getVirtualFluidTarget(stack);
    }

    public static Optional<FluidTarget> getVirtualFluidTarget(ItemStack stack) {
        if (!FluidCraftingHelper.isVirtualFluidItem(stack)) {
            return Optional.empty();
        }

        var virtualFluid = VirtualFluidStorage.get(stack);
        if (virtualFluid == null || virtualFluid.fluid().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FluidTarget(virtualFluid.fluid(), true));
    }

    public static List<EntryStack<?>> createTargetEntries(FluidTarget target) {
        List<EntryStack<?>> entries = new ArrayList<>();
        boolean showBucketRecipes = target.virtualFluid()
                ? FluidCraftingConfig.showBucketRecipesForVirtualFluids()
                : FluidCraftingConfig.showBucketRecipesForAe2FluidKeys();
        boolean showFluidRecipes = !target.virtualFluid()
                || FluidCraftingConfig.showFluidRecipesForVirtualFluids();

        if (showBucketRecipes) {
            ItemStack bucket = FluidUtil.getFilledBucket(target.fluidStack());
            if (!bucket.isEmpty()) {
                entries.add(EntryStacks.of(bucket));
            }
        }
        if (showFluidRecipes) {
            entries.add(EntryStacks.of(toReiFluidStack(target.fluidStack())));
        }
        return entries;
    }

    public static dev.architectury.fluid.FluidStack toReiFluidStack(FluidStack fluidStack) {
        return dev.architectury.fluid.FluidStack.create(
                fluidStack.getFluid(),
                fluidStack.getAmount(),
                fluidStack.getTag() == null ? null : fluidStack.getTag().copy());
    }

    public record FluidTarget(FluidStack fluidStack, boolean virtualFluid) {
    }

    private enum LookupMode {
        RECIPES,
        USAGES
    }

}
