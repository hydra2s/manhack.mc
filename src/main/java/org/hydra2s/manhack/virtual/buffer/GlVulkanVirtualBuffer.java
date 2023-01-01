package org.hydra2s.manhack.virtual.buffer;

//
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedBuffer;

//
import java.io.IOException;
import java.nio.ByteBuffer;

//
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL30.GL_R8UI;
import static org.lwjgl.opengl.GL30.GL_RED_INTEGER;
import static org.lwjgl.opengl.GL45.glClearNamedBufferSubData;
import static org.lwjgl.opengl.GL45.glNamedBufferSubData;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

//
public class GlVulkanVirtualBuffer implements GlBaseVirtualBuffer {
    //
    public static class VirtualBufferObj extends GlBaseVirtualBuffer.VirtualBufferObj {
        public GlVulkanSharedBuffer.VkSharedBuffer mapped = null;

        //
        public VirtualBufferObj() {
            super();

            // TODO: support for typed (entity, indexed, blocks, etc.)
            if ((this.mapped = GlVulkanSharedBuffer.sharedBufferMap.get(0)) != null) {
                this.glStorageBuffer = this.mapped.glStorageBuffer;
            }
        }

        public VirtualBufferObj(int typed) {
            super();

            // TODO: support for typed (entity, indexed, blocks, etc.)
            if ((this.mapped = GlVulkanSharedBuffer.sharedBufferMap.get(typed)) != null) {
                this.glStorageBuffer = this.mapped.glStorageBuffer;
            }
        }

        @Override
        public ByteBuffer map(int target, int access) {
            return (this.allocatedMemory = this.mapped.obj.map(this.realSize, offset.get(0)));
        }

        @Override
        public void unmap(int target) {
            this.mapped.obj.unmap();
            this.allocatedMemory = null;
        }

        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj deallocate() throws Exception {
            this.assert_();
            if (this.allocId.get(0) != 0) {
                if (this.glStorageBuffer > 0) {
                    glClearNamedBufferSubData(this.glStorageBuffer, GL_R8UI, this.offset.get(0), this.blockSize, GL_RED_INTEGER, GL_UNSIGNED_BYTE, memAlloc(1).put(0, (byte) 0));
                }

                //
                this.blockSize = 0L;
                this.offset.put(0, 0L);

                //
                if (this.mapped != null) {
                    vmaVirtualFree(this.mapped.vb.get(0), this.allocId.get(0));
                }

                this.address = 0L;
                this.allocId.put(0, 0L);
            }
            return this;
        }

        //
        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj allocate(long defaultSize, int usage) throws Exception {
            long MEM_BLOCK = 1024L * 3L;
            defaultSize = roundUp(defaultSize, MEM_BLOCK) * MEM_BLOCK;
            if (this.assert_().blockSize < defaultSize)
            {
                //System.out.println("WARNING! Size of virtual buffer was changed! " + this.blockSize + " != " + defaultSize);
                //System.out.println("Virtual GL buffer ID: " + this.glVirtualBuffer);

                // TODO: recopy to new chunk
                deallocate();

                //
                int res = vmaVirtualAllocate(this.mapped.vb.get(0), this.allocCreateInfo.size(this.blockSize = defaultSize), this.allocId.put(0, 0L), this.offset.put(0, 0L));
                if (res != VK_SUCCESS) {
                    System.out.println("Allocation Failed: " + res);
                    throw new Exception("Allocation Failed: " + res);
                }

                // get device address from 
                this.address = this.mapped.obj.getDeviceAddress() + this.offset.get(0);

                //
                //System.out.println("Virtual buffer Size Changed: " + this.blockSize);
            }
            return this;
        }

        // Will used for copying into special buffer
        @Override // TODO: replace to Vulkan in future...
        public GlBaseVirtualBuffer.VirtualBufferObj data(int target, ByteBuffer data, int usage) throws Exception {
            this.realSize = data.remaining();
            if (this.glStorageBuffer > 0) {
                glNamedBufferSubData(this.glStorageBuffer, this.offset.get(0), data);
            }
            return this.bindVertex();
        }

        // Will used for copying into special buffer
        @Override // TODO: replace to Vulkan in future...
        public GlBaseVirtualBuffer.VirtualBufferObj data(int target, long size, int usage) throws Exception {
            this.realSize = size;
            if (this.glStorageBuffer > 0) {
                glClearNamedBufferSubData(this.glStorageBuffer, GL_R8UI, this.offset.get(0), this.realSize = size, GL_RED_INTEGER, GL_UNSIGNED_BYTE, memAlloc(1).put(0, (byte) 0));
            }
            return this.bindVertex();
        }
    }

    //
    public static void initialize() throws IOException {

    };

    //
    public static int createVirtualBuffer() throws Exception {
        return (new VirtualBufferObj()).glVirtualBuffer;
    }

};
