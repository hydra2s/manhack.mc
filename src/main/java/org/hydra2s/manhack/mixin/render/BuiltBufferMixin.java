package org.hydra2s.manhack.mixin.render;

import net.minecraft.client.render.BufferBuilder;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.interfaces.BuiltBufferInterface;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
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
    @Shadow private BufferBuilder.DrawParameters parameters;

    //
    @Override public int getGlVertexBuffer() { return glVertexBuffer; };
    @Override public int getGlIndexBuffer() { return glIndexBuffer; };

    //
    @Override public GlContext.ResourceBuffer getVkVertexBuffer() { return vkVertexBuffer; };
    @Override public GlContext.ResourceBuffer getVkIndexBuffer() { return vkIndexBuffer; }

    @Override
    public boolean syncData() {
        return false;
    }

    @Override
    public void fromIndexData(ByteBuffer preAllocated) {

    }
}
