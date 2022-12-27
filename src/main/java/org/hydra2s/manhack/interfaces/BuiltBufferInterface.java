package org.hydra2s.manhack.interfaces;

import org.hydra2s.manhack.GlContext;

import java.nio.ByteBuffer;

public interface BuiltBufferInterface {
    public int getGlVertexBuffer();
    public int getGlIndexBuffer();
    public GlContext.ResourceBuffer getVkVertexBuffer();
    public GlContext.ResourceBuffer getVkIndexBuffer();
    public boolean syncData();
    public void fromIndexData(ByteBuffer preAllocated);

}
