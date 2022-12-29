package org.hydra2s.manhack.mixin.blaze3d;

//
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.hydra2s.manhack.interfaces.GlBaseVirtualBuffer;
import org.hydra2s.manhack.virtual.buffer.GlVulkanVirtualBuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL45;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;

//
// TODO: connect with virtual GL system!
@Mixin(GlStateManager.class)
public class GlStateManagerMixin {

    // Override Buffer System! //
    // Override Vertex System! //

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) throws Exception {
        RenderSystem.assertOnRenderThread();

        // TODO: needs to replace to external
        var glVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);

        //
        var vBinding = 0;//index;
        var VBO = GlBaseVirtualBuffer.boundBuffers.get(GL_ARRAY_BUFFER);
        VBO.vao = glVao;
        VBO.stride = stride;

        //
        GL45.glVertexArrayAttribBinding(VBO.vao, index, VBO.bindingIndex = vBinding);
        GL45.glVertexArrayAttribFormat(VBO.vao, index, size, type, normalized, (int) pointer);
        VBO.bindVertex();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _vertexAttribIPointer(int index, int size, int type, int stride, long pointer) throws Exception {
        RenderSystem.assertOnRenderThread();

        // TODO: needs to replace to external
        var glVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);

        //
        var vBinding = 0;//index;
        var VBO = GlBaseVirtualBuffer.boundBuffers.get(GL_ARRAY_BUFFER);
        VBO.vao = glVao;
        VBO.stride = stride;

        //
        GL45.glVertexArrayAttribBinding(VBO.vao, index, VBO.bindingIndex = vBinding);
        GL45.glVertexArrayAttribIFormat(VBO.vao, index, size, type, (int) pointer);
        VBO.bindVertex();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _drawElements(int mode, int count, int type, long indices) throws Exception {
        RenderSystem.assertOnRenderThread();

        // Here is problems...
        // TODO: replace draw elements by host memory operations
        var VBO = GlBaseVirtualBuffer.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER);
        GL11.glDrawElements(mode, count, type, indices + VBO.offset.get(0));
    }



    // Override Buffer System! //

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static int _glGenBuffers() throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlVulkanVirtualBuffer.createVirtualBuffer();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glBindBuffer(int target, int glVirtualBuffer) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBaseVirtualBuffer.bindVirtualBuffer(target, glVirtualBuffer);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glBufferData(int target, ByteBuffer data, int usage) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBaseVirtualBuffer.virtualBufferData(target, data, usage);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public static void _glBufferData(int target, long size, int usage) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBaseVirtualBuffer.virtualBufferData(target, size, usage);
    }

    /**
     * @author
     * @reason
     */
    @Nullable
    @Overwrite
    public static ByteBuffer mapBuffer(int target, int access) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlBaseVirtualBuffer.boundBuffers.get(target).map(target, access);
    }

    /**
     * @author
     * @reason
     */

    // TODO: needs Vulkan API synchronization!!!
    @Overwrite
    public static void _glUnmapBuffer(int target) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBaseVirtualBuffer.boundBuffers.get(target).unmap(target);
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
        GlBaseVirtualBuffer.virtualBufferMap.get(glVirtualBuffer).delete();
    }




    // Override Texture System!  //
    // TODO: Fix Texture System! //

    //
    /*
    @Redirect(remap = false, method="_deleteTexture", at=@At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures(I)V"))
    private static void onDeleteTexture(int glTex) {
        GL11.glDeleteTextures(glTex);

        //
        GlSharedTextureSystem.VkSharedImage image = GlSharedTextureSystem.imageMap.get(glTex);
        if (image != null) { GlSharedTextureSystem.imageMap.remove(glTex); }
    }

    //
    @Redirect(remap = false, method="_deleteTextures", at=@At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures([I)V"))
    private static void onDeleteTextures(int glTex[]) {
        GL11.glDeleteTextures(glTex);

        //
        for (var I=0;I<glTex.length;I++) {
            GlSharedTextureSystem.VkSharedImage image = GlSharedTextureSystem.imageMap.get(glTex[I]);
            if (image != null) { GlSharedTextureSystem.imageMap.remove(glTex[I]); }
        }
    }
    */

}
