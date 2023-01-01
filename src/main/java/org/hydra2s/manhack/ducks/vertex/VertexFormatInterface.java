package org.hydra2s.manhack.ducks.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormatElement;

//
public interface VertexFormatInterface {

    ImmutableList<VertexFormatElement> getElements();

    ImmutableMap<String, VertexFormatElement> getElementMap();

    IntList getOffsets();

    int getVertexSizeByte();

    VertexBuffer getBuffer();
}
