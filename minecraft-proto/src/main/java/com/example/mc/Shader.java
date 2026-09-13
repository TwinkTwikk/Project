package com.example.mc;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private final int program;

    private static final String VERT_3D = """
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

    private static final String FRAG_3D = """
            #version 330 core
            in vec3 vColor;
            out vec4 FragColor;
            void main() {
                FragColor = vec4(vColor, 1.0);
            }
            """;

    private static final String VERT_2D = """
            #version 330 core
            layout(location=0) in vec2 aPos;
            layout(location=1) in vec4 aColor;
            uniform vec2 uScreen;
            out vec4 vColor;
            void main() {
                vColor = aColor;
                vec2 ndc = (aPos / uScreen) * 2.0 - 1.0;
                ndc.y = -ndc.y;
                gl_Position = vec4(ndc, 0.0, 1.0);
            }
            """;

    private static final String FRAG_2D = """
            #version 330 core
            in vec4 vColor;
            out vec4 FragColor;
            void main() {
                FragColor = vColor;
            }
            """;

    private static final String VERT_TEX = """
            #version 330 core
            layout(location=0) in vec2 aPos;
            layout(location=1) in vec2 aUV;
            uniform vec2 uScreen;
            out vec2 vUV;
            void main() {
                vUV = aUV;
                vec2 ndc = (aPos / uScreen) * 2.0 - 1.0;
                ndc.y = -ndc.y;
                gl_Position = vec4(ndc, 0.0, 1.0);
            }
            """;

    private static final String FRAG_TEX = """
            #version 330 core
            in vec2 vUV;
            uniform sampler2D uTex;
            uniform vec4 uColor;
            out vec4 FragColor;
            void main() {
                vec4 t = texture(uTex, vUV);
                FragColor = vec4(uColor.rgb, uColor.a * t.a);
            }
            """;

    public Shader() { this(1); }

    public Shader(int mode) {
        String vsSrc, fsSrc;
        if (mode == 2) {
            vsSrc = VERT_2D; fsSrc = FRAG_2D;
        } else if (mode == 3) {
            vsSrc = VERT_TEX; fsSrc = FRAG_TEX;
        } else {
            vsSrc = VERT_3D; fsSrc = FRAG_3D;
        }

        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vsSrc);
        glCompileShader(vs);
        check(vs, "VERT");

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fsSrc);
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

    public void setUniform2f(String name, float x, float y) {
        int loc = glGetUniformLocation(program, name);
        glUniform2f(loc, x, y);
    }

    public void setUniform4f(String name, float x, float y, float z, float w) {
        int loc = glGetUniformLocation(program, name);
        glUniform4f(loc, x, y, z, w);
    }

    public void setUniform1i(String name, int v) {
        int loc = glGetUniformLocation(program, name);
        glUniform1i(loc, v);
    }

    public void dispose() { glDeleteProgram(program); }
}