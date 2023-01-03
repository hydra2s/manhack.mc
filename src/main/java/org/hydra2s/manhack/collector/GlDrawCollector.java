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
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.GL_VERTEX_ATTRIB_BINDING;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;
import static org.lwjgl.opengl.GL45.glClearNamedBufferSubData;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.vmaClearVirtualBlock;
import static org.lwjgl.vulkan.EXTIndexTypeUint8.VK_INDEX_TYPE_UINT8_EXT;
import static org.lwjgl.vulkan.VK10.*;

//
public class GlDrawCollector {

    //
    public static class VirtualTempBinding {
        // re-wrote info
        public long relativeOffset = 0L;
        public int format = VK_FORMAT_UNDEFINED;

        //
        public GlVulkanVirtualBuffer.VirtualBufferObj virtualBuffer = null;

        //
        static final int bindingStride = 8 + 8 + 4 + 4 + 4 + 4; // 32-byte binding

        //
        public void writeBinding(ByteBuffer by, long bfOffset, long bfIndex) {
            // address, offset, format, stride, size

            var bOffset = (int) (bfOffset + bfIndex * bindingStride);
            if (virtualBuffer != null) {
                memSlice(by, bOffset, 8).putLong(0, virtualBuffer.address + relativeOffset);
                memSlice(by, bOffset + 8, 8).putLong(0, virtualBuffer.realSize - relativeOffset);
                memSlice(by, bOffset + 16, 4).putInt(0, (int) relativeOffset);
                memSlice(by, bOffset + 20, 4).putInt(0, virtualBuffer.stride);
                memSlice(by, bOffset + 24, 4).putInt(0, format); // TODO: custom format encoding for shader
            } else {
                memSlice(by, bOffset, 8).putLong(0, 0);
                memSlice(by, bOffset + 8, 8).putLong(0, 0);
                memSlice(by, bOffset + 16, 4).putInt(0, 0);
                memSlice(by, bOffset + 20, 4).putInt(0, 0);
                memSlice(by, bOffset + 24, 4).putInt(0, VK_FORMAT_UNDEFINED);
            }
        }
    }

    //
    public static class DrawCallObj {
        // buffers
        public GlVulkanVirtualBuffer.VirtualBufferObj indexBuffer = null;
        public GlVulkanVirtualBuffer.VirtualBufferObj vertexBuffer = null;

        // TODO: needs it?! Sharing only...
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

        // rewrite from uniform data
        //public IntList vkImageViews; // will paired with `uniformDataBuffer`
    }

    // will reset and deallocated every draw...
    public static ArrayList<DrawCallObj> collectedDraws = new ArrayList<>();
    public static int drawCount = 0;
    public static long elementCount = 0;
    public static boolean potentialOverflow = false;

    // deallocate and reset all draws data
    // PROBLEM WAS FOUND HERE!
    // Old Data Still Reused...
    public static void resetDraw() throws Exception {
        collectedDraws.forEach((drawCall)->{
            try {
                //if (!GlContext.worldRendering) {
                    drawCall.vertexBuffer.delete();
                    drawCall.indexBuffer.delete();
                //}
                drawCall.vertexBuffer = null;
                drawCall.indexBuffer = null;
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
        ((AccelerationStructureCInfo)GlVulkanSharedBuffer.bottomLvl.cInfo).geometries.clear();

        // fully nullify that buffer
        //if (!GlContext.worldRendering)
        {
            GlVulkanSharedBuffer.sharedBufferMap.get(1).resetAllocations();
            GlVulkanSharedBuffer.sharedBufferMap.get(2).resetAllocations();
            GlVulkanSharedBuffer.sharedBufferMap.get(3).resetAllocations();

            //
            GlVulkanSharedBuffer.uniformDataBufferHost = new GlVulkanVirtualBuffer.VirtualBufferObj(2);
            GlVulkanSharedBuffer.uniformDataBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(3);
            GlVulkanSharedBuffer.uniformDataBufferHost.deallocate().allocate(GlVulkanSharedBuffer.uniformStride * GlVulkanSharedBuffer.maxDrawCalls, GL_DYNAMIC_DRAW).data(GL_UNIFORM_BUFFER, GlVulkanSharedBuffer.uniformStride * GlVulkanSharedBuffer.maxDrawCalls, GL_DYNAMIC_DRAW);
            GlVulkanSharedBuffer.uniformDataBuffer.deallocate().allocate(GlVulkanSharedBuffer.uniformStride * GlVulkanSharedBuffer.maxDrawCalls, GL_DYNAMIC_DRAW).data(GL_UNIFORM_BUFFER, GlVulkanSharedBuffer.uniformStride * GlVulkanSharedBuffer.maxDrawCalls, GL_DYNAMIC_DRAW);

            //
            glFinish();
        }
    }

    // collect draw calls for batch draw and acceleration structure
    // getting errors when any text rendering
    public static void collectDraw(int mode, int count, int type, long indices) throws Exception {
        // don't record GUI, or other trash
        if (!GlContext.worldRendering) { return; }

        // isn't valid! must be drawn in another layer, and directly.
        if (mode != GL_TRIANGLES) { return; }

        // TODO: uint8 index type may to be broken or corrupted...
        if (type == GL_UNSIGNED_BYTE || type == GL_BYTE) { return; }

        //
        if (drawCount >= GlVulkanSharedBuffer.maxDrawCalls) {
            System.out.println("WARNING! Draw Call Limit Exceeded...");
            return;
        }

        //
        var _pipelineLayout = GlContext.rendererObj.pipelineLayout;
        var _memoryAllocator = GlContext.rendererObj.memoryAllocator;

        //
        var boundVertexBuffer = GlContext.boundVertexBuffer;
        var boundVertexFormat = boundVertexBuffer.getVertexFormat();
        var boundShaderProgram = GlContext.boundShaderProgram; // only for download a uniform data

        //
        if (boundVertexBuffer == null) { System.out.println("Vertex Buffer Was Not Bound..."); return; };
        if (boundVertexFormat == null) { System.out.println("Vertex Format Was Not Bound..."); return; };
        if (boundShaderProgram == null) { System.out.println("Shader Program Was Not Bound..."); return; };

        //
        var boundVertexBufferI = (VertexBufferInterface)boundVertexBuffer;
        var boundVertexFormatI = (VertexFormatInterface)boundVertexFormat;
        var boundShaderProgramI = (ShaderProgramInterface)boundShaderProgram; // only for download a uniform data

        // TODO: direct and zero-copy host memory support
        // TODO: fix broken boundVertexBuffer support
        var vao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        var virtualVertexBuffer = GlContext.boundWithVao.get(vao);
        var virtualIndexBuffer = GlContext.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER);
        //var virtualIndexBuffer = boundVertexBufferI.getIndexBufferId() > 0 ? GlContext.virtualBufferMap.get(boundVertexBufferI.getIndexBufferId()) : GlContext.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER);
        //if (virtualIndexBuffer.realSize <= 0) { virtualIndexBuffer = GlContext.boundBuffers.get(GL_ELEMENT_ARRAY_BUFFER); };

        //
        var drawCallData = new DrawCallObj();
        drawCallData.indexBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(1);
        drawCallData.vertexBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(1);

        //
        drawCallData.uniformDataBuffer = GlVulkanSharedBuffer.uniformDataBuffer;//new GlVulkanVirtualBuffer.VirtualBufferObj(1);
        drawCallData.uniformOffset = GlVulkanSharedBuffer.uniformStride * drawCount;

        // TODO: allocation limiter support
        drawCallData.vertexBuffer.allocate(virtualVertexBuffer.realSize, GL_DYNAMIC_DRAW).data(GL_ARRAY_BUFFER, virtualVertexBuffer.realSize, GL_DYNAMIC_DRAW);
        drawCallData.indexBuffer.allocate(virtualIndexBuffer.realSize, GL_DYNAMIC_DRAW).data(GL_ELEMENT_ARRAY_BUFFER, virtualIndexBuffer.realSize, GL_DYNAMIC_DRAW);

        //
        drawCallData.primitiveCount = count/3;
        drawCallData.elementCount = count;
        drawCallData.vertexBuffer.stride = virtualVertexBuffer.stride;//boundVertexFormat.getVertexSizeByte();
        drawCallData.indexBuffer.indexType = VK_INDEX_TYPE_UINT32;
        drawCallData.indexBuffer.stride = 4;

        //
        if (type == GL_UNSIGNED_BYTE  || type == GL_BYTE ) { drawCallData.indexBuffer.indexType = VK_INDEX_TYPE_UINT8_EXT; drawCallData.indexBuffer.stride = 1; }
        if (type == GL_UNSIGNED_SHORT || type == GL_SHORT) { drawCallData.indexBuffer.indexType = VK_INDEX_TYPE_UINT16; drawCallData.indexBuffer.stride = 2; }
        if (type == GL_UNSIGNED_INT   || type == GL_INT  ) { drawCallData.indexBuffer.indexType = VK_INDEX_TYPE_UINT32; drawCallData.indexBuffer.stride = 4; }

        // TODO: fill uniform data
        var uniformData = memSlice(GlVulkanSharedBuffer.uniformDataBufferHost.map(GL_UNIFORM_BUFFER, GL_MAP_WRITE_BIT), (int) drawCallData.uniformOffset, (int) GlVulkanSharedBuffer.uniformStride);

        //
        var vertexFEL = boundVertexFormatI.getElementMap();
        var offsetMap = boundVertexFormatI.getOffsets();
        var keyList = vertexFEL.keySet().asList();

        // TODO: replace by array based
        var posElement = vertexFEL.get("Position");
        var posOffset = posElement != null ? offsetMap.getInt(keyList.indexOf("Position")) : 0;
        drawCallData.vertexBinding = new VirtualTempBinding();
        drawCallData.vertexBinding.virtualBuffer = posElement != null ? drawCallData.vertexBuffer : null;
        drawCallData.vertexBinding.format = VK_FORMAT_R32G32B32_SFLOAT;
        drawCallData.vertexBinding.relativeOffset = posOffset;

        // TODO: replace by array based
        var uvElement = vertexFEL.get("UV0");
        var uvOffset = uvElement != null ? offsetMap.getInt(keyList.indexOf("UV0")) : 0;
        drawCallData.uvBinding = new VirtualTempBinding();
        drawCallData.uvBinding.virtualBuffer = uvElement != null ? drawCallData.vertexBuffer : null;
        drawCallData.uvBinding.format = VK_FORMAT_R32G32_SFLOAT;
        drawCallData.uvBinding.relativeOffset = uvOffset;

        // TODO: replace by array based
        var colorElement = vertexFEL.get("Color");
        var colorOffset = colorElement != null ? offsetMap.getInt(keyList.indexOf("Color")) : 0;
        drawCallData.colorBinding = new VirtualTempBinding();
        drawCallData.colorBinding.virtualBuffer = colorElement != null ? drawCallData.vertexBuffer : null;
        drawCallData.colorBinding.format = VK_FORMAT_R32_UINT; // rgba8unorm de-facto
        drawCallData.colorBinding.relativeOffset = colorOffset;

        // TODO: replace by array based
        var normalElement = vertexFEL.get("Normal");
        var normalOffset = normalElement != null ? offsetMap.getInt(keyList.indexOf("Normal")) : 0;
        drawCallData.normalBinding = new VirtualTempBinding();
        drawCallData.normalBinding.virtualBuffer = normalElement != null ? drawCallData.vertexBuffer : null;
        drawCallData.normalBinding.format = VK_FORMAT_R32_UINT; // rgba8snorm de-facto
        drawCallData.normalBinding.relativeOffset = normalOffset;

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
        memSlice(uniformData, 16*4, 8).putLong(0, drawCallData.indexBuffer.address);
        memSlice(uniformData, 16*4 + 8, 4).putInt(0, drawCallData.indexBuffer.indexType);
        memSlice(uniformData, 16*4 + 8 + 4, 4).putInt(0, drawCallData.indexBuffer.stride);

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

        // is OpenGL only...
        var vOffset = virtualVertexBuffer.offset.get(0);
        var iOffset = virtualIndexBuffer.offset.get(0);

        // is Vulkan with OpenGL shared!
        var _vOffset = drawCallData.vertexBuffer.offset.get(0);
        var _iOffset = drawCallData.indexBuffer.offset.get(0);

        //
        GL45.glFinish();
        GL45.glCopyNamedBufferSubData(
            virtualVertexBuffer.glStorageBuffer, drawCallData.vertexBuffer.glStorageBuffer,
                vOffset, _vOffset,
            virtualVertexBuffer.realSize);
        GL45.glCopyNamedBufferSubData(
            virtualIndexBuffer.glStorageBuffer, drawCallData.indexBuffer.glStorageBuffer,
                iOffset, _iOffset,
            virtualIndexBuffer.realSize);

        // OpenGL, you are drunk?
        GL45.glFinish();

        //
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
        if (GlVulkanSharedBuffer.uniformDataBufferHost.glStorageBuffer > 0 && GlVulkanSharedBuffer.uniformDataBuffer.glStorageBuffer > 0) {
            GL45.glFinish();
            GL45.glCopyNamedBufferSubData(
                    GlVulkanSharedBuffer.uniformDataBufferHost.glStorageBuffer, GlVulkanSharedBuffer.uniformDataBuffer.glStorageBuffer,
                    GlVulkanSharedBuffer.uniformDataBufferHost.offset.get(0), GlVulkanSharedBuffer.uniformDataBuffer.offset.get(0),
                    GlVulkanSharedBuffer.uniformStride * collectedDraws.size());

            // OpenGL, you are drunk?
            GL45.glFinish();
        }

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
                    address = cDraw.vertexBinding.virtualBuffer.address + cDraw.vertexBinding.relativeOffset;
                    stride = cDraw.vertexBinding.virtualBuffer.stride;
                    vertexCount = (int) (cDraw.vertexBinding.virtualBuffer.realSize / cDraw.vertexBinding.virtualBuffer.stride);
                    format = cDraw.vertexBinding.format;
                }};
                indexBinding = new DataCInfo.IndexBindingCInfo() {{
                    address = cDraw.indexBuffer.address;
                    vertexCount = cDraw.elementCount;
                    type = cDraw.indexBuffer.indexType;
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
