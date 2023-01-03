package org.hydra2s.manhack;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_INTERLEAVED_ATTRIBS;
import static org.lwjgl.opengl.GL30.glTransformFeedbackVaryings;

public class TFShaderProgram {

    public final int programId;

    public int vertexShaderId;

    public int fragmentShaderId;

    public TFShaderProgram() throws Exception {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new Exception("Could not create Shader");
        }
    }

    public TFShaderProgram createVertexShader(String shaderCode, String defineType) throws Exception {
        var preCode = "#version 460 compatibility\n#define " + defineType;
        shaderCode = preCode + "\n" + shaderCode;
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
        return this;
    }

    public TFShaderProgram createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
        return this;
    }

    protected int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling Shader code: " + glGetShaderInfoLog(shaderId, 1024));
        }

        glAttachShader(programId, shaderId);

        return shaderId;
    }

    public TFShaderProgram link() throws Exception {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }

        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        glTransformFeedbackVaryings(programId, new CharSequence[]{
                "position",
                "normal",
                "texCoord0",
                "vertexColor",
                "texCoord1",
                "texCoord2"
        }, GL_INTERLEAVED_ATTRIBS);

        return this;
    }

    public TFShaderProgram bind() {
        glUseProgram(programId); return this;
    }

    public TFShaderProgram unbind() {
        glUseProgram(0); return this;
    }

    public TFShaderProgram cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
        return this;
    }
}