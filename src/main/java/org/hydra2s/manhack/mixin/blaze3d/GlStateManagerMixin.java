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
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
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
    @Overwrite(remap = false)
    public static int _glGenVertexArrays() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GL45.glCreateVertexArrays();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glBindVertexArray(int array) {
        RenderSystem.assertOnRenderThreadOrInit();
        GL30.glBindVertexArray(array);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) throws Exception {
        RenderSystem.assertOnRenderThread();

        //
        var bound = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        /*
        System.out.println("Used VAO: " + bound);
        System.out.println("Used override binding system!");
        System.out.println("Called `_vertexAttribPointer`:");
        System.out.println("Arg0 (index): " + index);
        System.out.println("Arg1 (size): " + size);
        System.out.println("Arg2 (type): " + type);
        System.out.println("Arg3 (normalized): " + normalized);
        System.out.println("Arg4 (stride): " + stride);
        System.out.println("Arg5 (pointer): " + pointer);*/

        // TODO: replace a VAO binding stack!
        // TODO: deferred vertex pointer system!
        //if (GlContext.VGL_VERSION_A)
        {
            var vBinding = 0;//index;
            var cache = GlContext.assertVirtualBuffer(GlContext.boundBuffers.get(GL_ARRAY_BUFFER));
            cache.vao = bound;
            cache.stride = stride;

            GL45.glVertexArrayAttribBinding(cache.vao, index, cache.bindingIndex = vBinding);
            GL45.glVertexArrayAttribFormat(cache.vao, index, size, type, normalized, (int) pointer);
            GlContext.glBindVertexBuffer(cache);
        }
        //else {
            //GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        //}

    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _vertexAttribIPointer(int index, int size, int type, int stride, long pointer) throws Exception {
        RenderSystem.assertOnRenderThread();

        //
        var bound = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        /*
        System.out.println("Used VAO: " + bound);
        System.out.println("Used override binding system!");
        System.out.println("Called `_vertexAttribIPointer`:");
        System.out.println("Arg0 (index): " + index);
        System.out.println("Arg1 (size): " + size);
        System.out.println("Arg2 (type): " + type);
        System.out.println("Arg3 (stride): " + stride);
        System.out.println("Arg4 (pointer): " + pointer);*/

        // TODO: replace a VAO binding stack!
        // TODO: deferred vertex pointer system!
        //if (GlContext.VGL_VERSION_A)
        {
            var vBinding = 0;//index;
            var cache = GlContext.assertVirtualBuffer(GlContext.boundBuffers.get(GL_ARRAY_BUFFER));
            cache.vao = bound;
            cache.stride = stride;
            GL45.glVertexArrayAttribBinding(cache.vao, index, cache.bindingIndex = vBinding);
            GL45.glVertexArrayAttribIFormat(cache.vao, index, size, type, (int) pointer);
            GlContext.glBindVertexBuffer(cache);
        }
        //else {
            //GL30.glVertexAttribIPointer(index, size, type, stride, pointer);
        //}

    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _drawElements(int mode, int count, int type, long indices) throws Exception {
        RenderSystem.assertOnRenderThread();

        // HERE IS PROBLEM!!! (`glBindBuffer` with `GL_ELEMENT_ARRAY_BUFFER`)
        // TODO: workaround by shaders!!!
        var cache = GlContext.assertVirtualBuffer(GlContext.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER));

        //System.out.println("Used Virtual Index Buffer: " + cache.glVirtualBuffer + ", With offset: " + cache.offset.get(0));

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
    public static void _glBindBuffer(int target, int glVirtualBuffer) throws Exception {
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
    public static ByteBuffer mapBuffer(int target, int access) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();

        var cache = GlContext.assertVirtualBuffer(GlContext.boundBuffers.get(target));
        System.out.println("Used Vulkan Mapped Memory, Synchronization May Required!");
        return (cache.allocatedMemory = cache.map(target, access, cache.allocCreateInfo.size(), 0L));
    }

    /**
     * @author
     * @reason
     */

    // TODO: needs Vulkan API synchronization!!!
    @Overwrite
    public static void _glUnmapBuffer(int target) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlContext.assertVirtualBuffer(GlContext.boundBuffers.get(target)).unmap(target);
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
        GlContext.glDeleteBuffer(glVirtualBuffer);
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
