package org.hydra2s.manhack.mixin.vertex;

//
import net.minecraft.client.render.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//
@Mixin(VertexFormatElement.Type.class)
public class VertexFormatElementTypeMixin {
    @Shadow private String name;
    @Shadow private VertexFormatElement.Type.SetupTask setupTask;
    @Shadow private VertexFormatElement.Type.ClearTask clearTask;
}
