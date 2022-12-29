package org.hydra2s.manhack;

import org.hydra2s.noire.objects.PipelineLayoutObj;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL45;

//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

//
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;

// TODO! Replace most GL calls to more modern OpenGL API (with DSA support)
public class GlBetterBufferSystem implements GlBufferSystem {
    public static HashMap<Integer, VirtualBufferObj> virtualBufferMap = new HashMap<>();
    public static Map<Integer, VirtualBufferObj> boundBuffers = new HashMap<Integer, VirtualBufferObj>() {{

    }};

    public static class VirtualBufferObj extends GlBufferSystem.VirtualBufferObj {
        //
        public VirtualBufferObj() {
            super();
            virtualBufferMap.put(this.glVirtualBuffer = GL45.glCreateBuffers(), this);
            this.glStorageBuffer = this.glVirtualBuffer;
        }

        //
        public ByteBuffer map(int target, int access, long vkWholeSize, long i) {
            return (this.allocatedMemory = GL45.glMapNamedBuffer(this.glVirtualBuffer, access));
        }

        public void unmap(int target) {
            GL45.glUnmapBuffer(this.glVirtualBuffer);
            this.allocatedMemory = null;
        }
    }

    // TODO: make object bound op
    public static VirtualBufferObj assertVirtualBufferObj(VirtualBufferObj VBO) throws Exception {
        if (VBO == null || VBO.glVirtualBuffer <= 0 || virtualBufferMap.get(VBO) == null) {
            System.out.println("Wrong Virtual Buffer Id! " + (VBO != null ? VBO.glVirtualBuffer : -1));
            throw new Exception("Wrong Virtual Buffer Id! " + (VBO != null ? VBO.glVirtualBuffer : -1));
        }
        return VBO;
    }

    //
    public static void initialize() throws IOException {

    };

    //
    public static int glCreateVirtualBuffer() throws Exception {
        var VBO = assertVirtualBufferObj(new VirtualBufferObj());
        System.out.println("Generated New Virtual Buffer! Id: " + VBO.glVirtualBuffer);
        return VBO.glVirtualBuffer;
    };

    // TODO: make object bound op
    public static VirtualBufferObj glBindVirtualVertexBuffer(VirtualBufferObj VBO) {
        if (VBO.target == GL_ARRAY_BUFFER && VBO.stride > 0 && VBO.size > 0 && VBO.vao > 0) {
            GL45.glVertexArrayVertexBuffer(VBO.vao, VBO.bindingIndex, VBO.glStorageBuffer, VBO.offset.get(0), VBO.stride);
            // TODO: fix calling spam by VAO objects
        }
        return VBO;
    }

    // TODO: make object bound op
    public static VirtualBufferObj glAllocateVirtualBuffer(VirtualBufferObj VBO, long defaultSize, int usage) throws Exception {
        return VBO;
    }

    // TODO: make object bound op
    public static VirtualBufferObj glAllocateVirtualBuffer(int target, long defaultSize, int usage) throws Exception {
        return glAllocateVirtualBuffer(boundBuffers.get(target), defaultSize, usage);
    }

    // TODO: make object bound op
    public static void glBindVirtualBuffer(int target, int glVirtual) throws Exception {
        GL20.glBindBuffer(target, 0);
        boundBuffers.remove(target);

        // TODO: unbound memory
        var VBO = assertVirtualBufferObj((VirtualBufferObj) virtualBufferMap.get(glVirtual));
        if (target == GL_ARRAY_BUFFER) {
            VBO.vao = VBO.vao > 0 ? VBO.vao : glGetInteger(GL_VERTEX_ARRAY_BINDING);
        }

        // TODO: unbound memory
        boundBuffers.put(VBO.target = target, VBO);
        GL20.glBindBuffer(target, VBO.glStorageBuffer);
        glBindVirtualVertexBuffer(VBO);
    }

    // TODO: make object bound op
    public static VirtualBufferObj glDeallocateVirtualBuffer(VirtualBufferObj resource) throws Exception {
        return resource;
    }

    public static VirtualBufferObj glDeallocateVirtualBuffer(int glVirtualBuffer) throws Exception {
        return glDeallocateVirtualBuffer(assertVirtualBufferObj(virtualBufferMap.get(glVirtualBuffer)));
    }

    // TODO: make object bound op
    public static void glDeleteVirtualBuffer(int glVirtualBuffer) throws Exception {
        VirtualBufferObj resource = assertVirtualBufferObj(virtualBufferMap.get(glVirtualBuffer));
        virtualBufferMap.remove(glDeallocateVirtualBuffer(resource));
        boundBuffers.remove(resource.target);
        System.out.println("Deleted Virtual Buffer! Id: " + resource.glVirtualBuffer);
        resource.glVirtualBuffer = -1;
        glDeleteBuffers(resource.glStorageBuffer);
        resource.glStorageBuffer = -1;
    }

    // TODO: make object bound op
    public static VirtualBufferObj glVirtualBufferData(int target, long data, int usage) throws Exception {
        var VBO = assertVirtualBufferObj(boundBuffers.get(target));
        GL45.glNamedBufferData(VBO.glStorageBuffer, VBO.size = data, usage);
        glBindVirtualVertexBuffer(VBO);
        return VBO;
    }

    // TODO: make object bound op
    public static VirtualBufferObj glVirtualBufferData(int target, ByteBuffer data, int usage) throws Exception {
        var VBO = assertVirtualBufferObj(boundBuffers.get(target));
        GL45.glNamedBufferData(VBO.glStorageBuffer, data, usage);
        VBO.size = data.remaining();
        glBindVirtualVertexBuffer(VBO);
        return VBO;
    }

}
