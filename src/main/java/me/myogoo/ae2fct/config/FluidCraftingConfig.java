package me.myogoo.ae2fct.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FluidCraftingConfig {
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

    public static class Common {
        private final ModConfigSpec spec;
        public final ModConfigSpec.BooleanValue QoL;

        Common() {
            var builder = new ModConfigSpec.Builder();

            builder.push("upgrade");
            this.QoL = builder
                    .comment("Enables QoL behavior without requiring the Terminal Fluid Interact Card. When false, fluid terminal interaction requires the upgrade card in the terminal upgrade slot.")
                    .define("QoL", false);
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

        Client() {
            var builder = new ModConfigSpec.Builder();

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

        public ModConfigSpec get() {
            return spec;
        }
    }
}
