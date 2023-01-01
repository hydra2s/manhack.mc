package org.hydra2s.manhack.mixin.render;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.VertexFormat;
import org.hydra2s.manhack.GlContext;
import org.hydra2s.manhack.ducks.render.ShaderProgramInterface;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ShaderProgram.class)
public class ShaderProgramMixin implements ShaderProgramInterface {

    //
    @Final @Shadow public static String SHADERS_DIRECTORY = "shaders";
    @Final @Shadow private static String CORE_DIRECTORY = "shaders/core/";
    @Final @Shadow private static String INCLUDE_DIRECTORY = "shaders/include/";
    @Final @Shadow static final Logger LOGGER = LogUtils.getLogger();
    @Final @Shadow private static Uniform DEFAULT_UNIFORM = new Uniform();
    @Final @Shadow private static boolean field_32780 = true;

    //
    @Shadow private static ShaderProgram activeProgram;
    @Shadow private static int activeProgramGlRef = -1;
    

    // will download to geometry data
    @Final @Shadow private Map<String, Object> samplers = Maps.newHashMap();
    @Final @Shadow private List<String> samplerNames = Lists.newArrayList();
    @Final @Shadow private List<Integer> loadedSamplerIds = Lists.newArrayList();

    //
    @Final @Shadow private List<GlUniform> uniforms = Lists.newArrayList();
    @Final @Shadow private List<Integer> loadedUniformIds = Lists.newArrayList();
    @Final @Shadow private Map<String, GlUniform> loadedUniforms = Maps.newHashMap();
    @Final @Shadow private int glRef;
    @Final @Shadow private String name;

    //
    @Shadow private boolean dirty;

    //
    @Final @Shadow private GlBlendState blendState;
    @Final @Shadow private List<Integer> loadedAttributeIds;
    @Final @Shadow private List<String> attributeNames;
    @Final @Shadow private ShaderStage vertexShader;
    @Final @Shadow private ShaderStage fragmentShader;
    @Final @Shadow private VertexFormat format;

    // will download to geometry data
    @Final @Shadow public GlUniform modelViewMat;
    @Final @Shadow public GlUniform projectionMat;
    @Final @Shadow public GlUniform viewRotationMat;
    @Final @Shadow public GlUniform textureMat;
    @Final @Shadow public GlUniform screenSize;
    @Final @Shadow public GlUniform colorModulator;
    @Final @Shadow public GlUniform light0Direction;
    @Final @Shadow public GlUniform light1Direction;
    @Final @Shadow public GlUniform fogStart;
    @Final @Shadow public GlUniform fogEnd;
    @Final @Shadow public GlUniform fogColor;
    @Final @Shadow public GlUniform fogShape;
    @Final @Shadow public GlUniform lineWidth;
    @Final @Shadow public GlUniform gameTime;
    @Final @Shadow public GlUniform chunkOffset;

    // download a shader data
    @Inject(method="bind", at=@At("HEAD"))
    void onBind(CallbackInfo ci) {
        GlContext.boundShaderProgram = (ShaderProgram)(Object)this;
    }

    //
    @Inject(method="unbind", at=@At("HEAD"))
    void onUnbind(CallbackInfo ci) {
        GlContext.boundShaderProgram = null;
    }

    //
    @Unique @Override public Map<String, Object> getSamplers() { return this.samplers;}
    @Unique @Override public List<String> getSamplerNames() { return this.samplerNames; }
    @Unique @Override public List<Integer> getLoadedSamplerIds() { return this.loadedSamplerIds; }

}
