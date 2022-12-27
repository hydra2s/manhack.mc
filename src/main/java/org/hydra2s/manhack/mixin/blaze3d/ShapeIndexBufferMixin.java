package org.hydra2s.manhack.mixin.blaze3d;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.minecraft.client.render.VertexFormat;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.interfaces.ShapeIndexBufferInterface;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

@Mixin(RenderSystem.ShapeIndexBuffer.class)
public class ShapeIndexBufferMixin implements ShapeIndexBufferInterface {
    @Shadow private int vertexCountInTriangulated;
    @Shadow private VertexFormat.IndexType indexType;
    //@Shadow private RenderSystem.ShapeIndexBuffer.Triangulator triangulator;
    @Shadow private int vertexCountInShape;
    @Shadow private int size;

    @Unique public GlContext.ResourceBuffer vk;
    @Shadow private int id;
    @Unique ByteBuffer preAllocated;



    // allocate vulkan buffer instead
    // TODO: vulkan memory mapping
    // TODO: unbound data for built buffer memory
    // TODO: unbound from OpenGL API
    @Redirect(method="grow(I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glBufferData(IJI)V"))
    private void onBufferData(int target, long data, int usage) {
        // allocate 128Mb
        //if (this.vk == null || this.id == 0) {
            // TODO: remove from GL
            //this.id = GlContext.glCreateBuffer(new int[]{this.id}, this.vk = GlContext.vkCreateBuffer(Math.max(data, 1024 * 1024 * 128)));
        //}

        if (this.preAllocated == null) {
            this.preAllocated = MemoryUtil.memAlloc((int)data);
        }
    }

    // return pre-allocated memory
    @Redirect(method="grow(I)V", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/mojang/blaze3d/platform/GlStateManager;mapBuffer(II)Ljava/nio/ByteBuffer;"))
    private ByteBuffer onMapData(int target, int access) {
        return this.preAllocated;
    }

    // stub for avoid GL error
    @Redirect(method="grow(I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glUnmapBuffer(I)V"))
    private void onUnmapData(int target, int access) {
        
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public boolean isLargeEnough(int capacity) {
        return false;
    }

    @Override
    public ByteBuffer getPreAllocated() {
        var preAllocated = this.preAllocated; this.preAllocated = null; return preAllocated;
    };

    @Shadow private void grow(int requiredSize) {}
    @Shadow private IntConsumer getIndexConsumer(ByteBuffer byteBuffer) { return null;}
    //@Shadow private boolean isLargeEnough(int requiredSize) { return false; }
}
