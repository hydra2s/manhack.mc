package org.hydra2s.manhack;

//
import org.hydra2s.noire.objects.PipelineLayoutObj;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL45;

//
import java.io.IOException;
import java.nio.ByteBuffer;

//
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL30.glGetInteger;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.glNamedBufferSubData;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

//
public class GlVirtualBufferSystem implements GlBufferSystem {
    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static PipelineLayoutObj.OutstandingArray<VirtualBufferObj> virtualBufferMap = new PipelineLayoutObj.OutstandingArray<VirtualBufferObj>();
    public static VirtualBufferObj dummyCache = new VirtualBufferObj();

    //
    public static class VirtualBufferObj extends GlBufferSystem.VirtualBufferObj {
        //
        public VirtualBufferObj() {
            super();
            this.glVirtualBuffer = virtualBufferMap.push(this);

            // TODO: support for typed (entity, indexed, blocks, etc.)
            var mapped = GlSharedBufferSystem.resourceTargetMap.get(0);
            this.glStorageBuffer = mapped.glStorageBuffer;
            this.mapped = mapped;

            //
            System.out.println("Generated New Virtual Buffer! Id: " + this.glVirtualBuffer);
        }

        //
        @Override
        public ByteBuffer map(int target, int access, long vkWholeSize, long i) {
            return (this.allocatedMemory = this.mapped.obj.map(vkWholeSize, i + offset.get(0)));
        }

        @Override
        public void unmap(int target) {
            this.mapped.obj.unmap();
            this.allocatedMemory = null;
        }

        @Override
        public VirtualBufferObj deallocate() throws Exception {
            this.assert_();
            vmaVirtualFree(this.mapped.vb.get(0), this.allocId.get(0));
            System.out.println("Deallocated Virtual Buffer! Id: " + this.glVirtualBuffer);
            this.size = 0L;
            this.offset.put(0, 0L);
            return this;
        }

        @Override
        public void delete() throws Exception {
            virtualBufferMap.removeMem(this.deallocate());
            boundBuffers.remove(this.target);
            System.out.println("Deleted Virtual Buffer! Id: " + this.glVirtualBuffer);
            this.glVirtualBuffer = -1;
        }

        //
        @Override
        public VirtualBufferObj assert_() throws Exception {
            if (this == null || this.glVirtualBuffer <= 0 || !virtualBufferMap.contains(this)) {
                System.out.println("Wrong Virtual Buffer Id! " + (this != null ? this.glVirtualBuffer : -1));
                throw new Exception("Wrong Virtual Buffer Id! " + (this != null ? this.glVirtualBuffer : -1));
            }
            return this;
        }

        //
        @Override
        public VirtualBufferObj bindVertex() {
            if (this.target == GL_ARRAY_BUFFER && this.stride > 0 && this.size > 0 && this.vao > 0) {
                GL45.glVertexArrayVertexBuffer(this.vao, this.bindingIndex, this.glStorageBuffer, this.offset.get(0), this.stride);
                // TODO: fix calling spam by VAO objects
            }
            return this;
        }

        @Override
        public VirtualBufferObj bind(int target) {
            GL20.glBindBuffer(target, 0);
            boundBuffers.remove(target);

            // TODO: unbound memory
            if (target == GL_ARRAY_BUFFER) {
                this.vao = this.vao > 0 ? this.vao : glGetInteger(GL_VERTEX_ARRAY_BINDING);
            }

            // TODO: unbound memory
            boundBuffers.put(this.target = target, this);
            GL20.glBindBuffer(target, this.glStorageBuffer);
            return bindVertex();
        }

        //
        @Override
        public VirtualBufferObj allocate(long defaultSize, int usage) throws Exception {
            // TODO: support for typed (entity, indexed, blocks, etc.)
            var mapped = GlSharedBufferSystem.resourceTargetMap.get(0);
            if (this.assert_().size < defaultSize)
            {
                System.out.println("WARNING! Size of virtual buffer was changed! " + this.size + " != " + defaultSize);
                System.out.println("Virtual GL buffer ID: " + this.glVirtualBuffer);

                //
                deallocate();

                //
                int res = vmaVirtualAllocate(mapped.vb.get(0), this.allocCreateInfo.size(this.size = defaultSize), this.allocId.put(0, 0L), this.offset.put(0, 0L));
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
        public VirtualBufferObj data(int target, ByteBuffer data, int usage) {
            glNamedBufferSubData(this.glStorageBuffer, this.offset.get(0), data);
            return this;
        }
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static void initialize() throws IOException {

    };

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static int glCreateVirtualBuffer() throws Exception {
        return (new VirtualBufferObj()).glVirtualBuffer;
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glAllocateVirtualBuffer(int target, long defaultSize, int usage) throws Exception {
        return boundBuffers.get(target).allocate(defaultSize, usage);
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glBindVirtualBuffer(int target, int glVirtual) throws Exception {
        return virtualBufferMap.get(glVirtual).bind(target);
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glDeallocateVirtualBuffer(int glVirtualBuffer) throws Exception {
        return virtualBufferMap.get(glVirtualBuffer).deallocate();
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static void glDeleteVirtualBuffer(int glVirtualBuffer) throws Exception {
        virtualBufferMap.get(glVirtualBuffer).delete();
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glVirtualBufferData(int target, long data, int usage) throws Exception {
        return boundBuffers.get(target).allocate(data, usage).bindVertex();
    }

    // TODO: Needs To Deprecate?
    // TODO: Needs Unified Mapping!
    public static GlBufferSystem.VirtualBufferObj glVirtualBufferData(int target, ByteBuffer data, int usage) throws Exception {
        return boundBuffers.get(target).allocate(data.remaining(), usage).data(target, data, usage).bindVertex();
    }
};
