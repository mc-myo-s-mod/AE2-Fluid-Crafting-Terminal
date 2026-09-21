package me.myogoo.ae2fct.config;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

public class FluidCraftingConfig {
    private static final List<String> DEFAULT_VIRTUAL_FLUID_RECIPE_BLACKLIST = List.of(
            "immersiveengineering:crafting/redstone_acid"
    );

    public static final Common COMMON = new Common();
    public static final ModConfigSpec COMMON_SPEC = COMMON.get();
    public static final Client CLIENT = new Client();
    public static final ModConfigSpec CLIENT_SPEC = CLIENT.get();

    public static boolean QoL() {
        return COMMON.QoL.get();
    }

    public static boolean showBucketRecipesForVirtualFluids() {
        return CLIENT.showBucketRecipesForVirtualFluids.get();
    }

    public static boolean showFluidRecipesForVirtualFluids() {
        return CLIENT.showFluidRecipesForVirtualFluids.get();
    }

    public static boolean showBucketRecipesForAe2FluidKeys() {
        return CLIENT.showBucketRecipesForAe2FluidKeys.get();
    }

    public static boolean isVirtualFluidRecipeBlacklisted(ResourceKey<Recipe<?>> recipeId) {
        return recipeId != null && COMMON.virtualFluidRecipeBlacklist.get().stream()
                .anyMatch(id -> recipeId.identifier().toString().equals(id));
    }

    private static boolean isValidRecipeId(Object value) {
        return value instanceof String id && Identifier.tryParse(id) != null;
    }

    public static class Common {
        private final ModConfigSpec spec;
        public final ModConfigSpec.BooleanValue QoL;
        public final ModConfigSpec.ConfigValue<List<? extends String>> virtualFluidRecipeBlacklist;

        Common() {
            var builder = new ModConfigSpec.Builder();

            builder.push("upgrade");
            this.QoL = builder
                    .comment("Enables QoL behavior without requiring the Terminal Fluid Interact Card. When false, fluid terminal interaction requires the upgrade card in the terminal upgrade slot.")
                    .define("QoL", false);
            builder.pop();

            builder.push("recipe_transfer");
            this.virtualFluidRecipeBlacklist = builder
                    .comment("Recipe ids that require real bucket items instead of virtual fluids, including recipe transfer, manual crafting, and crafting pattern encoding. All other recipes allow virtual fluids. The server's list controls terminal recipe previews.")
                    .defineListAllowEmpty("virtualFluidRecipeBlacklist", DEFAULT_VIRTUAL_FLUID_RECIPE_BLACKLIST,
                            FluidCraftingConfig::isValidRecipeId);
            builder.pop();

            this.spec = builder.build();
        }

        public ModConfigSpec get() {
            return spec;
        }
    }

    public static class Client {
        private final ModConfigSpec spec;
        public final ModConfigSpec.BooleanValue showBucketRecipesForVirtualFluids;
        public final ModConfigSpec.BooleanValue showFluidRecipesForVirtualFluids;
        public final ModConfigSpec.BooleanValue showBucketRecipesForAe2FluidKeys;

        Client() {
            var builder = new ModConfigSpec.Builder();

            builder.push("recipe_viewer");
            this.showBucketRecipesForVirtualFluids = builder
                    .comment("When looking up a virtual fluid item in JEI/REI, include recipes and uses for the fluid's bucket item.")
                    .define("showBucketRecipesForVirtualFluids", true);
            this.showFluidRecipesForVirtualFluids = builder
                    .comment("When looking up a virtual fluid item in JEI/REI, include recipes and uses for the fluid ingredient itself.")
                    .define("showFluidRecipesForVirtualFluids", true);
            this.showBucketRecipesForAe2FluidKeys = builder
                    .comment("When looking up an AE2 fluid key in JEI/REI, include recipes and uses for the fluid's filled bucket item.")
                    .define("showBucketRecipesForAe2FluidKeys", true);
            builder.pop();

            this.spec = builder.build();
        }

        public ModConfigSpec get() {
            return spec;
        }
    }
}
