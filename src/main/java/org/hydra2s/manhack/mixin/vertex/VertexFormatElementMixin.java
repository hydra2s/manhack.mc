package org.hydra2s.manhack.mixin.vertex;

import net.minecraft.client.render.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexFormatElement.class)
public class VertexFormatElementMixin {
    @Shadow private VertexFormatElement.Type type;
    @Shadow private VertexFormatElement.ComponentType componentType;
    @Shadow private int uvIndex;
    @Shadow private int componentCount;
    @Shadow private int byteLength;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void setupState(int elementIndex, long offset, int stride) {
        this.type.setupState(this.componentCount, this.componentType.getGlType(), stride, offset, this.uvIndex, elementIndex);
    }

}
