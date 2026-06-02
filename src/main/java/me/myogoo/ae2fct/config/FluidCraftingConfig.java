package me.myogoo.ae2fct.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FluidCraftingConfig {
    public static final Common COMMON = new Common();
    public static final ForgeConfigSpec COMMON_SPEC = COMMON.get();
    public static final ForgeConfigSpec CLIENT_SPEC = new ForgeConfigSpec.Builder().build();

    private FluidCraftingConfig() {
    }

    public static boolean allowFluidInteractionWithoutUpgrade() {
        return COMMON.allowFluidInteractionWithoutUpgrade.get();
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
}
