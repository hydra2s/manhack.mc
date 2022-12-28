package org.hydra2s.manhack.mixin.blaze3d;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.hydra2s.manhack.GlContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.*;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL42.GL_BUFFER_UPDATE_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.GL_ELEMENT_ARRAY_BARRIER_BIT;
import static org.lwjgl.opengl.GL43.glBindVertexBuffer;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAlloc;
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
// [x] `_glBufferData`   // virtual allocation, support for virtual buffer with offset
// [x] `mapBuffer`       // support for virtual buffer with offset
// [x] `_glUnmapBuffer`  // support for virtual buffer with offset

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) throws Exception {
        RenderSystem.assertOnRenderThread();
        var cache = GlContext.boundBuffers.get(GL_ARRAY_BUFFER);

        // TODO: replace a VAO binding stack!
        // TODO: deferred vertex pointer system!
        var vBinding = 0;//index;
        cache.stride = stride;
        GL43.glVertexAttribBinding(index, cache.bindingIndex = vBinding);
        GL43.glVertexAttribFormat(index, size, type, normalized, (int) pointer);

        //
        if (cache.target == GL_ARRAY_BUFFER && cache.stride > 0 && cache.size > 0) {
            glBindVertexBuffer(cache.bindingIndex, cache.glStorageBuffer, cache.offset.get(0), cache.stride);
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _vertexAttribIPointer(int index, int size, int type, int stride, long pointer) throws Exception {
        RenderSystem.assertOnRenderThread();
        var cache = GlContext.boundBuffers.get(GL_ARRAY_BUFFER);

        // TODO: replace a VAO binding stack!
        // TODO: deferred vertex pointer system!
        var vBinding = 0;
        cache.stride = stride;
        GL43.glVertexAttribBinding(index, cache.bindingIndex = vBinding);
        GL43.glVertexAttribIFormat(index, size, type, (int) pointer);

        //
        if (cache.target == GL_ARRAY_BUFFER && cache.stride > 0 && cache.size > 0) {
            glBindVertexBuffer(cache.bindingIndex, cache.glStorageBuffer, cache.offset.get(0), cache.stride);
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _drawElements(int mode, int count, int type, long indices) throws Exception {
        RenderSystem.assertOnRenderThread();
        var cache = GlContext.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER);

        // TODO: workaround by shaders!!!
        /*
        var EL = GL45.glCreateBuffers();
        GL45.glNamedBufferData(EL, cache.size, GL_DYNAMIC_DRAW);
        GL45.glCopyNamedBufferSubData(cache.glStorageBuffer, EL, cache.offset.get(0), 0, cache.size);
        GL20.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EL);
        GL11.glDrawElements(mode, count, type, 0L);
        GL20.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cache.glStorageBuffer);
        GL45.glDeleteBuffers(EL);*/

        //
        GL20.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cache.glStorageBuffer);
        GL11.glDrawElements(mode, count, type, indices + cache.offset.get(0));
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static int _glGenBuffers() throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlContext.glCreateBuffer();
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
    public static void _glBufferData(int target, ByteBuffer data, int usage) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlContext.glBufferData(target, data, usage);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glBufferData(int target, long size, int usage) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlContext.glBufferData(target, size, usage);
    }

    /**
     * @author
     * @reason
     */

    // TODO: needs Vulkan API synchronization!!!
    @Nullable @Overwrite
    public static ByteBuffer mapBuffer(int target, int access) {
        RenderSystem.assertOnRenderThreadOrInit();
        //return GL15.glMapBuffer(target, access);

        var cache = GlContext.boundBuffers.get(target);

        // required for map ops
        //GL45.glMemoryBarrier(GL_ELEMENT_ARRAY_BARRIER_BIT|GL_BUFFER_UPDATE_BARRIER_BIT|GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
        if (cache.allocatedMemory == null) {
            System.out.println("Used Vulkan Mapped Memory, Synchronization May Required!");
            return (cache.allocatedMemory = cache.map(cache.allocCreateInfo.size(), 0L));
        }
        return cache.allocatedMemory;
        //return (cache.allocatedMemory = memAlloc((int) cache.allocCreateInfo.size()));
    }

    /**
     * @author
     * @reason
     */

    // TODO: needs Vulkan API synchronization!!!
    @Overwrite
    public static void _glUnmapBuffer(int target) {
        RenderSystem.assertOnRenderThreadOrInit();

        var cache = GlContext.boundBuffers.get(target);
        if (cache.allocatedMemory != null) {
            //cache.unmap();

            // required for map ops
            //GL45.glMemoryBarrier(GL_ELEMENT_ARRAY_BARRIER_BIT|GL_BUFFER_UPDATE_BARRIER_BIT|GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);

            //
            //GL45.glNamedBufferSubData(cache.glStorageBuffer, cache.offset.get(0), cache.allocatedMemory);
            //cache.allocatedMemory = null;
        }
    }

    //
    @Shadow(remap = false) private static boolean ON_LINUX;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glDeleteBuffers(int glVirtualBuffer) throws Exception {
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
            resource.size = 0L; resource.offset.put(0, 0L);
            GlContext.resourceCacheMap.removeMem(resource);
        }
        //GL15.glDeleteBuffers(buffer);
    }

    //
    @Redirect(remap = false, method="_deleteTexture", at=@At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures(I)V"))
    private static void onDeleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
        GlContext.ResourceImage resource = GlContext.resourceImageMap.get(texture);
        if (resource != null) { GlContext.resourceImageMap.remove(texture); }
    }

    //
    @Redirect(remap = false, method="_deleteTextures", at=@At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures([I)V"))
    private static void onDeleteTextures(int textures[]) {
        GL11.glDeleteTextures(textures);
        for (var I=0;I<textures.length;I++) {
            GlContext.ResourceImage resource = GlContext.resourceImageMap.get(textures[I]);
            if (resource != null) { GlContext.resourceImageMap.remove(textures[I]); }
        }
    }


}
