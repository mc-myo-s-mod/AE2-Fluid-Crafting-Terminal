package me.myogoo.ae2fct.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.widgets.AECheckbox;
import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.myotus.api.MyotusAPI;
import me.myogoo.myotus.api.config.MyoConfigTab;
import me.myogoo.myotus.api.config.MyoConfigTabScreen;
import me.myogoo.myotus.client.gui.MyoIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class AE2FCTConfigTab implements MyoConfigTabScreen {
    private AECheckbox showBucketRecipesCheckbox;
    private AECheckbox showFluidRecipesCheckbox;

    public static void initialize() {
        MyotusAPI.configTabs().registerTerminalConfigTab(new MyoConfigTab(
                ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID, "virtual_fluid_recipe_lookup"),
                Component.translatable("gui.ae2fct.config.title"),
                MyoIcon.AE2FCT_CONFIG,
                "ae2fct.json",
                new AE2FCTConfigTab()
        ));
    }

    @Override
    public void buildTab(WidgetContainer widgets, AEBaseScreen<?> screen) {
        showBucketRecipesCheckbox = widgets.addCheckbox("show_bucket_recipes",
                Component.translatable("gui.ae2fct.config.show_bucket_recipes"), this::save);
        showFluidRecipesCheckbox = widgets.addCheckbox("show_fluid_recipes",
                Component.translatable("gui.ae2fct.config.show_fluid_recipes"), this::save);
        updateState();
    }

    private void updateState() {
        if (showBucketRecipesCheckbox != null) {
            showBucketRecipesCheckbox.setSelected(FluidCraftingConfig.CLIENT.showBucketRecipesForVirtualFluids.get());
        }
        if (showFluidRecipesCheckbox != null) {
            showFluidRecipesCheckbox.setSelected(FluidCraftingConfig.CLIENT.showFluidRecipesForVirtualFluids.get());
        }
    }

    private void save() {
        if (showBucketRecipesCheckbox != null) {
            FluidCraftingConfig.CLIENT.showBucketRecipesForVirtualFluids.set(showBucketRecipesCheckbox.isSelected());
        }
        if (showFluidRecipesCheckbox != null) {
            FluidCraftingConfig.CLIENT.showFluidRecipesForVirtualFluids.set(showFluidRecipesCheckbox.isSelected());
        }
        FluidCraftingConfig.CLIENT.get().save();
        updateState();
    }
}
