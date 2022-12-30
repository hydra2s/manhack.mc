package org.hydra2s.manhack.virtual.buffer;

//
import org.hydra2s.manhack.interfaces.GlBaseVirtualBuffer;
import org.hydra2s.manhack.opengl.GlDirectSharedBuffer;

//
import java.nio.ByteBuffer;

//
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.vmaVirtualAllocate;
import static org.lwjgl.util.vma.Vma.vmaVirtualFree;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

// This is memory, long time holding on host memory
// Always mapped! Can't to be unmapped (will ignored).
// Uses virtual allocation mostly.
public class GlHostVirtualBuffer implements GlBaseVirtualBuffer {

    // Simulate Vulkan Virtual Buffer Behaviour
    // Originally, planned to use only on host
    // GPU have no much memory space (except most expensive)
    // OpenGL doesn't support host-based memory by default
    // That feature is supported only by Vulkan API
    public static final boolean TO_BE_GL = true;

    //
    public static class VirtualBufferObj extends GlBaseVirtualBuffer.VirtualBufferObj {
        public GlDirectSharedBuffer.GlSharedBuffer mapped = null;

        // TODO: needs array mapping or not? (i.e. indexed wrapper or virtual ID)
        public VirtualBufferObj() {
            super();

            // TODO: support for typed (entity, indexed, blocks, etc.)
            if ((this.mapped = GlDirectSharedBuffer.sharedBufferMap.get(0)) != null) {
                this.glStorageBuffer = this.mapped.glStorageBuffer;
            }
        }

        @Override
        public ByteBuffer map(int target, int access) throws Exception {
            this.assert_();
            if (TO_BE_GL) {
                var offset = this.offset.get(0);
                if (this.glStorageBuffer > 0 && this.size > 0 && this.offset.get(0) >= 0) {
                    return memSlice((this.allocatedMemory = glMapNamedBuffer(this.glStorageBuffer, access)), (int) this.offset.get(0), (int) this.size);
                }
                //return (this.allocatedMemory = glMapNamedBufferRange(this.glStorageBuffer, offset, this.size, access));
            }
            return this.allocatedMemory;
        }

        @Override
        public void unmap(int target) { // ignored
            if (TO_BE_GL) {
                if (this.glStorageBuffer > 0) {
                    glUnmapNamedBuffer(this.glStorageBuffer);
                }
                this.allocatedMemory = null;
            }
        }

        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj allocate(long defaultSize, int usage) throws Exception {
            if (this.allocatedMemory == null || defaultSize != this.size) {
                this.deallocate();
                this.allocatedMemory = memAlloc((int) (this.size = defaultSize));

                // TODO: temp-alloc
                int res = vmaVirtualAllocate(this.mapped.vb.get(0), this.allocCreateInfo.size(this.size = defaultSize), this.allocId.put(0, 0L), this.offset.put(0, 0L));
                if (res != VK_SUCCESS) {
                    System.out.println("Allocation Failed: " + res);
                    throw new Exception("Allocation Failed: " + res);
                }
            }
            return this;
        }

        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj deallocate() throws Exception {
            this.assert_();
            if (this.allocatedMemory != null) {
                memFree(this.allocatedMemory);
            }

            //
            if (this.allocId.get(0) != 0) {
                if (this.glStorageBuffer > 0) {
                    glClearNamedBufferSubData(this.glStorageBuffer, GL_R8UI, this.offset.get(0), this.size, GL_RED_INTEGER, GL_UNSIGNED_BYTE, memAlloc(1).put(0, (byte) 0));
                }

                //
                this.size = 0L;
                this.offset.put(0, 0L);

                //
                if (this.mapped != null) {
                    vmaVirtualFree(this.mapped.vb.get(0), this.allocId.get(0));
                }

                this.allocId.put(0, 0L);
            }

            //
            return this;
        }

        // prefer a zero copy system
        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj data(int target, ByteBuffer data, int usage) throws Exception {
            if (TO_BE_GL) {
                if (this.glStorageBuffer > 0) {
                    glNamedBufferSubData(this.glStorageBuffer, this.offset.get(0), data);
                }
            } else {
                //this.allocatedMemory = data;
                memCopy(data, this.assert_().allocatedMemory);
            }
            return this.bindVertex();
        }

        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj preDraw() throws Exception {
            this.assert_();
            //System.out.println("Buffer Data Capacity: " + this.size);
            if ((!TO_BE_GL && this.allocatedMemory != null) && this.glStorageBuffer > 0) {
                glNamedBufferSubData(this.glStorageBuffer, this.offset.get(0), this.allocatedMemory);
            }
            return this;//this.bindVertex();
        }

        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj postDraw() throws Exception {
            this.assert_();
            return this;
        }
    }

    //
    public static int createVirtualBuffer() throws Exception {
        return (new VirtualBufferObj()).glVirtualBuffer;
    }
}
