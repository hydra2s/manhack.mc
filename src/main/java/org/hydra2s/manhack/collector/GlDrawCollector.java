package org.hydra2s.manhack.collector;

// TODO:
// - Add buffer copying to temp
// - Add geometry generation for AS
// - Add material inbound with geometry
// - Support for Vulkan API shaders structures (UBO, etc.)

//
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.AbstractTexture;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.ducks.render.ShaderProgramInterface;
import org.hydra2s.manhack.ducks.vertex.VertexBufferInterface;
import org.hydra2s.manhack.ducks.vertex.VertexFormatInterface;
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedBuffer;
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedTexture;
import org.hydra2s.manhack.virtual.buffer.GlBaseVirtualBuffer;
import org.hydra2s.manhack.virtual.buffer.GlVulkanVirtualBuffer;
import org.hydra2s.noire.descriptors.AccelerationStructureCInfo;
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.DataCInfo;
import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.hydra2s.noire.objects.ImageViewObj;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL45;
import org.lwjgl.vulkan.VkAccelerationStructureBuildRangeInfoKHR;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkMultiDrawInfoEXT;
import org.lwjgl.vulkan.VkTransformMatrixKHR;

//
import java.nio.ByteBuffer;
import java.util.ArrayList;

//
import static org.lwjgl.opengl.EXTSemaphore.glSignalSemaphoreEXT;
import static org.lwjgl.opengl.EXTSemaphore.glWaitSemaphoreEXT;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL30C.glBindBufferRange;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.GL_VERTEX_ATTRIB_BINDING;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;
import static org.lwjgl.opengl.GL45.glClearNamedBufferSubData;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.vmaClearVirtualBlock;
import static org.lwjgl.vulkan.EXTIndexTypeUint8.VK_INDEX_TYPE_UINT8_EXT;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_INDEX_TYPE_NONE_KHR;
import static org.lwjgl.vulkan.VK10.*;

//
public class GlDrawCollector {

    //
    public static class VirtualTempBinding {
        public VirtualTempBinding(long addressOffset, long realSize, int stride) {
            this.addressOffset = addressOffset;
            this.realSize = realSize;
            this.stride = stride;
        }

        // re-wrote info
        public long realSize = 0L;
        public long addressOffset = 0L;
        public long relativeOffset = 0L;
        public int format = VK_FORMAT_UNDEFINED;
        public int stride = 0;

        //
        public GlVulkanVirtualBuffer.VirtualBufferObj virtualBuffer = null;

        //
        static final int bindingStride = 8 + 8 + 4 + 4 + 4 + 4; // 32-byte binding

        //
        public void writeBinding(ByteBuffer by, long bfOffset, long bfIndex) {
            // address, offset, format, stride, size

            var bOffset = (int) (bfOffset + bfIndex * this.bindingStride);
            if (this.virtualBuffer != null) {
                memSlice(by, bOffset, 8).putLong(0, this.virtualBuffer.address + this.addressOffset + this.relativeOffset);
                memSlice(by, bOffset + 8, 8).putLong(0, this.realSize - this.relativeOffset);
                memSlice(by, bOffset + 16, 4).putInt(0, (int) this.relativeOffset);
                memSlice(by, bOffset + 20, 4).putInt(0, this.stride);
                memSlice(by, bOffset + 24, 4).putInt(0, this.format); // TODO: custom format encoding for shader
            } else {
                memSlice(by, bOffset, 8).putLong(0, 0);
                memSlice(by, bOffset + 8, 8).putLong(0, 0);
                memSlice(by, bOffset + 16, 4).putInt(0, 0);
                memSlice(by, bOffset + 20, 4).putInt(0, 0);
                memSlice(by, bOffset + 24, 4).putInt(0, VK_FORMAT_UNDEFINED);
            }
        }
    }

    //vertexDataBuffer
    public static class DrawCallObj {
        // TODO: needs it?! Sharing only...
        public GlVulkanVirtualBuffer.VirtualBufferObj vertexBuffer = null;
        public GlVulkanVirtualBuffer.VirtualBufferObj uniformDataBuffer = null;

        // bindings
        // TODO: replace to array based
        public VirtualTempBinding vertexBinding = null;
        public VirtualTempBinding normalBinding = null;
        public VirtualTempBinding colorBinding = null;
        public VirtualTempBinding uvBinding = null;

        // only this is really useful
        public long uniformOffset = 0L;
        public int primitiveCount = 0;
        public int elementCount = 0;
        public int vertexStride = 0;

        public long vertexDataOffset = 0L;
        public int indexType = VK_INDEX_TYPE_NONE_KHR;
        public int indexStride = 0;


        // rewrite from uniform data
        //public IntList vkImageViews; // will paired with `uniformDataBuffer`
    }

    // will reset and deallocated every draw...
    public static ArrayList<DrawCallObj> collectedDraws = new ArrayList<>();
    public static int drawCount = 0;
    public static long elementCount = 0;
    public static boolean potentialOverflow = false;

    //
    public static long vertexDataOffset = 0L;



    // deallocate and reset all draws data
    // PROBLEM WAS FOUND HERE!
    // Old Data Still Reused...
    public static void resetDraw() throws Exception {
        collectedDraws.forEach((drawCall)->{
            try {
                drawCall.vertexBuffer = null;
                drawCall.vertexBinding = null;
                drawCall.normalBinding = null;
                drawCall.colorBinding = null;
                drawCall.uvBinding = null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        //
        collectedDraws.clear();

        //
        drawCount = 0;
        elementCount = 0;
        potentialOverflow = false;

        //
        vertexDataOffset = 0L;

        //
        ((AccelerationStructureCInfo)GlVulkanSharedBuffer.bottomLvl.cInfo).geometries.clear();

        //
        glClearNamedBufferSubData(GlVulkanSharedBuffer.uniformDataBuffer.glStorageBuffer, GL_R8UI, GlVulkanSharedBuffer.uniformDataBuffer.offset.get(0), GlVulkanSharedBuffer.uniformDataBuffer.realSize, GL_RED_INTEGER, GL_UNSIGNED_BYTE, memAlloc(1).put(0, (byte) 0));
        glClearNamedBufferSubData(GlVulkanSharedBuffer.vertexDataBuffer.glStorageBuffer, GL_R8UI, GlVulkanSharedBuffer.vertexDataBuffer.offset.get(0), GlVulkanSharedBuffer.vertexDataBuffer.realSize, GL_RED_INTEGER, GL_UNSIGNED_BYTE, memAlloc(1).put(0, (byte) 0));
        glFinish();

        //


        // fully nullify that buffer
        //if (!GlContext.worldRendering)
    }

    // collect draw calls for batch draw and acceleration structure
    // getting errors when any text rendering
    // TODO: really needs transform feedback!
    public static void collectDraw(int mode, int count, int type, long indices) throws Exception {
        // don't record GUI, or other trash
        if (!GlContext.worldRendering) { return; }

        // isn't valid! must be drawn in another layer, and directly.
        if (mode != GL_TRIANGLES) { return; }

        // TODO: uint8 index type may to be broken or corrupted...
        if (type == GL_UNSIGNED_BYTE || type == GL_BYTE) { return; }

        // unsupported...
        if (indices > 0) { return; };

        //
        if (drawCount >= GlVulkanSharedBuffer.maxDrawCalls) {
            System.out.println("WARNING! Draw Call Limit Exceeded...");
            return;
        }

        //
        var _pipelineLayout = GlContext.rendererObj.pipelineLayout;
        var _memoryAllocator = GlContext.rendererObj.memoryAllocator;

        //
        var boundShaderProgram = GlContext.boundShaderProgram; // only for download a uniform data
        if (boundShaderProgram == null) { System.out.println("Shader Program Was Not Bound..."); return; };
        var boundShaderProgramI = (ShaderProgramInterface)boundShaderProgram; // only for download a uniform data

        // TODO: direct and zero-copy host memory support
        // TODO: fix broken boundVertexBuffer support
        var vao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        //var virtualIndexBuffer = boundVertexBufferI.getIndexBufferId() > 0 ? GlContext.virtualBufferMap.get(boundVertexBufferI.getIndexBufferId()) : GlContext.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER);
        //if (virtualIndexBuffer.realSize <= 0) { virtualIndexBuffer = GlContext.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER); };

        //
        var drawCallData = new DrawCallObj();
        drawCallData.vertexBuffer = GlVulkanSharedBuffer.vertexDataBuffer;
        drawCallData.uniformDataBuffer = GlVulkanSharedBuffer.uniformDataBuffer;//new GlVulkanVirtualBuffer.VirtualBufferObj(1);
        drawCallData.uniformOffset = GlVulkanSharedBuffer.uniformStride * drawCount;

        //
        drawCallData.primitiveCount = count/3;
        drawCallData.elementCount = count;
        drawCallData.vertexBuffer.stride = 48;//boundVertexFormat.getVertexSizeByte();
        drawCallData.indexType = VK_INDEX_TYPE_NONE_KHR;
        drawCallData.indexStride = 0;

        //
        //if (type == GL_UNSIGNED_BYTE  || type == GL_BYTE ) { drawCallData.indexType = VK_INDEX_TYPE_UINT8_EXT; drawCallData.indexStride = 1; }
        //if (type == GL_UNSIGNED_SHORT || type == GL_SHORT) { drawCallData.indexType = VK_INDEX_TYPE_UINT16; drawCallData.indexStride = 2; }
        //if (type == GL_UNSIGNED_INT   || type == GL_INT  ) { drawCallData.indexType = VK_INDEX_TYPE_UINT32; drawCallData.indexStride = 4; }

        // TODO: fill uniform data
        var uniformData = GlVulkanSharedBuffer.uniformDataBufferHost.map(GL_UNIFORM_BUFFER, GL_MAP_WRITE_BIT);//memSlice(GlVulkanSharedBuffer.uniformDataBufferHost.map(GL_UNIFORM_BUFFER, GL_MAP_WRITE_BIT), (int) drawCallData.uniformOffset, (int) GlVulkanSharedBuffer.uniformStride);

        //
        drawCallData.vertexDataOffset = vertexDataOffset;
        var vertexDataSize = count * 48;

        // TODO: replace by array based
        drawCallData.vertexBinding = new VirtualTempBinding(drawCallData.vertexDataOffset, vertexDataSize, drawCallData.vertexStride);
        drawCallData.vertexBinding.virtualBuffer = drawCallData.vertexBuffer;
        drawCallData.vertexBinding.format = VK_FORMAT_R32G32B32_SFLOAT;
        drawCallData.vertexBinding.relativeOffset = 0;

        // TODO: replace by array based
        drawCallData.uvBinding = new VirtualTempBinding(drawCallData.vertexDataOffset, vertexDataSize, drawCallData.vertexStride);
        drawCallData.uvBinding.virtualBuffer = drawCallData.vertexBuffer;
        drawCallData.uvBinding.format = VK_FORMAT_R32G32_SFLOAT;
        drawCallData.uvBinding.relativeOffset = 28;

        // TODO: replace by array based
        drawCallData.colorBinding = new VirtualTempBinding(drawCallData.vertexDataOffset, vertexDataSize, drawCallData.vertexStride);
        drawCallData.colorBinding.virtualBuffer = drawCallData.vertexBuffer;
        drawCallData.colorBinding.format = VK_FORMAT_R32_UINT; // rgba8unorm de-facto
        drawCallData.colorBinding.relativeOffset = 36;

        // TODO: replace by array based
        drawCallData.normalBinding = new VirtualTempBinding(drawCallData.vertexDataOffset, vertexDataSize, drawCallData.vertexStride);
        drawCallData.normalBinding.virtualBuffer = drawCallData.vertexBuffer;
        drawCallData.normalBinding.format = VK_FORMAT_R32_UINT; // rgba8snorm de-facto
        drawCallData.normalBinding.relativeOffset = 12;



        //
        var modelView = boundShaderProgram.modelViewMat;

        // TODO: needs to get transform matrix only
        var mvTransform = modelView != null ? new Matrix4f(modelView.getFloatData()) : new Matrix4f().identity();

        //
        var chunkOffset = boundShaderProgram.chunkOffset != null ? boundShaderProgram.chunkOffset.getFloatData() : memAllocFloat(3).put(0, 0.F).put(1, 0.F).put(2, 0.F);
        var transform = new Matrix4f().identity();

        // incorrect matrix!
        transform
            .mul(mvTransform)
            .translate(new Vector3f(chunkOffset.get(0), chunkOffset.get(1), chunkOffset.get(2)))
            .transpose()
            .get(memSlice(uniformData, 0, 16*4));

        //
        memSlice(uniformData, 16*4 + 8, 4).putInt(0, drawCallData.indexType);
        memSlice(uniformData, 16*4 + 8 + 4, 4).putInt(0, drawCallData.indexStride);

        //
        memSlice(uniformData, 16*4 + 16 + 4*0, 4).putFloat(0, chunkOffset.get(0));
        memSlice(uniformData, 16*4 + 16 + 4*1, 4).putFloat(0, chunkOffset.get(1));
        memSlice(uniformData, 16*4 + 16 + 4*2, 4).putFloat(0, chunkOffset.get(2));
        memSlice(uniformData, 16*4 + 16 + 4*3, 4).putFloat(0, 1.F);

        //
        // TODO: replace by array based
        var bindingOffset = 16*4 + 16 + 16;
        drawCallData.vertexBinding.writeBinding(uniformData, bindingOffset, 0);
        drawCallData.normalBinding.writeBinding(uniformData, bindingOffset, 1);
        drawCallData.uvBinding.writeBinding(uniformData, bindingOffset, 2);
        drawCallData.colorBinding.writeBinding(uniformData, bindingOffset, 3);

        //
        var metaOffset = bindingOffset + VirtualTempBinding.bindingStride*4;

        //
        var object = boundShaderProgramI.getSamplers().get("Sampler0");
        int l = -1;
        if (object instanceof Framebuffer)     { l = ((Framebuffer)object).getColorAttachment(); } else
        if (object instanceof AbstractTexture) { l = ((AbstractTexture)object).getGlId(); } else
        if (object instanceof Integer)         { l = (Integer)object; }

        // default image view values
        memSlice(uniformData, metaOffset, 4).putInt(0, -1);

        // TODO: get more images/samplers
        var vkTexture = GlVulkanSharedTexture.sharedImageMap.get(l);
        if (vkTexture != null) {
            memSlice(uniformData, metaOffset, 4).putInt(0, vkTexture.imageView.DSC_ID);
        }

        //
        GlVulkanSharedBuffer.uniformDataBufferHost.unmap(GL_UNIFORM_BUFFER);

        // is Vulkan with OpenGL shared!
        var _vOffset = drawCallData.vertexBuffer.offset.get(0) + drawCallData.vertexDataOffset;

        //
        GL45.glCopyNamedBufferSubData(
                GlVulkanSharedBuffer.uniformDataBufferHost.glStorageBuffer, drawCallData.uniformDataBuffer.glStorageBuffer,
                GlVulkanSharedBuffer.uniformDataBufferHost.offset.get(0), drawCallData.uniformDataBuffer.offset.get(0) + drawCallData.uniformOffset,
                GlVulkanSharedBuffer.uniformStride);

        //
        //GL45.glResumeTransformFeedback();
        var vertexFormat = GlContext.boundVertexBuffer.getVertexFormat();
        //GlContext.rendererObj.glTransformProgram.get(vertexFormat).bind();

        //glBindBufferRange(GL_TRANSFORM_FEEDBACK_BUFFER, 0, GlVulkanSharedBuffer.vertexDataBuffer.glStorageBuffer, GlVulkanSharedBuffer.vertexDataBuffer.offset.get(0) + drawCallData.vertexDataOffset, vertexDataSize);
        //glBeginTransformFeedback(GL_TRIANGLES);
        GL45.glDrawElements(mode, count, type, indices);
        //GL45.glPauseTransformFeedback();
        //glEndTransformFeedback();

        // OpenGL, you are drunk?
        GL45.glFinish();

        //
        vertexDataOffset += vertexDataSize;//virtualVertexBuffer.realSize;
        collectedDraws.add(drawCallData);
        drawCount++; elementCount += count;

        //
        if ((elementCount / drawCount) > GlVulkanSharedBuffer.averageVertexCount*3) {
            potentialOverflow = true;
            System.out.println("WARNING! Potential Average Vertex Count Overflow...");
        } else {
            potentialOverflow = false;
        }
    }

    // for building draw into acceleration structures
    public static void buildDraw() {
        var cInfo = (AccelerationStructureCInfo)GlVulkanSharedBuffer.bottomLvl.cInfo;
        cInfo.geometries.clear();

        // will be used in top level of acceleration structure
        var playerCamera = MinecraftClient.getInstance().gameRenderer.getCamera();

        // probka
        var fTransform = memAllocFloat(12);
        //var fTransform = memAllocFloat(16); // may corrupt instance data
        var gTransform = new Matrix4f().identity();
        gTransform = gTransform.translate(new Vector3f((float) (-playerCamera.getPos().x), (float) (-playerCamera.getPos().y), (float) (-playerCamera.getPos().z)));
        gTransform.transpose().get3x4(fTransform);

        //
        //GlVulkanSharedBuffer.instanceInfo.transform(VkTransformMatrixKHR.calloc().matrix(fTransform));
        GlVulkanSharedBuffer.drawRanges = VkAccelerationStructureBuildRangeInfoKHR.calloc(collectedDraws.size());
        GlVulkanSharedBuffer.multiDraw = VkMultiDrawInfoEXT.calloc(collectedDraws.size());

        //
        //System.out.println("DEBUG! Draw Call Count: " + collectedDraws.size());

        //
        for (int I=0;I<collectedDraws.size();I++) {
            var cDraw = collectedDraws.get(I);

            //
            GlVulkanSharedBuffer.drawRanges.get(I)
                .primitiveCount(cDraw.primitiveCount)
                .firstVertex(0)
                .primitiveOffset(0)
                .transformOffset(0);

            //
            GlVulkanSharedBuffer.multiDraw.get(I)
                .firstVertex(0)
                .vertexCount(cDraw.elementCount);

            //
            cInfo.geometries.add(new DataCInfo.TriangleGeometryCInfo() {{
                transformAddress = GlVulkanSharedBuffer.uniformDataBuffer.address + cDraw.uniformOffset;
                vertexBinding = new DataCInfo.VertexBindingCInfo() {{
                    address = cDraw.vertexBinding.virtualBuffer.address + cDraw.vertexDataOffset + cDraw.vertexBinding.relativeOffset;
                    stride = cDraw.vertexStride;
                    format = cDraw.vertexBinding.format;
                    vertexCount = cDraw.elementCount;
                }};
            }});
        }

        //
        GlVulkanSharedBuffer.bottomLvl.recallGeometryInfo();
    }

    // probably, prepare buffers and commands (indirect draw)
    public static void preBuildDraw() {

    }

}
