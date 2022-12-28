package org.hydra2s.manhack.mixin.render;

import net.minecraft.client.render.BufferBuilder;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.interfaces.BuiltBufferInterface;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

@Mixin(BufferBuilder.BuiltBuffer.class)
public class BuiltBufferMixin implements BuiltBufferInterface {
}
