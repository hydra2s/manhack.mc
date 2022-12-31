package org.hydra2s.manhack.virtual.vertex;

//
import net.minecraft.client.render.VertexFormatElement;
import org.hydra2s.manhack.virtual.buffer.GlBaseVirtualBuffer;

//
import java.util.HashMap;

//
public class GlVirtualVertexArrayObj {

    // virtual binding system
    // TODO: needs a `VertexFormatElement` or no?
    public HashMap<Integer, VertexFormatElement> vertexElements;
    public HashMap<Integer, GlBaseVirtualBuffer.VirtualBufferObj> boundBuffers;

    // TODO: Unresolved about ordering, <String, Integer> or <Integer, String>
    public HashMap<String, Integer> typeMapping;


}
