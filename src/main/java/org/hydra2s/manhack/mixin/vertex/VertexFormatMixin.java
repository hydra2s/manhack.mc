package org.hydra2s.manhack.mixin.vertex;

//
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntList;
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
    @Final @Shadow private ImmutableMap<String, VertexFormatElement> elementMap;
    @Final @Shadow private IntList offsets;

    //
    @Override public ImmutableMap<String, VertexFormatElement> getElementMap() { return this.elementMap; }
    @Override public IntList getOffsets() { return this.offsets; }
}
