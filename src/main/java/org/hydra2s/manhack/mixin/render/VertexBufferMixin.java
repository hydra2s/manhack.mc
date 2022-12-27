package org.hydra2s.manhack.mixin.render;

//
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//
import java.nio.ByteBuffer;

// BIG TODO LIST!
// - For Vulkan API, needs replace a `public void upload(BufferBuilder.BuiltBuffer buffer)`

@Mixin(VertexBuffer.class)
public class VertexBufferMixin {

    // TODO: replace by mapped Vulkan Buffer of Builder
    // TODO: remove such function, or replace by synchronization
    @Inject(method = "uploadVertexBuffer", at = @At("HEAD"))
    public void onUploadVertex(BufferBuilder.DrawParameters parameters, ByteBuffer vertexBuffer) {

    }

    // TODO: replace by mapped Vulkan Buffer of Builder
    // TODO: remove such function, or replace by synchronization
    @Inject(method = "uploadIndexBuffer", at = @At("HEAD"))
    public void onUploadIndex(BufferBuilder.DrawParameters parameters, ByteBuffer indexBuffer) {

    }

    // TODO: replace such method to compatible
    @Inject(method="bind()V", at=@At("HEAD"))
    public void bind(CallbackInfo ci) {

    }
}
