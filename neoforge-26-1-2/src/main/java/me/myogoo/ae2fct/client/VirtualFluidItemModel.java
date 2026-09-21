package me.myogoo.ae2fct.client;

import appeng.api.stacks.AEFluidKey;
import appeng.client.renderer.keytypes.FluidKeyRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public record VirtualFluidItemModel(Matrix4fc transform)
        implements ItemModel, SpecialModelRenderer<FluidKeyRenderer.RenderState> {
    private static final FluidKeyRenderer FLUID_RENDERER = new FluidKeyRenderer();
    private static final Vector3fc[] EXTENTS = {
            new Vector3f(0, 0, 0.51f), new Vector3f(1, 1, 0.51f)
    };

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver,
            ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        var virtualFluid = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
        if (virtualFluid == null || virtualFluid.fluid().isEmpty()) {
            return;
        }

        var fluidKey = AEFluidKey.of(virtualFluid.fluid());
        var fluidState = FLUID_RENDERER.createState();
        FLUID_RENDERER.extract(fluidState, fluidKey, level, seed);

        var layer = renderState.newLayer();
        layer.setLocalTransform(transform);
        layer.setUsesBlockLight(false);
        layer.setExtents(() -> EXTENTS);
        layer.setupSpecialModel(this, fluidState);
        renderState.appendModelIdentityElement(this);
        renderState.appendModelIdentityElement(fluidKey);
        renderState.setAnimated();
    }

    @Override
    public void submit(FluidKeyRenderer.RenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
            int packedLight, int packedOverlay, boolean hasFoil, int seed) {
        poseStack.pushPose();
        // AE2 renders around the origin; item models use the unit cube.
        poseStack.translate(0.5f, 0.5f, 0.5f);
        FLUID_RENDERER.submit(poseStack, state, nodes, packedLight);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> extents) {
        for (var point : EXTENTS) {
            extents.accept(point);
        }
    }

    @Override
    public @Nullable FluidKeyRenderer.RenderState extractArgument(ItemStack stack) {
        // update supplies the extracted state directly to setupSpecialModel.
        return null;
    }

    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
            return new VirtualFluidItemModel(new Matrix4f(transform));
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}
