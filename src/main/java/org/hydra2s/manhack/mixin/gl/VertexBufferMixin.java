package org.hydra2s.manhack.mixin.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import org.hydra2s.manhack.GlContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

import static com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThreadOrInit;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;

@Mixin(VertexBuffer.class)
public class VertexBufferMixin {

    @Shadow private int vertexCount;
    @Shadow private int vertexBufferId;
    @Shadow private int indexBufferId;
    @Shadow private boolean hasNoIndexBuffer;
    @Shadow
    private VertexFormat vertexFormat;

    @Shadow
    public VertexFormat getVertexFormat() {
        return null;
    }

    //
    @Redirect(method = "uploadInternal(Lnet/minecraft/client/render/BufferBuilder;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;glBufferData(ILjava/nio/ByteBuffer;I)V"))
    public void glBufferData(int target, ByteBuffer data, int usage) {
        assertOnRenderThreadOrInit();
        if (GlContext.worldRendering) {
            long defaultSize = Math.max((target == GL_ARRAY_BUFFER ? 1024 * 1024 : 65536) * 4, data.limit());
            GlContext.glBufferData(defaultSize, target, data, usage);
        } else {
            RenderSystem.glBufferData(target, data, usage);
        }
    }

    // bind to draw currency
    @Inject(method="bind()V", at=@At("HEAD"))
    public void bind(CallbackInfo ci) {
        assertOnRenderThreadOrInit();
        if (GlContext.worldRendering) {
            GlContext.currentVertexFormat = this.getVertexFormat();
            GlContext.hasIndexBuffer = !hasNoIndexBuffer;
        }
    }

    //
    @Redirect(method="drawVertices()V", at=@At(value = "INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;drawElements(III)V"))
    public void drawVertices(int mode, int count, int type) {
        RenderSystem.drawElements(mode, count, type);
        if (GlContext.worldRendering && mode == GL_TRIANGLES) {
            GlContext.inclusion();
        }
    }
}
