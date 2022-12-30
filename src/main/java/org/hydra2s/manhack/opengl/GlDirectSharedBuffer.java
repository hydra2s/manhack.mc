package org.hydra2s.manhack.opengl;

//
import org.hydra2s.manhack.GlContext;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL45;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;

//
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL30.GL_MAP_READ_BIT;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.util.vma.Vma.vmaCreateVirtualBlock;

//
public class GlDirectSharedBuffer {

    //
    public static void initialize() throws IOException {
        sharedBufferMap = new HashMap<Integer, GlSharedBuffer>(){{
            put(0, createBuffer(1024L * 1024L * 256L)); // for vertices
            put(1, createBuffer(1024L * 1024L * 256L)); // for indices
        }};
        //initialize();
    };

    //
    static public class GlSharedBuffer {
        public int glStorageBuffer = 0;
        public PointerBuffer vb;

        // also, is this full size
        VmaVirtualBlockCreateInfo vbInfo = VmaVirtualBlockCreateInfo.create().flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT);
    };

    // TODO: support for typed (entity, indexed, blocks, etc.)
    public static Map<Integer, GlSharedBuffer> sharedBufferMap = new HashMap<Integer, GlSharedBuffer>();

    // TODO: needs fully replace OpenGL buffer memory stack
    // TODO: needs immutable storage and ranges support
    public static GlSharedBuffer createBuffer(long defaultSize) {
        GlSharedBuffer resource = new GlSharedBuffer();
        var _pipelineLayout = GlContext.rendererObj.pipelineLayout;
        var _memoryAllocator = GlContext.rendererObj.memoryAllocator;

        //
        vmaCreateVirtualBlock(resource.vbInfo.size(defaultSize), resource.vb = memAllocPointer(1));

        //
        GL45.glNamedBufferStorage(resource.glStorageBuffer = GL45.glCreateBuffers(), defaultSize, GL_CLIENT_STORAGE_BIT | GL_DYNAMIC_STORAGE_BIT | GL_MAP_READ_BIT | GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        //GL45.glNamedBufferData(resource.glStorageBuffer = GL45.glCreateBuffers(), defaultSize, GL_DYNAMIC_DRAW);

        return resource;
    };

}
