package org.hydra2s.manhack.virtual.buffer;

//
import org.hydra2s.manhack.GlContext;
import org.lwjgl.opengl.GL45;

//
import java.io.IOException;
import java.nio.ByteBuffer;

//
import static org.lwjgl.opengl.GL15.*;

// OpenGL direct version
public class GlDirectVirtualBuffer implements GlBaseVirtualBuffer {

    public static class VirtualBufferObj extends GlBaseVirtualBuffer.VirtualBufferObj {
        // TODO: needs array mapping or not? (i.e. indexed wrapper or virtual ID)
        public VirtualBufferObj() {
            super();
            this.glStorageBuffer = GL45.glCreateBuffers();
        }

        @Override
        public ByteBuffer map(int target, int access) throws Exception {
            this.assert_();
            return (this.allocatedMemory = GL45.glMapNamedBuffer(this.glStorageBuffer, access));
            //return (this.allocatedMemory = GL45.glMapBuffer(target, access));
        }

        @Override
        public void unmap(int target) throws Exception {
            this.assert_();
            GL45.glUnmapNamedBuffer(this.glStorageBuffer);
            //GL45.glUnmapBuffer(target);
            this.allocatedMemory = null;
        }

        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj data(int target, ByteBuffer data, int usage) throws Exception {
            this.assert_();
            this.realSize = data.remaining();
            GL45.glNamedBufferData(this.glStorageBuffer, data, usage);
            return this;
        }

        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj data(int target, long size, int usage) throws Exception {
            this.assert_();
            GL45.glNamedBufferData(this.glStorageBuffer, this.realSize = size, usage);
            return this;
        }

        @Override
        public void delete() throws Exception {
            this.assert_();
            GlContext.virtualBufferMap.removeMem(this.deallocate());
            glDeleteBuffers(this.glStorageBuffer);
            this.glVirtualBuffer = -1;
            this.glStorageBuffer = -1;
            this.realSize = 0;
            this.blockSize = 0;
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
