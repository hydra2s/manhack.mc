package org.hydra2s.manhack.mixin.blaze3d;

import com.mojang.blaze3d.systems.RenderSystem;
import org.hydra2s.manhack.mixin.interfaces.ShapeIndexBufferInterface;
import org.spongepowered.asm.mixin.Mixin;

// allocate vulkan buffer instead
// TODO: vulkan memory mapping
// TODO: unbound data for built buffer memory
// TODO: unbound from OpenGL API
// TODO: unlock triangulator

@Mixin(RenderSystem.ShapeIndexBuffer.class)
public class ShapeIndexBufferMixin implements ShapeIndexBufferInterface {
}
