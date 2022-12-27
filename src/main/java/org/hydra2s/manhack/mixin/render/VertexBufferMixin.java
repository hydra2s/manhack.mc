package org.hydra2s.manhack.mixin.render;

//
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.hydra2s.manhack.interfaces.BuiltBufferInterface;
import org.hydra2s.manhack.interfaces.ShapeIndexBufferInterface;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//
import java.nio.ByteBuffer;

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

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void upload(BufferBuilder.BuiltBuffer buffer) {
        if (!((VertexBuffer)(Object)this).isClosed()) {
            RenderSystem.assertOnRenderThread();

            try {
                BufferBuilder.DrawParameters drawParameters = buffer.getParameters();
                BuiltBufferInterface bufferInterface = (BuiltBufferInterface)buffer;

                // TODO: getting correct mapped buffer, add binding with
                var glVkVertexBufferId = (this.vertexBufferId = bufferInterface.getGlVertexBuffer()); //buffer;

                //
                boolean bl = !drawParameters.format().equals(this.vertexFormat);
                if (bl && this.vertexFormat != null) { this.vertexFormat.clearState(); }
                if (bl || !drawParameters.indexOnly()) {
                    // TODO: support large buffer of vertex builder
                    //glBindBufferRange(34962, 0, glVkVertexBufferId, (long)org.hydra2s.utils.ReflectionUtil.getFieldValue(buffer, "batchOffset") + drawParameters.getVertexBufferStart(), drawParameters.getVertexBufferSize());
                    glBindBufferBase(34962, 0, glVkVertexBufferId);

                    // TODO: make a data for Vulkan API!
                    drawParameters.format().setupState();
                }

                //
                this.indexCount = drawParameters.indexCount();
                this.indexType = drawParameters.indexType();
                this.drawMode = drawParameters.mode();

                // TODO: support large buffer of vertex builder
                if (drawParameters.sequentialIndex()) {
                    // TODO: unbound support of index sequence
                    RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(drawParameters.mode());
                    if (shapeIndexBuffer != this.sharedSequentialIndexBuffer || !shapeIndexBuffer.isLargeEnough(drawParameters.indexCount())) {
                        shapeIndexBuffer.bindAndGrow(drawParameters.indexCount());
                        bufferInterface.fromIndexData(((ShapeIndexBufferInterface)(Object)shapeIndexBuffer).getPreAllocated());
                    }
                }

                //glBindBufferRange(34963, 0, glVkIndexBufferId, (long)org.hydra2s.utils.ReflectionUtil.getFieldValue(buffer, "batchOffset") + drawParameters.getIndexBufferStart(), (drawParameters.getIndexBufferEnd()-drawParameters.getIndexBufferStart()));
                glBindBufferBase(34963, 0, (this.indexBufferId = bufferInterface.getGlIndexBuffer()));

                //
                bufferInterface.syncData();

            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                buffer.release();
            }

        }
    }

    @Shadow
    private RenderSystem.ShapeIndexBuffer uploadIndexBuffer(BufferBuilder.DrawParameters drawParameters, ByteBuffer indexBuffer) {
        return null;
    }

    @Shadow
    private VertexFormat uploadVertexBuffer(BufferBuilder.DrawParameters drawParameters, ByteBuffer vertexBuffer) {
        return null;
    }

    // TODO: replace by mapped Vulkan Buffer of Builder
    // TODO: remove such function, or replace by synchronization
    @Inject(method = "uploadVertexBuffer", at = @At("HEAD"))
    public void onUploadVertex(BufferBuilder.DrawParameters parameters, ByteBuffer vertexBuffer, CallbackInfoReturnable<VertexFormat> cir) {

    }

    // TODO: replace by mapped Vulkan Buffer of Builder
    // TODO: remove such function, or replace by synchronization
    @Inject(method = "uploadIndexBuffer", at = @At("HEAD"))
    public void onUploadIndex(BufferBuilder.DrawParameters parameters, ByteBuffer indexBuffer, CallbackInfoReturnable<RenderSystem.ShapeIndexBuffer> cir) {

    }

    // TODO: replace such method to compatible
    @Inject(method="bind()V", at=@At("HEAD"))
    public void bind(CallbackInfo ci) {

    }
}
