package org.hydra2s.manhack.mixin.vertex;

//
import net.minecraft.client.render.VertexFormatElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//
@Mixin(VertexFormatElement.Type.class)
public class VertexFormatElementTypeMixin {
    @Final @Shadow private String name;
    @Final @Shadow public VertexFormatElement.Type.SetupTask setupTask;
    @Final @Shadow public VertexFormatElement.Type.ClearTask clearTask;
}
