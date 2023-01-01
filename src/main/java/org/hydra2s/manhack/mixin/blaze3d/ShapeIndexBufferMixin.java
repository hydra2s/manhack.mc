package org.hydra2s.manhack.mixin.blaze3d;

//
import com.mojang.blaze3d.systems.RenderSystem;
import org.hydra2s.manhack.ducks.blaze3d.ShapeIndexBufferInterface;
import org.spongepowered.asm.mixin.Mixin;

// allocate vulkan buffer instead
@Mixin(RenderSystem.ShapeIndexBuffer.class)
public class ShapeIndexBufferMixin implements ShapeIndexBufferInterface {



}
