package org.hydra2s.manhack.mixin.render;

//
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.collector.GlDrawCollector;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Final @Shadow MinecraftClient client;

    //
    @Inject(method="render(FJZ)V", at=@At("HEAD"))
    public void onRenderBegin(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        GlContext.worldRendering = false;
    }

    @Redirect(method="renderWorld", at=@At(value="INVOKE", target="Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;FJZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;)V"))
    public void onRender(WorldRenderer instance, MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix) throws Exception {
        GlContext.rendererObj.camera = camera;
        camera.getRotation().get(GlContext.rendererObj.viewMatrix);
        GlContext.rendererObj.viewMatrix = GlContext.rendererObj.viewMatrix.transpose();
        GlDrawCollector.resetDraw();

        //
        GlContext.worldRendering = true;
        this.client.worldRenderer.render(matrices, tickDelta, limitTime, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, positionMatrix);
        GlContext.worldRendering = false;
        GlContext.rendererObj.tickRendering();
    }
}
