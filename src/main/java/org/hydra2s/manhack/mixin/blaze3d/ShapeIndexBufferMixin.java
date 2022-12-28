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

import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

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
        if (this.vk == null || this.id == 0) {
            this.id = GlContext.glCreateBuffer(new int[]{this.id}, this.vk = GlContext.vkCreateBuffer(Math.max(data, 1024 * 1024 * 3 * 128)));
        }
    }

    // remap mapping...
    @Redirect(method="grow(I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;mapBuffer(II)Ljava/nio/ByteBuffer;"))
    private ByteBuffer onMapData(int target, int access) {
        return this.vk.obj.map(VK_WHOLE_SIZE, 0);
    }

    // remap unmapping...
    @Redirect(method="grow(I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glUnmapBuffer(I)V"))
    private void onUnmapData(int target, int access) {
        this.vk.obj.unmap();
    }

    @Shadow private void grow(int requiredSize) {}
    @Shadow private IntConsumer getIndexConsumer(ByteBuffer byteBuffer) { return null;}

    @Override
    public ByteBuffer getPreAllocated() {
        return null;
    }
    //@Shadow private boolean isLargeEnough(int requiredSize) { return false; }
}
