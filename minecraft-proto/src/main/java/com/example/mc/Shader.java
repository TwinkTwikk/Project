package com.example.mc;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private final int program;

    private static final String VERT = """
            #version 330 core
            layout(location=0) in vec3 aPos;
            layout(location=1) in vec3 aColor;
            uniform mat4 uProjection;
            uniform mat4 uView;
            out vec3 vColor;
            void main() {
                vColor = aColor;
                gl_Position = uProjection * uView * vec4(aPos, 1.0);
            }
            """;

    private static final String FRAG = """
            #version 330 core
            in vec3 vColor;
            out vec4 FragColor;
            void main() {
                FragColor = vec4(vColor, 1.0);
            }
            """;

    public Shader() {
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, VERT);
        glCompileShader(vs);
        check(vs, "VERT");

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, FRAG);
        glCompileShader(fs);
        check(fs, "FRAG");

        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0)
            throw new RuntimeException("Link: " + glGetProgramInfoLog(program));

        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private void check(int shader, String name) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0)
            throw new RuntimeException(name + ": " + glGetShaderInfoLog(shader));
    }

    public void bind()   { glUseProgram(program); }
    public void unbind() { glUseProgram(0); }

    public void setUniform(String name, Matrix4f m) {
        int loc = glGetUniformLocation(program, name);
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        m.get(buf);
        glUniformMatrix4fv(loc, false, buf);
    }

    public void dispose() { glDeleteProgram(program); }
}