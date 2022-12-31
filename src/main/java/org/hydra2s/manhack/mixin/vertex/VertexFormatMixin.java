package org.hydra2s.manhack.mixin.vertex;

//
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//
@Mixin(VertexFormat.class)
public class VertexFormatMixin {

    @Final @Shadow private ImmutableList<VertexFormatElement> elements;
    @Final @Shadow private ImmutableMap<String, VertexFormatElement> elementMap;
    @Final @Shadow private IntList offsets = new IntArrayList();
    @Final @Shadow private int vertexSizeByte;
    @Shadow private VertexBuffer buffer;

}
