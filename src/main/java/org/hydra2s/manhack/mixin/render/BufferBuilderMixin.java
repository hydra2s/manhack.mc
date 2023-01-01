package org.hydra2s.manhack.mixin.render;

//
import net.minecraft.client.render.BufferBuilder;
import org.hydra2s.manhack.ducks.render.BufferBuilderInterface;
import org.spongepowered.asm.mixin.Mixin;

// TODO: use own GL/VK buffer instead of uploading
@Mixin(BufferBuilder.class)
public class BufferBuilderMixin implements BufferBuilderInterface {

};
