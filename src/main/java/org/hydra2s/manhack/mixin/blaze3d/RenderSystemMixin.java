package org.hydra2s.manhack.mixin.blaze3d;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.NativeImage;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.noire.descriptors.RendererCInfo;
import org.hydra2s.noire.objects.MinecraftRendererObj;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Inject(method = "initRenderer(IZ)V", at=@At("TAIL"))
    private static void mInitRenderer(int debugVerbosity, boolean debugSync, CallbackInfo ci) throws IOException {
        GlContext.initialize();
    }

}
