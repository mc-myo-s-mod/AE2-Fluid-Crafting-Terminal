package me.myogoo.ae2fct.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.integration.jei.AE2FCTJeiPlugin;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.common.Internal;
import mezz.jei.common.ingredients.TypedIngredient;
import mezz.jei.common.input.InputPhase;
import mezz.jei.common.input.UserInput;
import mezz.jei.gui.input.ClickableIngredientInternal;
import mezz.jei.gui.input.GuiInputScene;
import mezz.jei.gui.input.IClickableIngredientInternal;
import mezz.jei.gui.input.IDraggableIngredientInternal;
import mezz.jei.gui.input.IRecipeFocusSource;
import mezz.jei.gui.input.IngredientFocusSource;
import mezz.jei.gui.input.InputArea;
import mezz.jei.gui.input.InputCommands;
import mezz.jei.gui.input.handlers.IngredientCommands;
import mezz.jei.gui.overlay.elements.IngredientElement;
import mezz.jei.gui.util.FocusUtil;
import mezz.jei.library.ingredients.IngredientManager;
import mezz.jei.forge.platform.FluidHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.RegisterEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Exercises JEI's real command dispatch and the production mixin without opening a recipe screen. */
final class JeiLookupProbe {
    private JeiLookupProbe() {
    }

    static void run() {
        boolean previousBucket = FluidCraftingConfig.CLIENT.showBucketRecipesForVirtualFluids.get();
        boolean previousFluid = FluidCraftingConfig.CLIENT.showFluidRecipesForVirtualFluids.get();
        var queries = new ArrayList<Query>();
        var source = new Source();
        var ingredients = new IngredientManager(null) {
            @Override
            public <T> Optional<ITypedIngredient<T>> createTypedIngredient(IIngredientType<T> type, T ingredient,
                    boolean normalize) {
                return Optional.of(TypedIngredient.createUnvalidated(type, ingredient));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<ITypedIngredient<T>> createTypedIngredient(T ingredient, boolean normalize) {
                if (!(ingredient instanceof ItemStack stack)) {
                    throw new AssertionError("Unexpected untyped ingredient: " + ingredient);
                }
                return Optional.of((ITypedIngredient<T>) TypedIngredient.createUnvalidated(VanillaTypes.ITEM_STACK, stack));
            }
        };
        var focusUtil = new FocusUtil(null, null, ingredients) {
            @Override
            public List<IFocus<?>> createFocuses(ITypedIngredient<?> ingredient, List<RecipeIngredientRole> roles) {
                queries.add(new Query(ingredient.getIngredient(), List.copyOf(roles)));
                // Record expansion requests, but reject every focus: completion must fall back to native lookup.
                return List.of();
            }
        };
        new AE2FCTJeiPlugin().registerFluidSubtypes(null, new FluidHelper());
        var scene = new GuiInputScene(InputArea.builder("AE2FCT lookup probe", source).build());
        var commands = new InputCommands();
        new IngredientCommands(new IngredientFocusSource(scene), Internal.getKeyMappings(), null, focusUtil,
                ingredients, Internal.getClientToggleState(), null, null, null).registerInputCommands(commands);
        try {
            for (boolean bucket : new boolean[]{false, true}) {
                FluidCraftingConfig.CLIENT.showBucketRecipesForVirtualFluids.set(bucket);
                for (boolean fluid : new boolean[]{false, true}) {
                    FluidCraftingConfig.CLIENT.showFluidRecipesForVirtualFluids.set(fluid);
                    for (int key : new int[]{GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_U}) {
                        source.reset(VirtualFluidItem.createItemStack(Fluids.WATER));
                        queries.clear();
                        var input = keyboard(key);
                        var action = commands.resolve(Minecraft.getInstance().screen, input, true).orElseThrow();
                        require(queries.isEmpty() && source.nativeLookups == 0, "Lookup ran during preparation");
                        action.complete(input);
                        require(source.nativeLookups == 1, "Empty focus expansion lost native lookup fallback");
                        require(queries.size() == (bucket ? 1 : 0) + (fluid ? 1 : 0), "Wrong number of fluid targets");
                        require(queries.stream().anyMatch(q -> q.ingredient instanceof ItemStack stack
                                && stack.is(Items.WATER_BUCKET)) == bucket, "Bucket target missing or unexpected");
                        require(queries.stream().anyMatch(q -> q.ingredient instanceof FluidStack stack
                                && stack.getFluid() == Fluids.WATER) == fluid, "Fluid target missing or unexpected");
                        var roles = key == GLFW.GLFW_KEY_R ? List.of(RecipeIngredientRole.OUTPUT)
                                : List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.CATALYST);
                        require(queries.stream().allMatch(q -> q.roles.equals(roles)), "Wrong recipe/usage roles");
                    }
                }
            }

            FluidCraftingConfig.CLIENT.showBucketRecipesForVirtualFluids.set(true);
            FluidCraftingConfig.CLIENT.showFluidRecipesForVirtualFluids.set(true);
            for (int button : new int[]{GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_MOUSE_BUTTON_RIGHT}) {
                source.reset(VirtualFluidItem.createItemStack(Fluids.WATER));
                queries.clear();
                var press = mouse(button, 5, InputPhase.PRESS);
                require(press.is(button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                        ? Internal.getKeyMappings().getShowRecipe() : Internal.getKeyMappings().getShowUses()),
                        "JEI mouse recipe/usage binding does not match the test input");
                var action = commands.resolve(Minecraft.getInstance().screen, press, true).orElseThrow();
                require(queries.isEmpty(), "Mouse lookup ran before release");
                action.complete(mouse(button, 50, InputPhase.RELEASE));
                require(queries.isEmpty() && source.nativeLookups == 0,
                        "Release outside the original ingredient performed a lookup or fallback");

                action = commands.resolve(Minecraft.getInstance().screen, press, true).orElseThrow();
                source.visible = false;
                action.complete(mouse(button, 5, InputPhase.RELEASE));
                require(queries.isEmpty() && source.nativeLookups == 0, "Hidden ingredient performed a lookup or fallback");
                require(commands.resolve(Minecraft.getInstance().screen, press, true).isEmpty(),
                        "Hidden ingredient was selected");

                source.visible = true;
                action = commands.resolve(Minecraft.getInstance().screen, press, true).orElseThrow();
                action.complete(mouse(button, 5, InputPhase.RELEASE));
                require(queries.size() == 2 && source.nativeLookups == 1,
                        "Mouse release did not expand fluid targets and fall back after empty focuses");
                queries.clear();
                source.canClick = false;
                require(commands.resolve(Minecraft.getInstance().screen, press, true).isEmpty(),
                        "Conflicting vanilla mouse click bypassed canClickToFocus");
            }

            FluidCraftingConfig.CLIENT.showBucketRecipesForVirtualFluids.set(true);
            FluidCraftingConfig.CLIENT.showFluidRecipesForVirtualFluids.set(false);
            require(BucketlessFluidFixture.fluid != null, "Bucketless fluid fixture was not registered");
            require(FluidUtil.getFilledBucket(new FluidStack(BucketlessFluidFixture.fluid, 1000)).isEmpty(),
                    "Bucketless fluid fixture unexpectedly has a bucket");
            source.reset(VirtualFluidItem.createItemStack(BucketlessFluidFixture.fluid));
            queries.clear();
            var bucketlessInput = keyboard(GLFW.GLFW_KEY_R);
            var bucketlessAction = commands.resolve(Minecraft.getInstance().screen, bucketlessInput, true).orElseThrow();
            require(source.nativeLookups == 0, "Bucketless fallback ran during preparation");
            bucketlessAction.complete(bucketlessInput);
            require(source.nativeLookups == 1 && queries.isEmpty(),
                    "Bucket-only lookup of a bucketless fluid must fall back without generating targets");

            source.reset(new ItemStack(Items.STONE));
            queries.clear();
            commands.resolve(Minecraft.getInstance().screen, keyboard(GLFW.GLFW_KEY_R), true).orElseThrow()
                    .complete(keyboard(GLFW.GLFW_KEY_R));
            require(source.nativeLookups == 1 && queries.isEmpty(), "Ordinary ingredient lookup was intercepted");
            LogUtils.getLogger().info("AE2FCT JEI command lookup expansion/deferred fallback/bucketless fluid/visibility/click guards PASS");
        } finally {
            FluidCraftingConfig.CLIENT.showBucketRecipesForVirtualFluids.set(previousBucket);
            FluidCraftingConfig.CLIENT.showFluidRecipesForVirtualFluids.set(previousFluid);
        }
    }

    private static UserInput keyboard(int key) {
        return new UserInput(InputConstants.Type.KEYSYM.getOrCreate(key), 5, 5, 0, InputPhase.PRESS);
    }

    private static UserInput mouse(int button, double x, InputPhase phase) {
        return new UserInput(InputConstants.Type.MOUSE.getOrCreate(button), x, 5, 0, phase);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Query(Object ingredient, List<RecipeIngredientRole> roles) {
    }

    @EventBusSubscriber(modid = "ae2fct_gametest", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class BucketlessFluidFixture {
        private static Fluid fluid;

        @SubscribeEvent
        public static void register(RegisterEvent event) {
            if (!Boolean.getBoolean("ae2fct.clientProbe")) {
                return;
            }
            event.register(Registries.FLUID, helper -> {
                fluid = new WaterFluid.Source() {
                    @Override
                    public Item getBucket() {
                        return Items.AIR;
                    }

                    @Override
                    public FluidType getFluidType() {
                        return Fluids.WATER.getFluidType();
                    }
                };
                helper.register(new ResourceLocation("ae2fct_gametest", "bucketless_lookup"), fluid);
            });
        }
    }

    private static final class Source implements IRecipeFocusSource {
        private IngredientElement<ItemStack> element;
        private boolean visible;
        private boolean canClick;
        private int nativeLookups;

        private void reset(ItemStack stack) {
            visible = true;
            canClick = true;
            nativeLookups = 0;
            element = new IngredientElement<>(TypedIngredient.createUnvalidated(VanillaTypes.ITEM_STACK, stack)) {
                @Override
                public boolean isVisible() {
                    return visible;
                }

                @Override
                public void show(IRecipesGui recipes, FocusUtil focusUtil, List<RecipeIngredientRole> roles) {
                    nativeLookups++;
                }
            };
        }

        @Override
        public Stream<IClickableIngredientInternal<?>> getIngredientUnderMouse(double mouseX, double mouseY) {
            return containsMouse(mouseX, mouseY)
                    ? Stream.of(new ClickableIngredientInternal<>(element, this, false, canClick)) : Stream.empty();
        }

        @Override
        public Stream<IDraggableIngredientInternal<?>> getDraggableIngredientUnderMouse(double mouseX, double mouseY) {
            return Stream.empty();
        }

        @Override
        public boolean containsMouse(double mouseX, double mouseY) {
            return mouseX >= 0 && mouseX < 20 && mouseY >= 0 && mouseY < 20;
        }
    }
}
