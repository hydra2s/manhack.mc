package org.hydra2s.manhack.mixin.blaze3d;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.hydra2s.manhack.GlContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.glBindVertexBuffer;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.util.vma.Vma.vmaVirtualFree;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

// NEW BIG TODO LIST!
// - Needs replace a buffer memory stack!
// - Needs add immutable buffer support!
// - Needs add virtual buffer support!
// - Needs add virtual allocation support!

// TODO! NEEDS TO REPLACE:
// [x] `_vertexAttribPointer`  // support for virtual buffer with offset
// [x] `_vertexAttribIPointer` // support for virtual buffer with offset
// [x] `_drawElements`   // support for virtual buffer with offset
// [x] `_glGenBuffers`   // virtual pre-allocation
// [x] `_glBindBuffer`   // virtual binding system
// [ ] `_glBufferData`   // virtual allocation, support for virtual buffer with offset
// [ ] `mapBuffer`       // support for virtual buffer with offset
// [ ] `_glUnmapBuffer`  // support for virtual buffer with offset

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        var cache = GlContext.boundBuffers.get(GL_ARRAY_BUFFER);

        RenderSystem.assertOnRenderThread();
        if (cache == null || cache.glStorageBuffer == 0) {
            GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        } else {
            GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer + cache.offset.get(0));
            //GL43.glVertexAttribBinding(index, 0);
            //GL43.glBindVertexBuffer(0, cache.glStorageBuffer, cache.offset.get(0), stride);
            //GL43.glVertexAttribFormat(index, size, type, normalized, 0);
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _vertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        var cache = GlContext.boundBuffers.get(GL_ARRAY_BUFFER);

        RenderSystem.assertOnRenderThread();
        if (cache == null || cache.glStorageBuffer == 0) {
            GL30.glVertexAttribIPointer(index, size, type, stride, pointer);
        } else {
            GL30.glVertexAttribIPointer(index, size, type, stride, pointer + cache.offset.get(0));
            //GL43.glVertexAttribBinding(index, 0);
            //GL43.glBindVertexBuffer(0, cache.glStorageBuffer, cache.offset.get(0), stride);
            //GL43.glVertexAttribIFormat(index, size, type, 0);
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _drawElements(int mode, int count, int type, long indices) {
        RenderSystem.assertOnRenderThread();
        var cache = GlContext.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER);

        // TODO: workaround by shaders!
        // Use TEMP buffer for binding element arrays
        int EL = GL45.glCreateBuffers();
        GL45.glNamedBufferData(EL, cache.allocCreateInfo.size(), GL_DYNAMIC_DRAW);
        GL45.glCopyNamedBufferSubData(cache.glStorageBuffer, EL, cache.offset.get(0), 0, cache.allocCreateInfo.size());
        GL20.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EL);
        GL11.glDrawElements(mode, count, type, indices);
        GL20.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cache.glStorageBuffer);
        GL20.glDeleteBuffers(EL);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static int _glGenBuffers() {
        RenderSystem.assertOnRenderThreadOrInit();
        try {
            return GlContext.glCreateBuffer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glBindBuffer(int target, int glVirtualBuffer) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlContext.glBindBuffer(target, glVirtualBuffer);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glBufferData(int target, ByteBuffer data, int usage) {
        RenderSystem.assertOnRenderThreadOrInit();
        try {
            GlContext.glBufferData(target, data, usage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glBufferData(int target, long size, int usage) {
        RenderSystem.assertOnRenderThreadOrInit();
        try {
            GlContext.glBufferData(target, size, usage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @author
     * @reason
     */
    @Nullable @Overwrite
    public static ByteBuffer mapBuffer(int target, int access) {
        RenderSystem.assertOnRenderThreadOrInit();
        //return GL15.glMapBuffer(target, access);

        var cache = GlContext.boundBuffers.get(target);
        return cache.map(cache.allocCreateInfo.size(), 0L);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glUnmapBuffer(int target) {
        RenderSystem.assertOnRenderThreadOrInit();

        var cache = GlContext.boundBuffers.get(target);
        cache.unmap();
    }

    //
    @Shadow private static boolean ON_LINUX;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glDeleteBuffers(int glVirtualBuffer) {
        RenderSystem.assertOnRenderThread();
        if (ON_LINUX) {
            _glBindBuffer(GlConst.GL_ARRAY_BUFFER, glVirtualBuffer);
            _glBufferData(GlConst.GL_ARRAY_BUFFER, 0L, GlConst.GL_DYNAMIC_DRAW);
            _glBindBuffer(GlConst.GL_ARRAY_BUFFER, 0);
        }

        //
        GlContext.ResourceCache resource = GlContext.resourceCacheMap.get(glVirtualBuffer);
        if (resource != null) {
            vmaVirtualFree(resource.mapped.vb.get(0), resource.allocId.get(0));
            GlContext.resourceCacheMap.remove(glVirtualBuffer);
        }
        //GL15.glDeleteBuffers(buffer);
    }

    //
    @Redirect(method="_deleteTexture", at=@At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures(I)V"))
    private static void onDeleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
        GlContext.ResourceImage resource = GlContext.resourceImageMap.get(texture);
        if (resource != null) { GlContext.resourceImageMap.remove(texture); }
    }

    //
    @Redirect(method="_deleteTextures", at=@At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures([I)V"))
    private static void onDeleteTextures(int textures[]) {
        GL11.glDeleteTextures(textures);
        for (var I=0;I<textures.length;I++) {
            GlContext.ResourceImage resource = GlContext.resourceImageMap.get(textures[I]);
            if (resource != null) { GlContext.resourceImageMap.remove(textures[I]); }
        }
    }


}
