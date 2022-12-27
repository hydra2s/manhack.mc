package org.hydra2s.manhack.mixin.render;

//
import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;

// BIG TODO LIST!
// - Replace `this.buffer` by mapped memory
// - Construct with bigger memory `public BufferBuilder(int initialCapacity)`
// - Add special buffer of Vulkan API

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {
    
};
