package org.hydra2s.manhack.mixin.blaze3d;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.MathHelper;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.noire.descriptors.RendererCInfo;
import org.hydra2s.noire.objects.MinecraftRendererObj;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.util.vma.Vma.vmaVirtualFree;

// NEW BIG TODO LIST!
// - Needs replace a buffer memory stack!
// - Needs add immutable buffer support!
// - Needs add virtual buffer support!
// - Needs add virtual allocation support!

// TODO: unbound indexed memory
@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Inject(method = "initRenderer(IZ)V", at=@At("TAIL"))
    private static void mInitRenderer(int debugVerbosity, boolean debugSync, CallbackInfo ci) throws IOException {
        GlContext.initialize();
    }

    // TODO: deallocate texture
    @Inject(method="deleteTexture(I)V", at=@At("HEAD"))
    private static void mDeleteTexture(int texture, CallbackInfo ci) {
        GlContext.ResourceImage resource = GlContext.resourceImageMap.get(texture);
        if (resource != null) { GlContext.resourceImageMap.remove(texture); }
    };

    // TODO: deallocate buffer
    @Inject(method="glDeleteBuffers(I)V", at=@At("HEAD"))
    private static void mDeleteBuffer(int buffer, CallbackInfo ci) {
        GlContext.ResourceCache resource = GlContext.resourceCacheMap.get(buffer);
        // free virtual memory
        if (resource != null) {
            vmaVirtualFree(resource.mapped.vb.get(0), resource.allocInfo.address());
            GlContext.resourceCacheMap.remove(buffer);
        }
    };

}
