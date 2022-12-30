package org.hydra2s.manhack.mixin.render;

import net.minecraft.client.render.BufferBuilder;
import org.hydra2s.manhack.ducks.render.BuiltBufferInterface;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BufferBuilder.BuiltBuffer.class)
public class BuiltBufferMixin implements BuiltBufferInterface {
}
