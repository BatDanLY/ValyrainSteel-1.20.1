package com.batdan.valyrainsteel.client.renderer;

import com.batdan.valyrainsteel.entity.projectile.CatspawDaggerProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;

public class CatspawDaggerRenderer extends EntityRenderer<CatspawDaggerProjectile> {

    private final ItemRenderer itemRenderer;

    public CatspawDaggerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(
            CatspawDaggerProjectile entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {

        poseStack.pushPose();

        // Move to centre
        poseStack.translate(0D, 0.1D, 0D);

        poseStack.scale(0.75F, 0.75F, 0.75F);

        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()*-1));

        if (!entity.isInGround()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.tickCount * 30F));
        }

        // Face direction of travel


        // Continuous spin



        itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.GROUND,
                packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                0
        );

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public net.minecraft.resources.ResourceLocation getTextureLocation(CatspawDaggerProjectile entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}