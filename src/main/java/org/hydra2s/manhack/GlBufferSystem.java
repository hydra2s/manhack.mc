package org.hydra2s.manhack;

//
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;

//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

//
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;

//
public interface GlBufferSystem {
    //
    public static Map<Integer, VirtualBufferObj> boundBuffers = new HashMap<Integer, VirtualBufferObj>() {{

    }};
    public static UnifiedMap<VirtualBufferObj> virtualBufferMap = new UnifiedMap<VirtualBufferObj>();


    //
    static public class VirtualBufferObj {
        public long size = 0;
        public LongBuffer offset = memAllocLong(1).put(0, 0L);

        //
        public VmaVirtualAllocationCreateInfo allocCreateInfo = null;
        public GlSharedBufferSystem.VkSharedBuffer mapped = null;
        public PointerBuffer allocId = null;

        //
        public int glVirtualBuffer = -1;
        public int glStorageBuffer = -1;
        public int stride = 0;
        public int bindingIndex = 0;
        public int target = 0;
        public int vao = 0;
        public ByteBuffer allocatedMemory;


        //
        public ByteBuffer map(int target, int access, long vkWholeSize, long i) {
            return this.allocatedMemory;
        }
        public void unmap(int target) {
            this.allocatedMemory = null;
        }

        public VirtualBufferObj deallocate() throws Exception {
            return this;
        }

        public void delete() throws Exception {
        }

        public VirtualBufferObj assert_() throws Exception {
            return this;
        }

        public VirtualBufferObj bindVertex() {
            return this;
        }

        public VirtualBufferObj bind(int target) {
            return this;
        }

        public VirtualBufferObj allocate(long defaultSize, int usage) throws Exception {
            return this;
        }

        //
        public VirtualBufferObj() {
            this.offset = memAllocLong(1).put(0, 0L);
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.allocCreateInfo = VmaVirtualAllocationCreateInfo.create().alignment(4L);
        }

        public VirtualBufferObj data(int target, ByteBuffer data, int usage) {
            return this;
        }
    };

    // Dummy
    public static void initialize() throws IOException {
    }

    // Dummy
    public static int glCreateVirtualBuffer() throws Exception {
        return -1;
    }

    public static GlBufferSystem.VirtualBufferObj glAllocateVirtualBuffer(int target, long defaultSize, int usage) throws Exception {
        return boundBuffers.get(target).allocate(defaultSize, usage);
    }

    public static GlBufferSystem.VirtualBufferObj glBindVirtualBuffer(int target, int glVirtual) throws Exception {
        return virtualBufferMap.get(glVirtual).bind(target);
    }

    public static GlBufferSystem.VirtualBufferObj glDeallocateVirtualBuffer(int glVirtualBuffer) throws Exception {
        return virtualBufferMap.get(glVirtualBuffer).deallocate();
    }

    public static void glDeleteVirtualBuffer(int glVirtualBuffer) throws Exception {
        virtualBufferMap.get(glVirtualBuffer).delete();
    }

    public static GlBufferSystem.VirtualBufferObj glVirtualBufferData(int target, long data, int usage) throws Exception {
        return boundBuffers.get(target).allocate(data, usage).bindVertex();
    }

    public static GlBufferSystem.VirtualBufferObj glVirtualBufferData(int target, ByteBuffer data, int usage) throws Exception {
        return boundBuffers.get(target).allocate(data.remaining(), usage).data(target, data, usage).bindVertex();
    }
}
