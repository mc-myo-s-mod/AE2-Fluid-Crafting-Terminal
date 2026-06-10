package me.myogoo.ae2fct.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FluidCraftingConfig {
    public static final Common COMMON = new Common();
    public static final ForgeConfigSpec COMMON_SPEC = COMMON.get();
    public static final Client CLIENT = new Client();
    public static final ForgeConfigSpec CLIENT_SPEC = CLIENT.get();

    private FluidCraftingConfig() {
    }

    public static boolean allowFluidInteractionWithoutUpgrade() {
        return COMMON.allowFluidInteractionWithoutUpgrade.get();
    }

    public static boolean showBucketRecipesForVirtualFluids() {
        return CLIENT.showBucketRecipesForVirtualFluids.get();
    }

    public static boolean showFluidRecipesForVirtualFluids() {
        return CLIENT.showFluidRecipesForVirtualFluids.get();
    }

    public static final class Common {
        private final ForgeConfigSpec spec;
        public final ForgeConfigSpec.BooleanValue allowFluidInteractionWithoutUpgrade;

        private Common() {
            var builder = new ForgeConfigSpec.Builder();

            builder.push("upgrade");
            this.allowFluidInteractionWithoutUpgrade = builder
                    .comment("Allows fluid terminal interaction without installing the Terminal Fluid Interact Card.")
                    .define("allowFluidInteractionWithoutUpgrade", false);
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

        private Client() {
            var builder = new ForgeConfigSpec.Builder();

            builder.push("recipe_viewer");
            this.showBucketRecipesForVirtualFluids = builder
                    .comment("When looking up a virtual fluid item in JEI/EMI, include recipes and uses for the fluid's bucket item.")
                    .define("showBucketRecipesForVirtualFluids", true);
            this.showFluidRecipesForVirtualFluids = builder
                    .comment("When looking up a virtual fluid item in JEI/EMI, include recipes and uses for the fluid ingredient itself.")
                    .define("showFluidRecipesForVirtualFluids", true);
            builder.pop();

            this.spec = builder.build();
        }

        public ForgeConfigSpec get() {
            return spec;
        }
    }
}
