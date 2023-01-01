package org.hydra2s.manhack.mixin.vertex;

//
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import org.hydra2s.manhack.ducks.vertex.VertexFormatInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//
@Mixin(VertexFormat.class)
public class VertexFormatMixin implements VertexFormatInterface {

    //
    @Final @Shadow private ImmutableList<VertexFormatElement> elements;
    @Final @Shadow private ImmutableMap<String, VertexFormatElement> elementMap;
    @Final @Shadow private IntList offsets = new IntArrayList();
    @Final @Shadow private int vertexSizeByte;
    @Shadow private VertexBuffer buffer;

    //
    @Override public ImmutableList<VertexFormatElement> getElements() { return this.elements; }
    @Override public ImmutableMap<String, VertexFormatElement> getElementMap() { return this.elementMap; }
    @Override public IntList getOffsets() { return this.offsets; }
    @Override public int getVertexSizeByte() { return this.vertexSizeByte; }
    @Override public VertexBuffer getBuffer() { return buffer; }

}
