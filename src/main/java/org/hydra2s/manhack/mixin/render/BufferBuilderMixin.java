package org.hydra2s.manhack.mixin.render;

//
import net.minecraft.client.render.BufferBuilder;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.interfaces.BufferBuilderInterface;
import org.hydra2s.noire.objects.MemoryAllocationObj;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

// BIG TODO LIST!
// - Replace `this.buffer` by mapped memory
// - Construct with bigger memory `public BufferBuilder(int initialCapacity)`
// - Add special mapped buffer of Vulkan API

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin implements BufferBuilderInterface {

    /**
     * @author
     * @reason Unavailable for Vulkan API
     */
    //@Overwrite private void grow() {};

    /**
     * @author
     * @reason Unavailable for Vulkan API
     */
    //@Overwrite private void grow(int size) {};

};
