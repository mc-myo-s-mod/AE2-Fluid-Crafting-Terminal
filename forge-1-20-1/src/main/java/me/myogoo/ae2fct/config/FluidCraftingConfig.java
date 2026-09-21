package me.myogoo.ae2fct.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class FluidCraftingConfig {
    private static final List<String> DEFAULT_VIRTUAL_FLUID_RECIPE_BLACKLIST = List.of(
            "immersiveengineering:crafting/redstone_acid"
    );

    public static final Common COMMON = new Common();
    public static final ForgeConfigSpec COMMON_SPEC = COMMON.get();
    public static final Client CLIENT = new Client();
    public static final ForgeConfigSpec CLIENT_SPEC = CLIENT.get();

    private FluidCraftingConfig() {
    }

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

    public static boolean isVirtualFluidRecipeBlacklisted(ResourceLocation recipeId) {
        return recipeId != null && COMMON.virtualFluidRecipeBlacklist.get().stream()
                .anyMatch(id -> recipeId.toString().equals(id));
    }

    private static boolean isValidRecipeId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }

    public static final class Common {
        private final ForgeConfigSpec spec;
        public final ForgeConfigSpec.BooleanValue QoL;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> virtualFluidRecipeBlacklist;

        private Common() {
            var builder = new ForgeConfigSpec.Builder();

            builder.push("upgrade");
            this.QoL = builder
                    .comment("Enables QoL behavior without requiring the Terminal Fluid Interact Card. When false, fluid terminal interaction requires the upgrade card in the terminal upgrade slot.")
                    .define("QoL", false);
            builder.pop();

            builder.push("recipe_transfer");
            this.virtualFluidRecipeBlacklist = builder
                    .comment("Recipe ids where AE2FCT must not use virtual fluid items for recipe transfer, manual crafting pattern encoding, or cached virtual-fluid crafting patterns. Real filled buckets remain valid.")
                    .defineListAllowEmpty("virtualFluidRecipeBlacklist", DEFAULT_VIRTUAL_FLUID_RECIPE_BLACKLIST,
                            FluidCraftingConfig::isValidRecipeId);
            builder.pop();

            this.spec = builder.build();
        }

        public ForgeConfigSpec get() {
            return spec;
        }
    }

    public static final class Client {
        private final ForgeConfigSpec spec;
        public final ForgeConfigSpec.BooleanValue showBucketRecipesForVirtualFluids;
        public final ForgeConfigSpec.BooleanValue showFluidRecipesForVirtualFluids;
        public final ForgeConfigSpec.BooleanValue showBucketRecipesForAe2FluidKeys;

        private Client() {
            var builder = new ForgeConfigSpec.Builder();

            builder.push("recipe_viewer");
            this.showBucketRecipesForVirtualFluids = builder
                    .comment("When looking up a virtual fluid item in JEI/EMI/REI, include recipes and uses for the fluid's bucket item.")
                    .define("showBucketRecipesForVirtualFluids", true);
            this.showFluidRecipesForVirtualFluids = builder
                    .comment("When looking up a virtual fluid item in JEI/EMI/REI, include recipes and uses for the fluid ingredient itself.")
                    .define("showFluidRecipesForVirtualFluids", true);
            this.showBucketRecipesForAe2FluidKeys = builder
                    .comment("When looking up an AE2 fluid key in JEI/EMI/REI, include recipes and uses for the fluid's filled bucket item.")
                    .define("showBucketRecipesForAe2FluidKeys", true);
            builder.pop();

            this.spec = builder.build();
        }

        public ForgeConfigSpec get() {
            return spec;
        }
    }
}
