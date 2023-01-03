package org.hydra2s.manhack.mixin.render;

import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import org.hydra2s.manhack.GlContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextRenderer.class)
public class TextRendererMixin {

    @Inject(method="draw(Ljava/lang/String;FFILorg/joml/Matrix4f;ZZ)I", at=@At("HEAD"))
    void _disableTextRender(String text, float x, float y, int color, Matrix4f matrix, boolean shadow, boolean mirror, CallbackInfoReturnable<Integer> cir) {
        //GlContext.worldRendering = false;
    }

    @Inject(method="draw(Lnet/minecraft/text/OrderedText;FFILorg/joml/Matrix4f;Z)I", at=@At("HEAD"))
    void _disableTextRender2(OrderedText text, float x, float y, int color, Matrix4f matrix, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        //GlContext.worldRendering = false;
    }

    @Inject(method="drawWithOutline", at=@At("HEAD"))
    void _disableTextRender3(OrderedText text, float x, float y, int color, int outlineColor, Matrix4f matrix, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        //GlContext.worldRendering = false;
    }

    @Inject(method= "drawLayer(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;ZII)F", at=@At("HEAD"))
    void _disableTextRender4(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light, CallbackInfoReturnable<Float> cir) {
        //GlContext.worldRendering = false;
    }

    @Inject(method= "drawLayer(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;ZII)F", at=@At("HEAD"))
    void _disableTextRender5(OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumerProvider, boolean seeThrough, int underlineColor, int light, CallbackInfoReturnable<Float> cir) {
        //GlContext.worldRendering = false;
    }

    @Inject(method= "drawGlyph", at=@At("HEAD"))
    void _disableTextRender6(GlyphRenderer glyphRenderer, boolean bold, boolean italic, float weight, float x, float y, Matrix4f matrix, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int light, CallbackInfo ci) {
        //GlContext.worldRendering = false;
    }

}
