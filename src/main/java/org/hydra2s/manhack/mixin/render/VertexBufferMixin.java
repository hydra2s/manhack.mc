package org.hydra2s.manhack.mixin.render;

//
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.interfaces.BuiltBufferInterface;
import org.hydra2s.manhack.interfaces.ShapeIndexBufferInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//
import java.nio.ByteBuffer;

import static org.hydra2s.manhack.GlContext.worldRendering;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL30C.glBindBufferRange;

// BIG TODO LIST!
// - For Vulkan API, needs replace a `public void upload(BufferBuilder.BuiltBuffer buffer)`

@Mixin(VertexBuffer.class)
public class VertexBufferMixin {

    @Shadow private int vertexBufferId;
    @Shadow private int indexBufferId;
    @Shadow private int vertexArrayId;
    @Shadow @Nullable private VertexFormat vertexFormat;
    @Shadow @Nullable private RenderSystem.ShapeIndexBuffer sharedSequentialIndexBuffer;
    @Shadow private VertexFormat.IndexType indexType;
    @Shadow private int indexCount;
    @Shadow private VertexFormat.DrawMode drawMode;

    //
    @Redirect(method="uploadIndexBuffer", at=@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;glBufferData(ILjava/nio/ByteBuffer;I)V"))
    void onIndexBufferData(int target, ByteBuffer data, int usage) {
        if (worldRendering) {
            GlContext.glBufferData(target, data, usage, Math.max(1024 * 1024 * 3 * 16, data.capacity()));
        } else {
            RenderSystem.glBufferData(target, data, usage);
        }
    }

    //
    @Redirect(method="uploadVertexBuffer", at=@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;glBufferData(ILjava/nio/ByteBuffer;I)V"))
    void onVertexBufferData(int target, ByteBuffer data, int usage) {
        if (worldRendering) {
            GlContext.glBufferData(target, data, usage, Math.max(1024 * 1024 * 3 * 16, data.capacity()));
        } else {
            RenderSystem.glBufferData(target, data, usage);
        }
    }

}
