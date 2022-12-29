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
public class GlVirtualBufferSystem implements GlBufferSystem {
    public static PipelineLayoutObj.OutstandingArray<VirtualBufferObj> virtualBufferMap = new PipelineLayoutObj.OutstandingArray<VirtualBufferObj>();
    public static VirtualBufferObj dummyCache = new VirtualBufferObj();
    public static Map<Integer, VirtualBufferObj> boundBuffers = new HashMap<Integer, VirtualBufferObj>() {{

    }};

    public static class VirtualBufferObj extends GlBufferSystem.VirtualBufferObj {
        //
        public VirtualBufferObj() {
            super();
            this.glVirtualBuffer = virtualBufferMap.push(this);
        }

        //
        public ByteBuffer map(int target, int access, long vkWholeSize, long i) {
            return (this.allocatedMemory = this.mapped.obj.map(vkWholeSize, i + offset.get(0)));
        }

        public void unmap(int target) {
            this.mapped.obj.unmap();
            this.allocatedMemory = null;
        }
    }

    // TODO: make object bound op
    public static VirtualBufferObj assertVirtualBufferObj(VirtualBufferObj VBO) throws Exception {
        if (VBO == null || VBO.glVirtualBuffer <= 0 || !virtualBufferMap.contains(VBO)) {
            System.out.println("Wrong Virtual Buffer Id! " + (VBO != null ? VBO.glVirtualBuffer : -1));
            throw new Exception("Wrong Virtual Buffer Id! " + (VBO != null ? VBO.glVirtualBuffer : -1));
        }
        return VBO;
    }

    //
    public static void initialize() throws IOException {

    };

    //
    public static int glCreateVirtualBuffer() throws Exception {
        var VBO = assertVirtualBufferObj(new VirtualBufferObj());

        // TODO: support for typed (entity, indexed, blocks, etc.)
        var mapped = GlSharedBufferSystem.resourceTargetMap.get(0);
        VBO.glStorageBuffer = mapped.glStorageBuffer;
        VBO.mapped = mapped;

        //
        System.out.println("Generated New Virtual Buffer! Id: " + VBO.glVirtualBuffer);
        return VBO.glVirtualBuffer;
    };

    // TODO: make object bound op
    public static VirtualBufferObj glBindVirtualVertexBuffer(VirtualBufferObj VBO) {
        if (VBO.target == GL_ARRAY_BUFFER && VBO.stride > 0 && VBO.size > 0 && VBO.vao > 0) {
            GL45.glVertexArrayVertexBuffer(VBO.vao, VBO.bindingIndex, VBO.glStorageBuffer, VBO.offset.get(0), VBO.stride);
            // TODO: fix calling spam by VAO objects
        }
        return VBO;
    }

    // TODO: make object bound op
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

    // TODO: make object bound op
    public static VirtualBufferObj glAllocateVirtualBuffer(int target, long defaultSize, int usage) throws Exception {
        return glAllocateVirtualBuffer(boundBuffers.get(target), defaultSize, usage);
    }

    // TODO: make object bound op
    public static void glBindVirtualBuffer(int target, int glVirtual) throws Exception {
        GL20.glBindBuffer(target, 0);
        boundBuffers.remove(target);

        // TODO: unbound memory
        var VBO = assertVirtualBufferObj((VirtualBufferObj) virtualBufferMap.get(glVirtual));
        if (target == GL_ARRAY_BUFFER) {
            VBO.vao = VBO.vao > 0 ? VBO.vao : glGetInteger(GL_VERTEX_ARRAY_BINDING);
        }

        // TODO: unbound memory
        boundBuffers.put(VBO.target = target, VBO);
        GL20.glBindBuffer(target, VBO.glStorageBuffer);
        glBindVirtualVertexBuffer(VBO);
    }

    // TODO: make object bound op
    public static VirtualBufferObj glDeallocateVirtualBuffer(VirtualBufferObj resource) throws Exception {
        assertVirtualBufferObj(resource);
        vmaVirtualFree(resource.mapped.vb.get(0), resource.allocId.get(0));
        System.out.println("Deallocated Virtual Buffer! Id: " + resource.glVirtualBuffer);
        resource.size = 0L;
        resource.offset.put(0, 0L);
        return resource;
    }

    //
    public static VirtualBufferObj glDeallocateVirtualBuffer(int glVirtualBuffer) throws Exception {
        return glDeallocateVirtualBuffer(assertVirtualBufferObj(virtualBufferMap.get(glVirtualBuffer)));
    }

    // TODO: make object bound op
    public static void glDeleteVirtualBuffer(int glVirtualBuffer) throws Exception {
        VirtualBufferObj resource = assertVirtualBufferObj(virtualBufferMap.get(glVirtualBuffer));
        virtualBufferMap.removeMem(glDeallocateVirtualBuffer(resource));
        boundBuffers.remove(resource.target);
        System.out.println("Deleted Virtual Buffer! Id: " + resource.glVirtualBuffer);
        resource.glVirtualBuffer = -1;
    }

    //
    public static VirtualBufferObj glVirtualBufferData(int target, long data, int usage) throws Exception {
        var VBO = assertVirtualBufferObj(boundBuffers.get(target));
        glAllocateVirtualBuffer(VBO, data, usage);
        glBindVirtualVertexBuffer(VBO);
        return VBO;
    }

    //
    public static VirtualBufferObj glVirtualBufferData(int target, ByteBuffer data, int usage) throws Exception {
        var VBO = assertVirtualBufferObj(boundBuffers.get(target));
        glAllocateVirtualBuffer(VBO, data.remaining(), usage);
        glNamedBufferSubData(VBO.glStorageBuffer, VBO.offset.get(0), data);
        glBindVirtualVertexBuffer(VBO);
        return VBO;
    }
};
