package org.hydra2s.manhack.mixin.vertex;

import net.minecraft.client.render.VertexFormatElement;
import org.hydra2s.manhack.ducks.vertex.VertexFormatElementInterface;
import org.spongepowered.asm.mixin.*;

@Mixin(VertexFormatElement.class)
public class VertexFormatElementMixin implements VertexFormatElementInterface {
    //
    @Final @Shadow private VertexFormatElement.Type type;
    @Final @Shadow private VertexFormatElement.ComponentType componentType;

    //
    @Final @Shadow private int uvIndex;
    @Final @Shadow private int componentCount;
    @Final @Shadow private int byteLength;

    /**
     * @author
     * @reason
     */
    @Overwrite public void setupState(int elementIndex, long offset, int stride) {
        this.type.setupState(this.componentCount, this.componentType.getGlType(), stride, offset, this.uvIndex, elementIndex);
    }

    //
    @Unique @Override public int getByteLength() {
        return this.byteLength;
    }
    @Unique @Override public int getComponentCount() {
        return this.componentCount;
    }
    @Unique @Override public int getUvIndex() {
        return this.uvIndex;
    }

    //
    @Unique @Override public VertexFormatElement.ComponentType getComponentType() {
        return this.componentType;
    }

    @Unique @Override public VertexFormatElement.Type getType() {
        return this.type;
    }

}
