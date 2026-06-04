package me.myogoo.ae2fct.config;


import net.neoforged.neoforge.common.ModConfigSpec;

public class FluidCraftingConfig {
    public static final Common COMMON = new Common();
    public static final ModConfigSpec COMMON_SPEC = COMMON.get();
    public static final ModConfigSpec ClientSpec = new Client().get();

    public static boolean allowFluidInteractionWithoutUpgrade() {
        return COMMON.allowFluidInteractionWithoutUpgrade.get();
    }

    public static class Common {
        private final ModConfigSpec spec;
        public final ModConfigSpec.BooleanValue allowFluidInteractionWithoutUpgrade;

        Common() {
            var builder = new ModConfigSpec.Builder();

            builder.push("upgrade");
            this.allowFluidInteractionWithoutUpgrade = builder
                    .comment("Allows fluid terminal interaction without installing the Terminal Fluid Interact Card.")
                    .define("allowFluidInteractionWithoutUpgrade", false);
            builder.pop();

            this.spec = builder.build();
        }

        public ModConfigSpec get() {
            return spec;
        }
    }

    public static class Client {
        private final ModConfigSpec spec;

        Client() {
            var builder = new ModConfigSpec.Builder();

            this.spec = builder.build();
        }

        public ModConfigSpec get() {
            return spec;
        }
    }
}
