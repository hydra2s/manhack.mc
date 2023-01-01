package org.hydra2s.manhack.mixin.vertex;

//
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.ducks.vertex.VertexBufferInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//
@Mixin(VertexBuffer.class)
public class VertexBufferMixin implements VertexBufferInterface {
    @Shadow private int vertexBufferId;
    @Shadow private int indexBufferId;
    @Shadow private int vertexArrayId;
    @Shadow private VertexFormat vertexFormat;
    @Shadow private RenderSystem.ShapeIndexBuffer sharedSequentialIndexBuffer;
    @Shadow private VertexFormat.IndexType indexType;
    @Shadow private int indexCount;
    @Shadow private VertexFormat.DrawMode drawMode;

    @Inject(method="bind", at=@At(value = "RETURN"))
    void onBind(CallbackInfo ci) {
        GlContext.boundVertexBuffer = (VertexBuffer)(Object)this;
        GlContext.boundVertexFormat = vertexFormat;
    }

    @Inject(method="unbind", at=@At(value = "RETURN"))
    static void onUnbind(CallbackInfo ci) {
        GlContext.boundVertexBuffer = null;
        GlContext.boundVertexFormat = null;
    }

}