package org.hydra2s.manhack.mixin.render;

//
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.collector.GlDrawCollector;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    //
    @Inject(method="setupFrustum", at=@At("HEAD"))
    public void onSetupFrustum(MatrixStack matrices, Vec3d pos, Matrix4f projectionMatrix, CallbackInfo ci) {
        GlContext.rendererObj.projectionMatrix = projectionMatrix;
    }

    //
    @Inject(method="render", at=@At("HEAD"))
    public void onRenderBegin(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {
        GlContext.worldRendering = true;
        GlContext.rendererObj.camera = camera;
        camera.getRotation().get(GlContext.rendererObj.viewMatrix);
        GlDrawCollector.resetDraw();
    }

    //
    @Inject(method="render", at=@At("RETURN"))
    public void onRenderEnd(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {

        // Test rendering!
        // TODO: OpenGL isn't "know" about your/our virtual swap-chain
        // TODO: pardon, but needs OpenGL shader for draw such image, or fully replace to Vulkan API
        // Plan was partially failed...
        GlContext.worldRendering = false;
        GlContext.rendererObj.tickRendering();
    }

}
