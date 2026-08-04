package com.batdan.valyrainsteel.client.renderer;

import com.batdan.valyrainsteel.entity.projectile.CalamityProjectile;
import com.batdan.valyrainsteel.entity.projectile.CatspawDaggerProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;

public class CalamityRenderer extends EntityRenderer<CalamityProjectile> {

    private final ItemRenderer itemRenderer;

    public CalamityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(
            CalamityProjectile entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {

        poseStack.pushPose();

        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // Model points sideways
        //poseStack.mulPose(Axis.ZP.rotationDegrees(-90));

        // Put centre of mass on entity origin
        //poseStack.translate(0.0, 0.0, -0.37);

        float spin = (entity.tickCount + partialTicks) * 55F;

        // Head over heels
        poseStack.mulPose(Axis.XP.rotationDegrees(spin));

        itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.NONE,
                packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public net.minecraft.resources.ResourceLocation getTextureLocation(CalamityProjectile entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}