package org.hydra2s.manhack.mixin.vertex;

//
import net.minecraft.client.render.VertexFormatElement;
import org.hydra2s.manhack.ducks.vertex.VertexFormatElementTypeInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

//
@Mixin(VertexFormatElement.Type.class)
public class VertexFormatElementTypeMixin implements VertexFormatElementTypeInterface {
    @Final @Shadow private String name;

    // accessWidener public
    @Final @Shadow public VertexFormatElement.Type.SetupTask setupTask;
    @Final @Shadow public VertexFormatElement.Type.ClearTask clearTask;
}
