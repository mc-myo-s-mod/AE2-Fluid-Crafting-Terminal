package me.myogoo.ae2fct.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class VirtualFluidItemRenderer extends BlockEntityWithoutLevelRenderer {

    public VirtualFluidItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!stack.has(AE2FCTDataComponent.VIRTUAL_FLUID))
            return;
        FluidStack fluidStack = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID).fluid();
        if (fluidStack == null || fluidStack.isEmpty())
            return;

        IClientFluidTypeExtensions fluidExt = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(fluidExt.getStillTexture(fluidStack));
        int color = fluidExt.getTintColor(fluidStack);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());

        int alpha = (color >> 24) & 0xFF;
        if (alpha == 0)
            alpha = 255;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = ((color >> 0) & 0xFF) / 255f;
        float a = alpha / 255f;

        var vertexConsumer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        poseStack.pushPose();
        Matrix4f matrix4f = poseStack.last().pose();

        float minX = 0f;
        float maxX = 1f;
        float minY = 0f;
        float maxY = 1f;
        float z = 0.5f;

        float zFront = 0.501f;
        float zBack = 0.499f;

        // Front face (+Z)
        renderFace(matrix4f, vertexConsumer, 0, 1, 0, 1, zFront, sprite, r, g, b, a, packedLight, packedOverlay, 0, 0,
                1);

        // Back face (-Z)
        renderFace(matrix4f, vertexConsumer, 1, 0, 0, 1, zBack, sprite, r, g, b, a, packedLight, packedOverlay, 0, 0,
                -1);

        poseStack.popPose();
        RenderSystem.disableBlend();
        BufferUploader.drawWithShader(vertexConsumer.buildOrThrow());
    }

    private void renderFace(Matrix4f matrix, VertexConsumer builder, float minX, float maxX, float minY, float maxY,
            float z,
            TextureAtlasSprite sprite, float r, float g, float b, float a, int light, int overlay, float nx, float ny,
            float nz) {
        builder.addVertex(matrix, minX, minY, z).setColor(r, g, b, a).setUv(sprite.getU0(), sprite.getV1())
                .setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        builder.addVertex(matrix, maxX, minY, z).setColor(r, g, b, a).setUv(sprite.getU1(), sprite.getV1())
                .setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        builder.addVertex(matrix, maxX, maxY, z).setColor(r, g, b, a).setUv(sprite.getU1(), sprite.getV0())
                .setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        builder.addVertex(matrix, minX, maxY, z).setColor(r, g, b, a).setUv(sprite.getU0(), sprite.getV0())
                .setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
    }
}
