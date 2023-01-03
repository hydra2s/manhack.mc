package org.hydra2s.manhack.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.hydra2s.manhack.collector.GlDrawCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    //
    @Redirect(method="drawElements", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/platform/GlStateManager;_drawElements(IIIJ)V"))
    private static void onDrawElements(int mode, int count, int type, long indices) throws Exception {
        GlDrawCollector.collectDraw(mode, count, type, indices);
        GlStateManager._drawElements(mode, count, type, indices);
    }

}
