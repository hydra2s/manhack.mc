package org.hydra2s.manhack.mixin.render;

import net.minecraft.client.render.BufferBuilder;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.interfaces.BuiltBufferInterface;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

@Mixin(BufferBuilder.BuiltBuffer.class)
public class BuiltBufferMixin implements BuiltBufferInterface {
    @Unique public GlContext.ResourceBuffer vkVertexBuffer;
    @Unique public GlContext.ResourceBuffer vkIndexBuffer;
    @Unique public ByteBuffer preAllocated;
    @Unique public int glVertexBuffer = 0;
    @Unique public int glIndexBuffer = 0;

    //
    @Shadow private final BufferBuilder.DrawParameters parameters = null;

    //
    @Override public int getGlVertexBuffer() { return glVertexBuffer; };
    @Override public int getGlIndexBuffer() { return glIndexBuffer; };

    //
    @Override public GlContext.ResourceBuffer getVkVertexBuffer() { return vkVertexBuffer; };
    @Override public GlContext.ResourceBuffer getVkIndexBuffer() { return vkIndexBuffer; };

    @Inject(method = "<init>", at = @At("RETURN"))
    void onConstruct(BufferBuilder bufferBuilder, int batchOffset, BufferBuilder.DrawParameters parameters, CallbackInfo ci) {
        // construct vertex buffer
        this.vkVertexBuffer = GlContext.vkCreateBuffer(parameters.getVertexBufferSize()); // allocate 128Mb
        this.glVertexBuffer = GlContext.glCreateBuffer(vkVertexBuffer);

        // construct index buffer
        this.vkIndexBuffer = GlContext.vkCreateBuffer(parameters.getIndexBufferEnd() - parameters.getIndexBufferStart()); // allocate 128Mb
        this.glIndexBuffer = GlContext.glCreateBuffer(vkIndexBuffer);

        //
        this.syncData();
    }

    @Override public void fromIndexData(ByteBuffer preAllocated) {
        if (this.vkIndexBuffer == null || this.vkIndexBuffer.obj == null) {
            this.vkIndexBuffer = GlContext.vkCreateBuffer((this.preAllocated = preAllocated).capacity());
            this.glIndexBuffer = GlContext.glCreateBuffer(vkIndexBuffer);

            //
            //MemoryUtil.memCopy(preAllocated != null ? preAllocated : this.getIndexBuffer(), this.vkIndexBuffer.obj.map(preAllocated.capacity(), 0));
        }
    }

    @Override public boolean syncData() {
        if (this.vkIndexBuffer == null || this.vkIndexBuffer.obj == null) {
            this.vkIndexBuffer = GlContext.vkCreateBuffer(preAllocated.capacity()); // allocate 128Mb
            this.glIndexBuffer = GlContext.glCreateBuffer(vkIndexBuffer);
        }

        //
        MemoryUtil.memCopy(this.getVertexBuffer(), this.vkVertexBuffer.obj.map(parameters.getVertexBufferSize(), 0));
        MemoryUtil.memCopy(this.preAllocated != null ? this.preAllocated : this.getIndexBuffer(), this.vkIndexBuffer.obj.map(this.preAllocated != null ? this.preAllocated.capacity() : (this.parameters.getIndexBufferEnd() - this.parameters.getIndexBufferStart()), 0));

        //
        this.vkVertexBuffer.obj.unmap();
        this.vkIndexBuffer.obj.unmap();

        //
        return true;
    }

    @Shadow public ByteBuffer getVertexBuffer() { return null; }
    @Shadow public ByteBuffer getIndexBuffer() { return null; }
}
