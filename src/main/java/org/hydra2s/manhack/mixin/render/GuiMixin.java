package org.hydra2s.manhack.mixin.render;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.hydra2s.manhack.GlContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void _disableWorldRendering(MatrixStack par1, int par2, int par3, float par4, CallbackInfo ci) {
        GlContext.worldRendering = false;
    }

    // TODO: Move this to a more appropriate mixin
    @Inject(method = "render", at = @At("RETURN"))
    public void _restoreWorldRendering(MatrixStack par1, int par2, int par3, float par4, CallbackInfo ci) {

    }
}
