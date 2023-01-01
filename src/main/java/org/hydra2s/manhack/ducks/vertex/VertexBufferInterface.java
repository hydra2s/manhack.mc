package org.hydra2s.manhack.ducks.vertex;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexFormat;

public interface VertexBufferInterface {
    int getVertexArrayId();

    int getVertexBufferId();

    int getIndexBufferId();

    int getIndexCount();

    VertexFormat.IndexType getIndexType();

    RenderSystem.ShapeIndexBuffer getSharedSequentialIndexBuffer();

    VertexFormat.DrawMode getDrawMode();
}
