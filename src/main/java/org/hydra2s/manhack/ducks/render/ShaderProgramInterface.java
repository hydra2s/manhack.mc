package org.hydra2s.manhack.ducks.render;

import java.util.List;
import java.util.Map;

public interface ShaderProgramInterface {
    Map<String, Object> getSamplers();

    List<String> getSamplerNames();

    List<Integer> getLoadedSamplerIds();
}
