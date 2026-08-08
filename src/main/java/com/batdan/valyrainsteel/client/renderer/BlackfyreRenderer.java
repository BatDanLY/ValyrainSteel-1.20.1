package com.batdan.valyrainsteel.client.renderer;

import com.batdan.valyrainsteel.client.model.BlackfyreModel;
import com.batdan.valyrainsteel.item.BlackfyreItem;

import software.bernie.geckolib.renderer.GeoItemRenderer;

public class BlackfyreRenderer extends GeoItemRenderer<BlackfyreItem> {

    public BlackfyreRenderer() {
        super(new BlackfyreModel());
    }
}