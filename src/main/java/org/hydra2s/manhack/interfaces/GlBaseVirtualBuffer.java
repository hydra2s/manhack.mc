package org.hydra2s.manhack.interfaces;

//
import org.hydra2s.manhack.UnifiedMap;
import org.hydra2s.manhack.virtual.buffer.GlHostVirtualBuffer;
import org.hydra2s.manhack.vulkan.GlVulkanSharedBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;

//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

//
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;

//
public interface GlBaseVirtualBuffer {
    //
    public static Map<Integer, VirtualBufferObj> boundBuffers = new HashMap<Integer, VirtualBufferObj>() {{

    }};

    //
    public static UnifiedMap<VirtualBufferObj> virtualBufferMap = new UnifiedMap<VirtualBufferObj>();
    public static VirtualBufferObj dummyCache = new VirtualBufferObj();

    //
    static public class VirtualBufferObj {
        public long size = 0;
        public LongBuffer offset = memAllocLong(1).put(0, 0L);

        //
        public VmaVirtualAllocationCreateInfo allocCreateInfo = null;
        public PointerBuffer allocId = null;

        //
        public int glVirtualBuffer = -1;
        public int glStorageBuffer = -1;
        public int stride = 0;
        public int bindingIndex = 0;
        public int target = 0;
        public int vao = 0;
        public ByteBuffer allocatedMemory = null;


        //
        public ByteBuffer map(int target, int access) throws Exception {
            return this.allocatedMemory;
        }

        public void unmap(int target) throws Exception {
            this.allocatedMemory = null;
        }

        public VirtualBufferObj deallocate() throws Exception {
            this.size = 0L;
            return this;
        }

        public void delete() throws Exception {
            this.assert_();
            virtualBufferMap.removeMem(this.deallocate());
            this.size = 0L;
            this.glVirtualBuffer = -1;
            this.glStorageBuffer = -1;
        }

        public VirtualBufferObj assert_() throws Exception {
            if (this == null || this.glVirtualBuffer <= 0 || !virtualBufferMap.contains(this)) {
                System.out.println("Wrong Virtual Buffer Id! " + (this != null ? this.glVirtualBuffer : -1));
                throw new Exception("Wrong Virtual Buffer Id! " + (this != null ? this.glVirtualBuffer : -1));
            }
            return this;
        }

        public VirtualBufferObj bindVertex() throws Exception {
            this.assert_();
            if (this.target == GL_ARRAY_BUFFER && this.glStorageBuffer > 0 && this.stride > 0 && this.vao > 0) {
                GL45.glVertexArrayVertexBuffer(this.vao, this.bindingIndex, this.glStorageBuffer, this.offset.get(0), this.stride);
            }
            if (this.target == GL_ELEMENT_ARRAY_BUFFER && this.glStorageBuffer > 0) {
                glBindBuffer(this.target, this.glStorageBuffer);
            }
            return this;
        }

        public VirtualBufferObj preDraw() throws Exception {
            return this;
        }

        public VirtualBufferObj postDraw() throws Exception {
            return this;
        }

        public GlBaseVirtualBuffer.VirtualBufferObj bind(int target) throws Exception {
            this.assert_();
            boundBuffers.remove(target);

            // TODO: unbound memory
            if (target == GL_ARRAY_BUFFER) {
                this.vao = this.vao > 0 ? this.vao : glGetInteger(GL_VERTEX_ARRAY_BINDING);
            }

            // TODO: unbound memory
            boundBuffers.put(this.target = target, this);
            return this.bindVertex();
        }

        public VirtualBufferObj allocate(long defaultSize, int usage) throws Exception {
            this.size = defaultSize;
            return this;
        }

        //
        public VirtualBufferObj() {
            this.offset = memAllocLong(1).put(0, 0L);
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.allocCreateInfo = VmaVirtualAllocationCreateInfo.create().alignment(4L);
            this.glVirtualBuffer = virtualBufferMap.arrayMap.push(this);
            this.glStorageBuffer = -1;
            this.allocatedMemory = null;
            System.out.println("Generated New Virtual Buffer! Id: " + this.glVirtualBuffer);
        }

        public VirtualBufferObj data(int target, ByteBuffer data, int usage) throws Exception {
            this.size = data.remaining();
            return this.bindVertex();
        }

        public VirtualBufferObj data(int target, long size, int usage) throws Exception {
            this.size = size;
            return this.bindVertex();
        }
    };

    // Dummy
    public static void initialize() throws IOException {
    }

    // Dummy
    public static int createVirtualBuffer() throws Exception {
        return -1;
    }
}
