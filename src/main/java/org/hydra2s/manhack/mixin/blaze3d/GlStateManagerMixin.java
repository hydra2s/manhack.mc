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
            var cache = GlVirtualBufferSystem.assertVirtualBuffer(GlVirtualBufferSystem.boundBuffers.get(GL_ARRAY_BUFFER));
            cache.vao = bound;
            cache.stride = stride;

            GL45.glVertexArrayAttribBinding(cache.vao, index, cache.bindingIndex = vBinding);
            GL45.glVertexArrayAttribFormat(cache.vao, index, size, type, normalized, (int) pointer);
            GlVirtualBufferSystem.glBindVertexBuffer(cache);
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
            var cache = GlVirtualBufferSystem.assertVirtualBuffer(GlVirtualBufferSystem.boundBuffers.get(GL_ARRAY_BUFFER));
            cache.vao = bound;
            cache.stride = stride;
            GL45.glVertexArrayAttribBinding(cache.vao, index, cache.bindingIndex = vBinding);
            GL45.glVertexArrayAttribIFormat(cache.vao, index, size, type, (int) pointer);
            GlVirtualBufferSystem.glBindVertexBuffer(cache);
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
        var cache = GlVirtualBufferSystem.assertVirtualBuffer(GlVirtualBufferSystem.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER));

        //System.out.println("Used Virtual Index Buffer: " + cache.glVirtualBuffer + ", With offset: " + cache.offset.get(0));

        GL11.glDrawElements(mode, count, type, indices + cache.offset.get(0));
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite(remap = false)
    public static int _glGenBuffers() throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlVirtualBufferSystem.glCreateBuffer();
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _glBindBuffer(int target, int glVirtualBuffer) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlVirtualBufferSystem.glBindBuffer(target, glVirtualBuffer);
    }*/

    /**
     * @author
     * @reason
     */
    /*@Overwrite
    public static void _glBufferData(int target, ByteBuffer data, int usage) throws Exception {
        RenderSystem.assertOnRenderThreadOrInit();
        GlVirtualBufferSystem.glBufferData(target, data, usage);
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

        var cache = GlVirtualBufferSystem.assertVirtualBuffer(GlVirtualBufferSystem.boundBuffers.get(target));
        System.out.println("Used Vulkan Mapped Memory, Synchronization May Required!");
        return (cache.allocatedMemory = cache.map(target, access, cache.allocCreateInfo.size(), 0L));
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
        GlVirtualBufferSystem.glDeleteBuffer(glVirtualBuffer);
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
