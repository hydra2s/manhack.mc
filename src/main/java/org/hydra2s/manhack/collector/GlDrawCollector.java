package org.hydra2s.manhack.collector;

// TODO:
// - Add buffer copying to temp
// - Add geometry generation for AS
// - Add material inbound with geometry
// - Support for Vulkan API shaders structures (UBO, etc.)

//
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.ducks.render.ShaderProgramInterface;
import org.hydra2s.manhack.ducks.vertex.VertexBufferInterface;
import org.hydra2s.manhack.ducks.vertex.VertexFormatInterface;
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedBuffer;
import org.hydra2s.manhack.virtual.buffer.GlVulkanVirtualBuffer;
import org.lwjgl.opengl.GL45;

//
import java.nio.ByteBuffer;
import java.util.ArrayList;

//
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.vmaClearVirtualBlock;
import static org.lwjgl.util.vma.Vma.vmaCreateVirtualBlock;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_UNDEFINED;

//
public class GlDrawCollector {

    //
    public static class VirtualTempBinding {
        // re-wrote info
        long relativeOffset = 0L;
        long byteSize = 0L;
        long address = 0L;
        long stride = 0L;

        //
        int binding = 0;
        int format = VK_FORMAT_UNDEFINED;

        static final int bindingStride = 8 + 8 + 4 + 4 + 4 + 4; // 32-byte binding

        //
        public void writeBinding(ByteBuffer by, long bfOffset, long bfIndex) {
            // address, offset, format, stride, size

            var bOffset = (int) (bfOffset + 0 + bfIndex * bindingStride);
            by.putLong( bOffset + 0, address);
            by.putLong(bOffset + 8, byteSize - relativeOffset);
            by.putInt(bOffset + 16, (int) relativeOffset);
            by.putInt(bOffset + 20, (int) stride);
            by.putInt(bOffset + 24, format);
        }
    }

    //
    public static class DrawCallObj {
        // buffers
        public GlVulkanVirtualBuffer.VirtualBufferObj indexBuffer;
        public GlVulkanVirtualBuffer.VirtualBufferObj vertexBuffer;

        // TODO: needs it?! Sharing only...
        public GlVulkanVirtualBuffer.VirtualBufferObj uniformDataBuffer;

        // bindings
        public VirtualTempBinding vertexBinding;
        public VirtualTempBinding normalBinding;
        public VirtualTempBinding colorBinding;
        public VirtualTempBinding uvBinding;

        // only this is really useful
        public long uniformOffset = 0L;
        public int primitiveCount = 0;

        // rewrite from uniform data
        //public IntList vkImageViews; // will paired with `uniformDataBuffer`
    }

    // will reset and deallocated every draw...
    public static ArrayList<DrawCallObj> collectedDraws = new ArrayList<DrawCallObj>();
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

    // TODO: add forgotten support for UINT8 index data
    public static void collectDraw(int mode, int count, int type, long indices) throws Exception {
        // isn't valid!
        if (mode != GL_TRIANGLES) { return; };

        // TODO: uint8 index type support is broken! I forgot to add such extension.
        if (type == GL_UNSIGNED_BYTE || type == GL_BYTE) { return; };

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
        drawCallData.uniformDataBuffer = GlVulkanSharedBuffer.uniformDataBuffer;//new GlVulkanVirtualBuffer.VirtualBufferObj(1);
        drawCallData.uniformOffset = GlVulkanSharedBuffer.uniformStride * drawCount;
        drawCallData.primitiveCount = count/3;

        // TODO: allocation limiter support
        drawCallData.vertexBuffer.data(GL_ARRAY_BUFFER, virtualVertexBuffer.realSize, GL_DYNAMIC_DRAW);
        drawCallData.indexBuffer.data(GL_ELEMENT_ARRAY_BUFFER, virtualIndexBuffer.realSize, GL_DYNAMIC_DRAW);

        // TODO: fill uniform data
        var uniformData = GlVulkanSharedBuffer.uniformDataBufferHost.map(GL_UNIFORM_BUFFER, GL_MAP_WRITE_BIT);
        for (var I=0;I<16;I++) {}; // set transform
        var transformOffset = 16*4;

        //
        drawCallData.vertexBinding.writeBinding(uniformData, transformOffset, 0);
        drawCallData.normalBinding.writeBinding(uniformData, transformOffset, 1);
        drawCallData.uvBinding.writeBinding(uniformData, transformOffset, 2);
        drawCallData.colorBinding.writeBinding(uniformData, transformOffset, 3);

        // TODO: fill metadata and combined samplers
        var metaOffset = transformOffset + VirtualTempBinding.bindingStride*4;
        
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

    }

    // probably, prepare buffers and commands (indirect draw)
    public static void preBuildDraw() {

    }

}
