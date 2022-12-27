package org.hydra2s.manhack.mixin.render;

//
import net.minecraft.client.render.BufferRenderer;
import org.spongepowered.asm.mixin.Mixin;

// BIG TODO!
// - Remove `private static VertexBuffer bind(VertexFormat vertexFormat)`, due unsuitable for Vulkan API
// - Remove `private static void bind(VertexBuffer vertexBuffer)`, due unsuitable unsuitable for Vulkan API
//

//
@Mixin(BufferRenderer.class)
public abstract class BufferRendererMixin {

}
