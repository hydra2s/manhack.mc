package org.hydra2s.manhack.mixin.blaze3d;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.minecraft.client.render.VertexFormat;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.interfaces.ShapeIndexBufferInterface;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

// allocate vulkan buffer instead
// TODO: vulkan memory mapping
// TODO: unbound data for built buffer memory
// TODO: unbound from OpenGL API
// TODO: unlock triangulator

@Mixin(RenderSystem.ShapeIndexBuffer.class)
public class ShapeIndexBufferMixin implements ShapeIndexBufferInterface {
    @Shadow private int vertexCountInTriangulated;
    @Shadow private VertexFormat.IndexType indexType;
    @Shadow private int vertexCountInShape;
    @Shadow private int size;

    @Unique public GlContext.ResourceCache vk;
    @Shadow private int id;
    @Unique public ByteBuffer preAllocated;

}
