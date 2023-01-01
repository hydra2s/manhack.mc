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
import java.util.ArrayList;

//
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.vmaClearVirtualBlock;
import static org.lwjgl.util.vma.Vma.vmaCreateVirtualBlock;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_UNDEFINED;

//
public class GlDrawCollector {

    //
    public static class VirtualTempBinding {
        // re-wrote info
        long offset = 0L;
        long size = 0L;
        long address = 0L;
        long stride = 0L;

        //
        int binding = 0;
        int format = VK_FORMAT_UNDEFINED;
    }

    //
    public static class GeometryDataObj {
        // buffers
        public GlVulkanVirtualBuffer.VirtualBufferObj indexBuffer;
        public GlVulkanVirtualBuffer.VirtualBufferObj vertexBuffer;
        public GlVulkanVirtualBuffer.VirtualBufferObj uniformDataBuffer;

        // bindings
        public VirtualTempBinding vertexBinding;
        public VirtualTempBinding colorBinding;
        public VirtualTempBinding uvBinding;

        // rewrite from uniform data
        //public IntList vkImageViews; // will paired with `uniformDataBuffer`
    }

    // will reset and deallocated every draw...
    public static ArrayList<GeometryDataObj> collectedDraws = new ArrayList<GeometryDataObj>();

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

        //
        if (sharedBuffer.vb.get(0) != 0) {
            vmaClearVirtualBlock(sharedBuffer.vb.get(0));
        }

        //
        //vmaCreateVirtualBlock(sharedBuffer.vbInfo.size(sharedBuffer.bufferCreateInfo.size), sharedBuffer.vb = memAllocPointer(1));
    }

    //
    public static void collectDraw(int mode, int count, int type, long indices) throws Exception {
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
        var geometryData = new GeometryDataObj();
        geometryData.indexBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(1);
        geometryData.vertexBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(1);
        geometryData.uniformDataBuffer = new GlVulkanVirtualBuffer.VirtualBufferObj(1);

        // TODO: allocation limiter support
        geometryData.uniformDataBuffer.data(GL_SHADER_STORAGE_BUFFER, 256, GL_DYNAMIC_DRAW);
        geometryData.vertexBuffer.data(GL_VERTEX_ARRAY, virtualVertexBuffer.realSize, GL_DYNAMIC_DRAW);
        geometryData.indexBuffer.data(GL_VERTEX_ARRAY, virtualIndexBuffer.realSize, GL_DYNAMIC_DRAW);

        // TODO: copy using Vulkan API!
        GL45.glCopyNamedBufferSubData(virtualVertexBuffer.glStorageBuffer, geometryData.vertexBuffer.glStorageBuffer, virtualVertexBuffer.offset.get(0), geometryData.vertexBuffer.offset.get(0), virtualVertexBuffer.realSize);
        GL45.glCopyNamedBufferSubData(virtualIndexBuffer.glStorageBuffer, geometryData.indexBuffer.glStorageBuffer, virtualIndexBuffer.offset.get(0), geometryData.indexBuffer.offset.get(0), virtualIndexBuffer.realSize);

        //

    }

    // for building draw into acceleration structures
    public static void buildDraw() {

    }

    // probably, prepare buffers and commands (indirect draw)
    public static void preBuildDraw() {

    }

}
