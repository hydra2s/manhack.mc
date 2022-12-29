package org.hydra2s.manhack.mixin.blaze3d;

//
import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;

//
@Mixin(GlStateManager.class)
public class GlStateManagerMixin {

    /**
     * @author
     * @reason
     */
    /*@Overwrite(remap = false)
    public static int _glGenVertexArrays() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GL45.glCreateVertexArrays();
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _glBindVertexArray(int array) {
        RenderSystem.assertOnRenderThreadOrInit();
        GL30.glBindVertexArray(array);
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) throws Exception {
        RenderSystem.assertOnRenderThread();

        // TODO: replace a VAO binding stack!
        // TODO: deferred vertex pointer system!
        var bound = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        //if (GlContext.VGL_VERSION_A)
        {
            var vBinding = 0;//index;
            var VBO = GlVirtualBufferSystem.assertVirtualBuffer(GlVirtualBufferSystem.boundBuffers.get(GL_ARRAY_BUFFER));
            VBO.vao = bound;
            VBO.stride = stride;

            GL45.glVertexArrayAttribBinding(VBO.vao, index, VBO.bindingIndex = vBinding);
            GL45.glVertexArrayAttribFormat(VBO.vao, index, size, type, normalized, (int) pointer);
            GlVirtualBufferSystem.glBindVirtualVertexBuffer(VBO);
        }
        //else {
            //GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        //}
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _vertexAttribIPointer(int index, int size, int type, int stride, long pointer) throws Exception {
        RenderSystem.assertOnRenderThread();
        var bound = glGetInteger(GL_VERTEX_ARRAY_BINDING);

        // TODO: replace a VAO binding stack!
        // TODO: deferred vertex pointer system!
        //if (GlContext.VGL_VERSION_A)
        {
            var vBinding = 0;//index;
            var VBO = GlVirtualBufferSystem.assertVirtualBuffer(GlVirtualBufferSystem.boundBuffers.get(GL_ARRAY_BUFFER));
            VBO.vao = bound;
            VBO.stride = stride;
            GL45.glVertexArrayAttribBinding(VBO.vao, index, VBO.bindingIndex = vBinding);
            GL45.glVertexArrayAttribIFormat(VBO.vao, index, size, type, (int) pointer);
            GlVirtualBufferSystem.glBindVirtualVertexBuffer(VBO);
        }
        //else {
            //GL30.glVertexAttribIPointer(index, size, type, stride, pointer);
        //}

    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _drawElements(int mode, int count, int type, long indices) throws Exception {
        RenderSystem.assertOnRenderThread();

        // HERE IS PROBLEM!!! (`glBindBuffer` with `GL_ELEMENT_ARRAY_BUFFER`)
        // TODO: workaround by shaders!!!
        var VBO = GlVirtualBufferSystem.assertVirtualBuffer(GlVirtualBufferSystem.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER));

        //System.out.println("Used Virtual Index Buffer: " + VBO.glVirtualBuffer + ", With offset: " + VBO.offset.get(0));

        GL11.glDrawElements(mode, count, type, indices + VBO.offset.get(0));
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite(remap = false)
    public static int _glGenBuffers() throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlVirtualBufferSystem.glCreateVirtualBuffer();
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _glBindBuffer(int target, int glVirtualBuffer) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlVirtualBufferSystem.glBindVirtualBuffer(target, glVirtualBuffer);
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _glBufferData(int target, ByteBuffer data, int usage) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlVirtualBufferSystem.glVirtualBufferData(target, data, usage);
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _glBufferData(int target, long size, int usage) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlVirtualBufferSystem.glBufferData(target, size, usage);
    }*/

    /**
     * @author
     * @reason
     */

    // TODO: needs Vulkan API synchronization!!!
    /*@Nullable @Overwrite
    public static ByteBuffer mapBuffer(int target, int access) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();

        var VBO = GlVirtualBufferSystem.assertVirtualBuffer(GlVirtualBufferSystem.boundBuffers.get(target));
        System.out.println("Used Vulkan Mapped Memory, Synchronization May Required!");
        return (VBO.allocatedMemory = VBO.map(target, access, VBO.allocCreateInfo.size(), 0L));
    }*/

    /**
     * @author
     * @reason
     */

    // TODO: needs Vulkan API synchronization!!!
    /*@Overwrite
    public static void _glUnmapBuffer(int target) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlVirtualBufferSystem.assertVirtualBuffer(GlVirtualBufferSystem.boundBuffers.get(target)).unmap(target);
    }*/

    //
    //@Shadow(remap = false) private static boolean ON_LINUX;

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _glDeleteBuffers(int glVirtualBuffer) throws Exception {
        RenderSystem.assertOnRenderThread();
        if (ON_LINUX) {
            _glBindBuffer(GlConst.GL_ARRAY_BUFFER, glVirtualBuffer);
            _glBufferData(GlConst.GL_ARRAY_BUFFER, 0L, GlConst.GL_DYNAMIC_DRAW);
            _glBindBuffer(GlConst.GL_ARRAY_BUFFER, 0);
        }
        GlVirtualBufferSystem.glDeleteVirtualBuffer(glVirtualBuffer);
    }*/

    //
    /*@Redirect(remap = false, method="_deleteTexture", at=@At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures(I)V"))
    private static void onDeleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
        GlContext.ResourceImage resource = GlContext.resourceImageMap.get(texture);
        if (resource != null) { GlContext.resourceImageMap.remove(texture); }
    }*/

    //
    /*@Redirect(remap = false, method="_deleteTextures", at=@At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures([I)V"))
    private static void onDeleteTextures(int textures[]) {
        GL11.glDeleteTextures(textures);
        for (var I=0;I<textures.length;I++) {
            GlContext.ResourceImage resource = GlContext.resourceImageMap.get(textures[I]);
            if (resource != null) { GlContext.resourceImageMap.remove(textures[I]); }
        }
    }*/


}
