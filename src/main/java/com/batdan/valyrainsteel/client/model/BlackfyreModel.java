package com.batdan.valyrainsteel.client.model;

import com.batdan.valyrainsteel.Valyrainsteel;
import com.batdan.valyrainsteel.item.BlackfyreItem;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class BlackfyreModel extends GeoModel<BlackfyreItem> {

    @Override
    public ResourceLocation getModelResource(BlackfyreItem animatable) {
        return new ResourceLocation(Valyrainsteel.MODID, "geo/blackfyre.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BlackfyreItem animatable) {
        return new ResourceLocation(Valyrainsteel.MODID, "textures/item/blackfyre.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BlackfyreItem animatable) {
        return new ResourceLocation(Valyrainsteel.MODID, "animations/blackfyre.animation.json");
    }
}