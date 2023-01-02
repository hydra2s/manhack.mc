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
import org.hydra2s.manhack.virtual.buffer.GlVulkanVirtualBuffer;
import org.hydra2s.noire.descriptors.AccelerationStructureCInfo;
import org.hydra2s.noire.descriptors.DataCInfo;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL45;
import org.lwjgl.vulkan.VkAccelerationStructureBuildRangeInfoKHR;

//
import java.nio.ByteBuffer;
import java.util.ArrayList;

//
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
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
                by.putLong(bOffset, virtualBuffer.address + relativeOffset);
                by.putLong(bOffset + 8, virtualBuffer.realSize - relativeOffset);
                by.putInt(bOffset + 16, (int) relativeOffset);
                by.putInt(bOffset + 20, virtualBuffer.stride);
                by.putInt(bOffset + 24, format); // TODO: custom format encoding for shader
            } else {
                by.putLong(bOffset, 0);
                by.putLong(bOffset + 8, 0);
                by.putInt(bOffset + 16, 0);
                by.putInt(bOffset + 20, 0);
                by.putInt(bOffset + 24, VK_FORMAT_UNDEFINED);
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

        // rewrite from uniform data
        //public IntList vkImageViews; // will paired with `uniformDataBuffer`
    }

    // will reset and deallocated every draw...
    public static ArrayList<DrawCallObj> collectedDraws = new ArrayList<>();
    public static int drawCount = 0;

    // deallocate and reset all draws data
    public static void resetDraw() {
        var sharedBuffer = GlVulkanSharedBuffer.sharedBufferMap.get(1);
        
        //
        collectedDraws.forEach((drawCall)->{
            try {
                drawCall.vertexBuffer.delete();
                drawCall.indexBuffer.delete();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        collectedDraws.clear();
        drawCount = 0;

        //
        if (sharedBuffer.vb.get(0) != 0) {
            vmaClearVirtualBlock(sharedBuffer.vb.get(0));
        }

        //
        //vmaCreateVirtualBlock(sharedBuffer.vbInfo.size(sharedBuffer.bufferCreateInfo.size), sharedBuffer.vb = memAllocPointer(1));
    }

    // collect draw calls for batch draw and acceleration structure
    public static void collectDraw(int mode, int count, int type, long indices) throws Exception {
        // don't record GUI, or other trash
        if (!GlContext.worldRendering) { return; }

        // isn't valid! must be drawn in another layer, and directly.
        if (mode != GL_TRIANGLES) { return; }

        // TODO: uint8 index type may to be broken or corrupted...
        //if (type == GL_UNSIGNED_BYTE || type == GL_BYTE) { return; }

        //
        if (drawCount >= GlVulkanSharedBuffer.maxDrawCalls) {
            System.out.println("WARNING! Draw Call Limit Exceeded...");
            return;
        }

        //
        var boundVertexBuffer = GlContext.boundVertexBuffer;
        var boundVertexFormat = GlContext.boundVertexFormat;
        var boundShaderProgram = GlContext.boundShaderProgram; // only for download a uniform data

        //
        var boundVertexBufferI = (VertexBufferInterface)boundVertexBuffer;
        var boundVertexFormatI = (VertexFormatInterface)boundVertexFormat;
        var boundShaderProgramI = (ShaderProgramInterface)boundShaderProgram; // only for download a uniform data

        // TODO: direct and zero-copy host memory support
        var virtualVertexBuffer = GlContext.virtualBufferMap.get(boundVertexBufferI.getVertexBufferId());
        var virtualIndexBuffer = GlContext.virtualBufferMap.get(boundVertexBufferI.getIndexBufferId());

        //
        var drawCallData = new DrawCallObj();
        drawCallData.indexBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(1);
        drawCallData.vertexBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(1);

        //
        drawCallData.uniformDataBuffer = GlVulkanSharedBuffer.uniformDataBuffer;//new GlVulkanVirtualBuffer.VirtualBufferObj(1);
        drawCallData.uniformOffset = GlVulkanSharedBuffer.uniformStride * drawCount;

        // TODO: allocation limiter support
        drawCallData.vertexBuffer.data(GL_ARRAY_BUFFER, virtualVertexBuffer.realSize, GL_DYNAMIC_DRAW);
        drawCallData.indexBuffer.data(GL_ELEMENT_ARRAY_BUFFER, virtualIndexBuffer.realSize, GL_DYNAMIC_DRAW);
        drawCallData.primitiveCount = count/3;

        //
        if (type == GL_UNSIGNED_BYTE  || type == GL_BYTE ) { drawCallData.indexBuffer.indexType = VK_INDEX_TYPE_UINT8_EXT; }
        if (type == GL_UNSIGNED_SHORT || type == GL_SHORT) { drawCallData.indexBuffer.indexType = VK_INDEX_TYPE_UINT16; }
        if (type == GL_UNSIGNED_INT   || type == GL_INT  ) { drawCallData.indexBuffer.indexType = VK_INDEX_TYPE_UINT32; }

        // TODO: fill uniform data
        var uniformData = GlVulkanSharedBuffer.uniformDataBufferHost.map(GL_UNIFORM_BUFFER, GL_MAP_WRITE_BIT);

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

        // will used in top level of acceleration structure
        var playerCamera = MinecraftClient.getInstance().gameRenderer.getCamera();

        //
        var chunkOffset = boundShaderProgram.chunkOffset != null ? boundShaderProgram.chunkOffset.getFloatData() : memAllocFloat(3).put(0, 0.F).put(1, 0.F).put(2, 0.F);

        // needs for acceleration structure
        var transform = new Matrix4f();
        //transform.translate(new Vector3f((float) (chunkOffset.get(0) - playerCamera.getPos().x), (float) (chunkOffset.get(1) - playerCamera.getPos().y), (float) (chunkOffset.get(2) - playerCamera.getPos().z)));
        transform.translate(new Vector3f(chunkOffset.get(0), chunkOffset.get(1), chunkOffset.get(2)));
        transform.transpose().get(uniformData);

        //
        var bindingOffset = 16*4;

        // TODO: replace by array based
        drawCallData.vertexBinding.writeBinding(uniformData, bindingOffset, 0);
        drawCallData.normalBinding.writeBinding(uniformData, bindingOffset, 1);
        drawCallData.uvBinding.writeBinding(uniformData, bindingOffset, 2);
        drawCallData.colorBinding.writeBinding(uniformData, bindingOffset, 3);

        // TODO: fill metadata and combined samplers
        var metaOffset = bindingOffset + VirtualTempBinding.bindingStride*4;

        //
        var object = boundShaderProgramI.getSamplers().get("Sampler0");
        int l = -1;
        if (object instanceof Framebuffer) {
            l = ((Framebuffer)object).getColorAttachment();
        } else if (object instanceof AbstractTexture) {
            l = ((AbstractTexture)object).getGlId();
        } else if (object instanceof Integer) {
            l = (Integer)object;
        }

        // TODO: get image view pipeline layout index
        var vkTexture = GlVulkanSharedTexture.sharedImageMap.get(l);
        if (vkTexture != null) {

        }



        // TODO: copy using Vulkan API!
        GL45.glMemoryBarrier(GL_ELEMENT_ARRAY_BARRIER_BIT | GL_BUFFER_UPDATE_BARRIER_BIT | GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT | GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
        GL45.glCopyNamedBufferSubData(
            virtualVertexBuffer.glStorageBuffer, drawCallData.vertexBuffer.glStorageBuffer,
            virtualVertexBuffer.offset.get(0),   drawCallData.vertexBuffer.offset.get(0),
            virtualVertexBuffer.realSize);
        GL45.glCopyNamedBufferSubData(
            virtualIndexBuffer.glStorageBuffer, drawCallData.indexBuffer.glStorageBuffer,
            virtualIndexBuffer.offset.get(0),   drawCallData.indexBuffer.offset.get(0),
            virtualIndexBuffer.realSize);
        GL45.glCopyNamedBufferSubData(
            GlVulkanSharedBuffer.uniformDataBufferHost.glStorageBuffer, drawCallData.uniformDataBuffer.glStorageBuffer,
            GlVulkanSharedBuffer.uniformDataBufferHost.offset.get(0),   drawCallData.uniformDataBuffer.offset.get(0),
            GlVulkanSharedBuffer.uniformStride);

        //
        collectedDraws.add(drawCallData);
        drawCount++;
    }

    // for building draw into acceleration structures
    public static void buildDraw() {
        var cInfo = (AccelerationStructureCInfo)GlVulkanSharedBuffer.bottomLvl.cInfo;
        cInfo.geometries.clear();

        //
        GlVulkanSharedBuffer.drawRanges = VkAccelerationStructureBuildRangeInfoKHR.calloc(collectedDraws.size());
        for (int I=0;I<collectedDraws.size();I++) {

            //
            var cDraw = collectedDraws.get(I);
            GlVulkanSharedBuffer.drawRanges.get(I)
                .primitiveCount(cDraw.primitiveCount)
                .firstVertex(0)
                .primitiveOffset(0)
                .transformOffset(0);

            //
            cInfo.geometries.add(new DataCInfo.TriangleGeometryCInfo() {{
                vertexBinding = new DataCInfo.VertexBindingCInfo() {{
                    address = cDraw.vertexBinding.virtualBuffer.address + cDraw.vertexBinding.relativeOffset;
                    stride = cDraw.vertexBinding.virtualBuffer.stride;
                    vertexCount = (int) (cDraw.vertexBinding.virtualBuffer.realSize / cDraw.vertexBinding.virtualBuffer.stride);
                    format = cDraw.vertexBinding.format;
                }};
                indexBinding = new DataCInfo.IndexBindingCInfo() {{
                    address = cDraw.indexBuffer.address;
                    vertexCount = (int) (cDraw.indexBuffer.realSize / cDraw.indexBuffer.stride);
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
