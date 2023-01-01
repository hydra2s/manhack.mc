package org.hydra2s.manhack.mixin.blaze3d;

import com.mojang.blaze3d.systems.RenderSystem;
import org.hydra2s.manhack.GlContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

// NEW BIG TODO LIST!
// - Needs replace a buffer memory stack!
// - Needs add immutable buffer support!
// - Needs add virtual buffer support!
// - Needs add virtual allocation support!

// TODO: unbound indexed memory
@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Inject(method = "initRenderer(IZ)V", at=@At("TAIL"))
    private static void mInitRenderer(int debugVerbosity, boolean debugSync, CallbackInfo ci) throws Exception {
        GlContext.initialize();
    }

}
