package org.hydra2s.manhack;

//
import org.hydra2s.noire.objects.PipelineLayoutObj;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;

//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

//
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL30.glGetInteger;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.glNamedBufferSubData;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

//
public class GlVirtualBufferSystem {

    public static final boolean VGL_VERSION_A = true;

    //
    static public class VirtualBufferObj {
        public long size = 0;
        public LongBuffer offset = memAllocLong(1).put(0, 0L);

        //
        //public VmaVirtualAllocationInfo allocInfo;
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
        //public ArrayList<Runnable> defer = null;

        //
        public ByteBuffer map(int target, int access, long vkWholeSize, long i) {
            if (VGL_VERSION_A) {
                return (this.allocatedMemory = this.mapped.obj.map(vkWholeSize, i + offset.get(0)));
            } else {
                return (this.allocatedMemory = glMapBuffer(target, access));
            }
        }

        public void unmap(int target) {
            if (VGL_VERSION_A) {
                this.mapped.obj.unmap();
            } else {
                glUnmapBuffer(target);
            }
            this.allocatedMemory = null;
        }

        //
        public VirtualBufferObj() {
            //this.defer = new ArrayList<>();
            this.glVirtualBuffer = virtualBufferMap.push(this);
            this.offset = memAllocLong(1).put(0, 0L);
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.allocCreateInfo = VmaVirtualAllocationCreateInfo.create().alignment(4L);
        }
    };


    public static VirtualBufferObj assertVirtualBufferObj(VirtualBufferObj VBO) throws Exception {
        if (VBO == null || VBO.glVirtualBuffer <= 0 || !virtualBufferMap.contains(VBO)) {
            System.out.println("Wrong Virtual Buffer Id! " + (VBO != null ? VBO.glVirtualBuffer : -1));
            throw new Exception("Wrong Virtual Buffer Id! " + (VBO != null ? VBO.glVirtualBuffer : -1));
        }
        return VBO;
    }



    // TODO: Virtual OpenGL Buffers support!
    // TODO: Outstanding Array instead of Map!
    public static PipelineLayoutObj.OutstandingArray<VirtualBufferObj> virtualBufferMap = new PipelineLayoutObj.OutstandingArray<VirtualBufferObj>();
    public static VirtualBufferObj dummyCache = new VirtualBufferObj();

    //
    public static Map<Integer, VirtualBufferObj> boundBuffers = new HashMap<Integer, VirtualBufferObj>() {{

    }};

    //
    public static boolean hasIndexBuffer = false;

    //
    public static void initialize() throws IOException {

    };

    //
    public static int glCreateVirtualBuffer() throws Exception {
        var VBO = assertVirtualBufferObj(new VirtualBufferObj());
        if (VGL_VERSION_A) {
            // TODO: support for typed (entity, indexed, blocks, etc.)
            var mapped = GlSharedBufferSystem.resourceTargetMap.get(0);
            VBO.glStorageBuffer = mapped.glStorageBuffer;
            VBO.mapped = mapped;
        } else {
            VBO.glStorageBuffer = GL45.glGenBuffers();
        }

        //
        System.out.println("Generated New Virtual Buffer! Id: " + VBO.glVirtualBuffer);
        return VBO.glVirtualBuffer;
    };

    //
    public static VirtualBufferObj glBindVirtualVertexBuffer(VirtualBufferObj VBO) {
        if (VBO.target == GL_ARRAY_BUFFER && VBO.stride > 0 && VBO.size > 0 && VBO.vao > 0) {
            GL45.glVertexArrayVertexBuffer(VBO.vao, VBO.bindingIndex, VBO.glStorageBuffer, VBO.offset.get(0), VBO.stride);
            // TODO: fix calling spam by VAO objects

            /*
            System.out.println("Vertex Buffer Bound!");
            System.out.println("Arg0 (VAO): " + VBO.vao);
            System.out.println("Arg1 (BindingIndex): " + VBO.bindingIndex);
            System.out.println("Arg2 (Buffer, SYSTEM): " + VBO.glStorageBuffer);
            System.out.println("Arg2 (Buffer, VIRTUAL): " + VBO.glVirtualBuffer);
            System.out.println("Arg3 (Offset): " + VBO.offset.get(0));
            System.out.println("Arg4 (Stride): " + VBO.stride);
            */
        }
        return VBO;
    }

    //
    public static VirtualBufferObj glAllocateVirtualBuffer(VirtualBufferObj VBO, long defaultSize, int usage) throws Exception {
        // TODO: support for typed (entity, indexed, blocks, etc.)
        var mapped = GlSharedBufferSystem.resourceTargetMap.get(0);

        //
        assertVirtualBufferObj(VBO);
        if (VBO.size < defaultSize)
        {
            System.out.println("WARNING! Size of virtual buffer was changed! " + VBO.size + " != " + defaultSize);
            System.out.println("Virtual GL buffer ID: " + VBO.glVirtualBuffer);

            //
            glDeallocateVirtualBuffer(VBO);

            //
            int res = vmaVirtualAllocate(mapped.vb.get(0), VBO.allocCreateInfo.size(VBO.size = defaultSize), VBO.allocId.put(0, 0L), VBO.offset.put(0, 0L));
            if (res != VK_SUCCESS) {
                System.out.println("Allocation Failed: " + res);
                throw new Exception("Allocation Failed: " + res);
            }

            //
            System.out.println("Virtual buffer Size Changed: " + VBO.size);
        }
        return VBO;
    }

    //
    public static VirtualBufferObj glAllocateVirtualBuffer(int target, long defaultSize, int usage) throws Exception {
        return glAllocateVirtualBuffer(boundBuffers.get(target), defaultSize, usage);
    }

    //
    public static void glBindVirtualBuffer(int target, int glVirtual) throws Exception {
        GL20.glBindBuffer(target, 0);
        boundBuffers.remove(target);

        // TODO: unbound memory
        var VBO = assertVirtualBufferObj(virtualBufferMap.get(glVirtual));
        if (target == GL_ARRAY_BUFFER) {
            VBO.vao = VBO.vao > 0 ? VBO.vao : glGetInteger(GL_VERTEX_ARRAY_BINDING);
        }

        //
        //if (target == GL_ELEMENT_ARRAY_BUFFER) {
            //System.out.println("Bound Virtual Index Buffer: " + VBO.glVirtualBuffer + ", With offset: " + VBO.offset.get(0));
        //}

        // TODO: unbound memory
        boundBuffers.put(VBO.target = target, VBO);
        GL20.glBindBuffer(target, VBO.glStorageBuffer);
        glBindVirtualVertexBuffer(VBO);
    }

    public static VirtualBufferObj glDeallocateVirtualBuffer(VirtualBufferObj resource) throws Exception {
        if (VGL_VERSION_A) {
            assertVirtualBufferObj(resource);
            vmaVirtualFree(resource.mapped.vb.get(0), resource.allocId.get(0));
            System.out.println("Deallocated Virtual Buffer! Id: " + resource.glVirtualBuffer);
            resource.size = 0L;
            resource.offset.put(0, 0L);
        }
        return resource;
    }

    public static VirtualBufferObj glDeallocateVirtualBuffer(int glVirtualBuffer) throws Exception {
        return glDeallocateVirtualBuffer(assertVirtualBufferObj(virtualBufferMap.get(glVirtualBuffer)));
    }

    public static void glDeleteVirtualBuffer(int glVirtualBuffer) throws Exception {
        VirtualBufferObj resource = assertVirtualBufferObj(virtualBufferMap.get(glVirtualBuffer));
        virtualBufferMap.removeMem(glDeallocateVirtualBuffer(resource));
        boundBuffers.remove(resource.target);
        System.out.println("Deleted Virtual Buffer! Id: " + resource.glVirtualBuffer);
        resource.glVirtualBuffer = -1;

        if (!VGL_VERSION_A) {
            glDeleteBuffers(resource.glStorageBuffer);
        }
    }

    // TODO: full replace by Vulkan
    public static VirtualBufferObj glVirtualBufferData(int target, long data, int usage) throws Exception {
        var VBO = assertVirtualBufferObj(boundBuffers.get(target));
        if (VGL_VERSION_A) {
            glAllocateVirtualBuffer(VBO, data, usage);
        } else {
            GL45.glNamedBufferData(VBO.glStorageBuffer, VBO.size = data, usage);
        }
        glBindVirtualVertexBuffer(VBO);
        return VBO;
    }

    // TODO: full replace by Vulkan
    public static VirtualBufferObj glVirtualBufferData(int target, ByteBuffer data, int usage) throws Exception {
        var VBO = assertVirtualBufferObj(boundBuffers.get(target));
        if (VGL_VERSION_A) {
            glAllocateVirtualBuffer(VBO, data.remaining(), usage);
            glNamedBufferSubData(VBO.glStorageBuffer, VBO.offset.get(0), data);
        } else {
            GL45.glNamedBufferData(VBO.glStorageBuffer, data, usage);
            VBO.size = data.remaining();
        }
        glBindVirtualVertexBuffer(VBO);
        return VBO;
    }
};
