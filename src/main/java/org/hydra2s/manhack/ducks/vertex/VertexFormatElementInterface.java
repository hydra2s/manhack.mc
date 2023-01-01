package org.hydra2s.manhack.ducks.vertex;

import net.minecraft.client.render.VertexFormatElement;

//
public interface VertexFormatElementInterface {

    int getByteLength();

    int getComponentCount();

    int getUvIndex();

    VertexFormatElement.ComponentType getComponentType();

    VertexFormatElement.Type getType();
}
