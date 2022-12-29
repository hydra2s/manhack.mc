package org.hydra2s.manhack;

//
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
    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static HashMap<Integer, VirtualBufferObj> virtualBufferMap = new HashMap<>();

    //
    public static class VirtualBufferObj extends GlBufferSystem.VirtualBufferObj {
        //
        public VirtualBufferObj() {
            super();
            virtualBufferMap.put(this.glVirtualBuffer = GL45.glCreateBuffers(), this);
            this.glStorageBuffer = this.glVirtualBuffer;
            System.out.println("Generated New Virtual Buffer! Id: " + this.glVirtualBuffer);
        }

        @Override
        public ByteBuffer map(int target, int access, long vkWholeSize, long i) {
            return (this.allocatedMemory = GL45.glMapNamedBuffer(this.glVirtualBuffer, access));
        }

        @Override
        public void unmap(int target) {
            GL45.glUnmapBuffer(this.glVirtualBuffer);
            this.allocatedMemory = null;
        }

        @Override
        public VirtualBufferObj assert_() throws Exception {
            if (this == null || this.glVirtualBuffer <= 0 || virtualBufferMap.get(this) == null) {
                System.out.println("Wrong Virtual Buffer Id! " + (this != null ? this.glVirtualBuffer : -1));
                throw new Exception("Wrong Virtual Buffer Id! " + (this != null ? this.glVirtualBuffer : -1));
            }
            return this;
        }

        @Override
        public VirtualBufferObj bindVertex() {
            if (this.target == GL_ARRAY_BUFFER && this.stride > 0 && this.size > 0 && this.vao > 0) {
                GL45.glVertexArrayVertexBuffer(this.vao, this.bindingIndex, this.glStorageBuffer, this.offset.get(0), this.stride);
                // TODO: fix calling spam by VAO objects
            }
            return this;
        }

        @Override
        public VirtualBufferObj allocate(long defaultSize, int usage) throws Exception {
            return this;
        }

        @Override
        public VirtualBufferObj deallocate() throws Exception {
            return this;
        }

        @Override
        public VirtualBufferObj data(int target, ByteBuffer data, int usage) {
            GL45.glNamedBufferData(target, data, usage);
            return this;
        }

        @Override
        public VirtualBufferObj bind(int target) {
            GL20.glBindBuffer(target, 0);
            boundBuffers.remove(target);

            // TODO: unbound memory
            if (target == GL_ARRAY_BUFFER) {
                this.vao = this.vao > 0 ? this.vao : glGetInteger(GL_VERTEX_ARRAY_BINDING);
            }

            // TODO: unbound memory
            boundBuffers.put(this.target = target, this);
            GL20.glBindBuffer(target, this.glStorageBuffer);
            return this.bindVertex();
        }

        @Override
        public void delete() throws Exception {
            virtualBufferMap.remove(this.deallocate());
            boundBuffers.remove(this.target);
            System.out.println("Deleted Virtual Buffer! Id: " + this.glVirtualBuffer);
            this.glVirtualBuffer = -1;
            glDeleteBuffers(this.glStorageBuffer);
            this.glStorageBuffer = -1;
        }
    }




    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static void initialize() throws IOException {
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static int glCreateVirtualBuffer() throws Exception {
        return (new VirtualBufferObj()).glVirtualBuffer;
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glAllocateVirtualBuffer(int target, long defaultSize, int usage) throws Exception {
        return boundBuffers.get(target).allocate(defaultSize, usage);
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glBindVirtualBuffer(int target, int glVirtual) throws Exception {
        return virtualBufferMap.get(glVirtual).bind(target);
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glDeallocateVirtualBuffer(int glVirtualBuffer) throws Exception {
        return virtualBufferMap.get(glVirtualBuffer).deallocate();
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static void glDeleteVirtualBuffer(int glVirtualBuffer) throws Exception {
        virtualBufferMap.get(glVirtualBuffer).delete();
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glVirtualBufferData(int target, long data, int usage) throws Exception {
        return boundBuffers.get(target).allocate(data, usage).bindVertex();
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glVirtualBufferData(int target, ByteBuffer data, int usage) throws Exception {
        return (VirtualBufferObj) boundBuffers.get(target).allocate(data.remaining(), usage).data(target, data, usage).bindVertex();
    }

}
