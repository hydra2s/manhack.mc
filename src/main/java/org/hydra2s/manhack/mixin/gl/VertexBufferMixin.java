package org.hydra2s.manhack.mixin.gl;

//
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//
import java.nio.ByteBuffer;

@Mixin(VertexBuffer.class)
public class VertexBufferMixin {

    // TODO: replace by mapped Vulkan Buffer of Builder
    @Inject(method = "uploadVertexBuffer", at = @At("HEAD"))
    public void onUploadVertex(BufferBuilder.DrawParameters parameters, ByteBuffer vertexBuffer) {

    }

    // TODO: replace by mapped Vulkan Buffer of Builder
    @Inject(method = "uploadIndexBuffer", at = @At("HEAD"))
    public void onUploadIndex(BufferBuilder.DrawParameters parameters, ByteBuffer indexBuffer) {

    }

    // bind to draw currency
    @Inject(method="bind()V", at=@At("HEAD"))
    public void bind(CallbackInfo ci) {

    }
}
