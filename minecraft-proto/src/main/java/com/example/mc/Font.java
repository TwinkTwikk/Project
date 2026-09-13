package com.example.mc;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

public class Font {
    private static final int ATLAS_W = 512;
    private static final int ATLAS_H = 512;
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 96;

    private int textureId;
    private STBTTBakedChar.Buffer charData;
    private final float fontSize;
    private final int vao, vbo;
    private Shader shader;

    public Font(String ttfPath, float pixelHeight, Shader textShader) {
        this.fontSize = pixelHeight;
        this.shader = textShader;

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(ttfPath));
            ByteBuffer ttf = BufferUtils.createByteBuffer(bytes.length);
            ttf.put(bytes).flip();

            ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_W * ATLAS_H);
            charData = STBTTBakedChar.malloc(NUM_CHARS);
            STBTruetype.stbtt_BakeFontBitmap(ttf, pixelHeight, bitmap, ATLAS_W, ATLAS_H, FIRST_CHAR, charData);

            // Загружаем атлас в OpenGL (1-канальный → расширяем до RGBA)
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            ByteBuffer rgba = BufferUtils.createByteBuffer(ATLAS_W * ATLAS_H * 4);
            for (int i = 0; i < ATLAS_W * ATLAS_H; i++) {
                byte a = bitmap.get(i);
                rgba.put((byte) 255);
                rgba.put((byte) 255);
                rgba.put((byte) 255);
                rgba.put(a);
            }
            rgba.flip();
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, ATLAS_W, ATLAS_H, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glBindTexture(GL_TEXTURE_2D, 0);

        } catch (Exception e) {
            throw new RuntimeException("Не могу загрузить шрифт: " + ttfPath, e);
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    /** Рисует строку. (x, y) — левый верхний угол в пикселях экрана. */
    public void draw(String text, float x, float y, float r, float g, float b, float a,
                     int screenW, int screenH) {
        shader.bind();
        shader.setUniform2f("uScreen", screenW, screenH);
        shader.setUniform4f("uColor", r, g, b, a);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        shader.setUniform1i("uTex", 0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(x);
            FloatBuffer yb = stack.floats(y);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;
                if (c == '\n') {
                    xb.put(0, x);
                    yb.put(0, yb.get(0) + fontSize);
                    continue;
                }
                stbtt_GetBakedQuad(charData, ATLAS_W, ATLAS_H, c - FIRST_CHAR, xb, yb, q, true);

                float x0 = q.x0(), y0 = q.y0(), x1 = q.x1(), y1 = q.y1();
                float s0 = q.s0(), t0 = q.t0(), s1 = q.s1(), t1 = q.t1();

                float[] v = {
                        x0, y0, s0, t0,
                        x1, y0, s1, t0,
                        x1, y1, s1, t1,
                        x0, y0, s0, t0,
                        x1, y1, s1, t1,
                        x0, y1, s0, t1
                };
                glBufferSubData(GL_ARRAY_BUFFER, 0, v);
                glDrawArrays(GL_TRIANGLES, 0, 6);
            }
            glBindVertexArray(0);
        }

        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, 0);
        shader.unbind();
    }

    public float textWidth(String text) {
        // приблизительная ширина — можно уточнить через stbtt_GetBakedQuad
        return text.length() * fontSize * 0.5f;
    }

    public float getFontSize() { return fontSize; }

    public void dispose() {
        glDeleteTextures(textureId);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}