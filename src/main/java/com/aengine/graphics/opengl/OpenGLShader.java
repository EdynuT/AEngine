package com.aengine.graphics.opengl;

import com.aengine.graphics.ShaderAPI;
import com.aengine.utils.FileUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class OpenGLShader implements ShaderAPI {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public OpenGLShader(String vertexPath, String fragmentPath) {
        int vert = compile(GL_VERTEX_SHADER,   FileUtils.readResource(vertexPath));
        int frag = compile(GL_FRAGMENT_SHADER, FileUtils.readResource(fragmentPath));

        programId = glCreateProgram();
        glAttachShader(programId, vert);
        glAttachShader(programId, frag);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader link error:\n" + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vert);
        glDeleteShader(frag);
    }

    private int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String label = (type == GL_VERTEX_SHADER) ? "Vertex" : "Fragment";
            throw new RuntimeException(label + " shader compile error:\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    @Override public void bind()   { glUseProgram(programId); }
    @Override public void unbind() { glUseProgram(0); }

    private int location(String name) {
        return uniformCache.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    @Override public void setInt(String name, int value)     { glUniform1i(location(name), value); }
    @Override public void setFloat(String name, float value) { glUniform1f(location(name), value); }
    @Override public void setVec2(String name, Vector2f v)   { glUniform2f(location(name), v.x, v.y); }
    @Override public void setVec3(String name, Vector3f v)   { glUniform3f(location(name), v.x, v.y, v.z); }
    @Override public void setVec4(String name, Vector4f v)   { glUniform4f(location(name), v.x, v.y, v.z, v.w); }

    @Override
    public void setMat4(String name, Matrix4f mat) {
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        mat.get(buf);
        glUniformMatrix4fv(location(name), false, buf);
    }

    @Override public void cleanup() { glDeleteProgram(programId); }
}
