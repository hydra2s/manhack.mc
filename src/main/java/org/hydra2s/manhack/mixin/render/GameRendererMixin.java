package org.hydra2s.manhack.mixin.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import org.hydra2s.manhack.GlContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    //
    @Inject(method="render(FJZ)V", at=@At("HEAD"))
    public void onRenderBegin(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        GlContext.worldRendering = false;
    };

}
