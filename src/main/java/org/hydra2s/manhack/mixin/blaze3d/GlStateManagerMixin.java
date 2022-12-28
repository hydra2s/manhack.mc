package org.hydra2s.manhack.mixin.blaze3d;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;

// NEW BIG TODO LIST!
// - Needs replace a buffer memory stack!
// - Needs add immutable buffer support!
// - Needs add virtual buffer support!
// - Needs add virtual allocation support!

// TODO! NEEDS TO REPLACE:
// - `_vertexAttribPointer`  // support for virtual buffer with offset
// - `_vertexAttribIPointer` // support for virtual buffer with offset
// - `_drawElements`   // support for virtual buffer with offset
// - `_glGenBuffers`   // virtual pre-allocation
// - `_glBindBuffer`   // virtual binding system
// - `_glBufferData`   // virtual allocation, support for virtual buffer with offset
// - `mapBuffer`       // support for virtual buffer with offset
// - `_glUnmapBuffer`  // support for virtual buffer with offset

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {

}
