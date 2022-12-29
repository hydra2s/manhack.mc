package org.hydra2s.manhack.virtual.buffer;

//
import org.hydra2s.manhack.interfaces.GlBaseVirtualBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL45;

//
import java.io.IOException;
import java.nio.ByteBuffer;

//
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;

// OpenGL direct version
public class GlDirectVirtualBuffer implements GlBaseVirtualBuffer {
    public static class VirtualBufferObj extends GlBaseVirtualBuffer.VirtualBufferObj {
        // TODO: needs array mapping or not? (i.e. indexed wrapper or virtual ID)
        public VirtualBufferObj() {
            super();
            this.glStorageBuffer = GL45.glCreateBuffers();
            //virtualBufferMap.hashMap.put(this.glVirtualBuffer = this.glStorageBuffer, this);

            //
            this.glVirtualBuffer = virtualBufferMap.arrayMap.push(this);
            System.out.println("Generated New Virtual Buffer! Id: " + this.glVirtualBuffer);
        }

        @Override
        public ByteBuffer map(int target, int access, long vkWholeSize, long i) {
            return (this.allocatedMemory = GL45.glMapNamedBuffer(this.glVirtualBuffer, access));
        }

        @Override
        public ByteBuffer map(int target, int access) {
            return (this.allocatedMemory = GL45.glMapNamedBuffer(this.glVirtualBuffer, access));
        }

        @Override
        public void unmap(int target) {
            GL45.glUnmapBuffer(this.glVirtualBuffer);
            this.allocatedMemory = null;
        }

        @Override
        public VirtualBufferObj assert_() throws Exception {
            if (this == null || this.glVirtualBuffer <= 0 || this == null) {
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
            virtualBufferMap.removeMem(this.deallocate());
            boundBuffers.remove(this.target);
            System.out.println("Deleted Virtual Buffer! Id: " + this.glVirtualBuffer);
            this.glVirtualBuffer = -1;
            glDeleteBuffers(this.glStorageBuffer);
            this.glStorageBuffer = -1;
        }
    }

    //
    public static void initialize() throws IOException {

    };

    //
    public static int createVirtualBuffer() throws Exception {
        return (new VirtualBufferObj()).glVirtualBuffer;
    }

}
