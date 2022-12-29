package org.hydra2s.manhack;

import org.hydra2s.noire.objects.PipelineLayoutObj;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;

public interface GlBufferSystem {

    //
    static public class VirtualBufferObj {
        public long size = 0;
        public LongBuffer offset = memAllocLong(1).put(0, 0L);

        //
        public VmaVirtualAllocationCreateInfo allocCreateInfo = null;
        public GlSharedBufferSystem.VkSharedBuffer mapped = null;
        public PointerBuffer allocId = null;

        // TODO: Virtual OpenGL Memory!
        public int glVirtualBuffer = -1;
        public int glStorageBuffer = -1;
        public int stride = 0;
        public int bindingIndex = 0;
        public int target = 0;
        public ByteBuffer allocatedMemory;
        public int vao = 0;

        //
        public ByteBuffer map(int target, int access, long vkWholeSize, long i) {
            return this.allocatedMemory;
        }

        public void unmap(int target) {
            this.allocatedMemory = null;
        }

        //
        public VirtualBufferObj() {
            this.offset = memAllocLong(1).put(0, 0L);
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.allocCreateInfo = VmaVirtualAllocationCreateInfo.create().alignment(4L);
        }
    };


}
