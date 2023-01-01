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

//
import java.util.ArrayList;

//
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
    public static class VirtualTempBuffer {
        long offset = 0L;
        long size = 0L;
        long address = 0L;
        long stride = 0L;

        // where to bind it
        int binding = 0;
        int sharedId = 1; // which will used

        // for index buffer
        int format = VK_FORMAT_UNDEFINED;
    }

    //
    public static class GeometryDataObj {
        // buffers
        public VirtualTempBuffer indexBuffer;
        public VirtualTempBuffer vertexBuffer;
        public VirtualTempBuffer uniformDataBuffer;

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
        collectedDraws.forEach((drawCall)->{

        });
        collectedDraws.clear();
    }

    //
    public static void collectDraw(int mode, int count, int type, long indices) {
        //
        var boundVertexBuffer = GlContext.boundVertexBuffer;
        var boundVertexFormat = GlContext.boundVertexFormat;
        var boundShaderProgram = GlContext.boundShaderProgram; // only for download a uniform data

        //
        var boundVertexBufferI = (VertexBufferInterface)boundVertexBuffer;
        var boundVertexFormatI = (VertexFormatInterface)boundVertexFormat;
        var boundShaderProgramI = (ShaderProgramInterface)boundShaderProgram; // only for download a uniform data

        //

    }

    // for building draw into acceleration structures
    public static void buildDraw() {

    }

    // probably, prepare buffers and commands (indirect draw)
    public static void preBuildDraw() {

    }

}
