package org.hydra2s.manhack.virtual.buffer;

//
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedBuffer;
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.SwapChainCInfo;
import org.lwjgl.opengl.GL45;

//
import java.io.IOException;
import java.nio.ByteBuffer;

//
import static org.lwjgl.opengl.EXTSemaphore.glSignalSemaphoreEXT;
import static org.lwjgl.opengl.EXTSemaphore.glWaitSemaphoreEXT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL30.GL_R8UI;
import static org.lwjgl.opengl.GL30.GL_RED_INTEGER;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;
import static org.lwjgl.opengl.GL45.glClearNamedBufferSubData;
import static org.lwjgl.opengl.GL45.glNamedBufferSubData;
import static org.lwjgl.system.MemoryUtil.*;
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

            //
            //System.out.println("Generated New Host Virtual Buffer! Id: " + this.glVirtualBuffer);
        }

        public VirtualBufferObj(int typed) {
            super();

            // TODO: support for typed (entity, indexed, blocks, etc.)
            if ((this.mapped = GlVulkanSharedBuffer.sharedBufferMap.get(typed)) != null) {
                this.glStorageBuffer = this.mapped.glStorageBuffer;
            }

            //
            //System.out.println("Generated New GPU or Uniform Buffer! Id: " + this.glVirtualBuffer);
        }

        @Override
        public ByteBuffer map(int target, int access) {

            // TODO: dedicated semaphore instead of swapchain
            // Avoid OpenGL and Vulkan API mapping memory corruption
            // Not helped...
            /*
            var queueFamilyIndex = 0;
            glSignalSemaphoreEXT(GlContext.rendererObj.glSignalSemaphore, memAllocInt(0), memAllocInt(0), memAllocInt(0));
            GlContext.rendererObj.logicalDevice.submitOnce(GlContext.rendererObj.logicalDevice.getCommandPool(queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
                waitSemaphores = memAllocLong(1).put(0, GlContext.rendererObj.swapchain.semaphoreImageAvailable.getHandle().get());
                queue = GlContext.rendererObj.logicalDevice.getQueue(queueFamilyIndex, 0);
            }}, (cmdBuf)->{
                return VK_SUCCESS;
            });*/

            //
            return (this.allocatedMemory = this.mapped.obj.map(this.realSize, offset.get(0)));
        }

        @Override
        public void unmap(int target) {
            this.mapped.obj.unmap();
            this.allocatedMemory = null;

            // TODO: dedicated semaphore instead of swapchain
            // Avoid OpenGL and Vulkan API mapping memory corruption
            // Not helped...
            /*
            var queueFamilyIndex = 0;
            GlContext.rendererObj.logicalDevice.submitOnce(GlContext.rendererObj.logicalDevice.getCommandPool(queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
                signalSemaphores = memAllocLong(1).put(0, GlContext.rendererObj.swapchain.semaphoreRenderingAvailable.getHandle().get());
                queue = GlContext.rendererObj.logicalDevice.getQueue(queueFamilyIndex, 0);
            }}, (cmdBuf)->{
                return VK_SUCCESS;
            });
            glWaitSemaphoreEXT(GlContext.rendererObj.glWaitSemaphore, memAllocInt(0), memAllocInt(0), memAllocInt(0));
*/
        }

        @Override
        public GlBaseVirtualBuffer.VirtualBufferObj deallocate() throws Exception {
            this.assert_();
            if (this.allocId.get(0) != 0) {
                //
                if (this.mapped != null) {
                    vmaVirtualFree(this.mapped.vb.get(0), this.allocId.get(0));
                }

                //
                this.realSize = 0L;
                this.blockSize = 0L;
                this.address = 0L;
                this.offset.put(0, 0L);
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

                // corrupted de-allocation!
                //deallocate();

                //
                var oldAlloc = this.allocId.get(0);
                this.offset.put(0, 0L);
                int res = vmaVirtualAllocate(this.mapped.vb.get(0), this.allocCreateInfo.size(this.blockSize = defaultSize), this.allocId.put(0, 0L), this.offset);
                if (res != VK_SUCCESS) {
                    System.out.println("Allocation Failed: " + res);
                    throw new Exception("Allocation Failed: " + res);
                }

                // get device address from 
                this.address = this.mapped.obj.getDeviceAddress() + this.offset.get(0);

                // TODO: copy from old segment
                // Avoid some data corruption
                if (this.mapped != null && oldAlloc != 0) {
                    vmaVirtualFree(this.mapped.vb.get(0), oldAlloc);
                }

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
                //glClearNamedBufferSubData(this.glStorageBuffer, GL_R8UI, this.offset.get(0), this.realSize = size, GL_RED_INTEGER, GL_UNSIGNED_BYTE, memAlloc(1).put(0, (byte) 0));
            }
            return this.bindVertex();
        }

        @Override
        public void delete() throws Exception {
            this.mapped.virtualBuffers.remove(this);
            super.delete();
        }
    }

    //
    public static void initialize() throws IOException {

    };

    //
    public static int createVirtualBuffer() throws Exception {
        return (new VirtualBufferObj(0)).glVirtualBuffer;
    }

};
