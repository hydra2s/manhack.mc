package org.hydra2s.manhack.virtual.buffer;

//
import org.hydra2s.manhack.interfaces.GlBaseVirtualBuffer;
import org.hydra2s.manhack.vulkan.GlVulkanSharedBuffer;

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
    public static VirtualBufferObj dummyCache = new VirtualBufferObj();

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

        @Override
        public ByteBuffer map(int target, int access) {
            return (this.allocatedMemory = this.mapped.obj.map(this.size, offset.get(0)));
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
            return this;
        }

        //
        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj allocate(long defaultSize, int usage) throws Exception {
            if (this.assert_().size != defaultSize)
            {
                System.out.println("WARNING! Size of virtual buffer was changed! " + this.size + " != " + defaultSize);
                System.out.println("Virtual GL buffer ID: " + this.glVirtualBuffer);

                //
                deallocate();

                //
                int res = vmaVirtualAllocate(this.mapped.vb.get(0), this.allocCreateInfo.size(this.size = defaultSize), this.allocId.put(0, 0L), this.offset.put(0, 0L));
                if (res != VK_SUCCESS) {
                    System.out.println("Allocation Failed: " + res);
                    throw new Exception("Allocation Failed: " + res);
                }

                //
                System.out.println("Virtual buffer Size Changed: " + this.size);
            }
            return this;
        }

        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj data(int target, ByteBuffer data, int usage) throws Exception {
            if (this.glStorageBuffer > 0) {
                glNamedBufferSubData(this.glStorageBuffer, this.offset.get(0), data);
            }
            this.size = data.remaining();
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
