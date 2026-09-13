package com.example.mc;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Hud {
    private final Shader shader2d;
    private final Font font;
    private int vao, vbo;

    public Hud() {
        shader2d = new Shader(2);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 4096 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 6 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);

        // шрифт (может упасть, если файла нет — обработаем)
        Font f;
        try {
            f = new Font("C:\\Windows\\Fonts\\consola.ttf", 18f, new Shader(3));
        } catch (Exception e) {
            System.err.println("Шрифт не загружен: " + e.getMessage());
            f = null;
        }
        font = f;
    }

    public void begin(int screenW, int screenH) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        shader2d.bind();
        shader2d.setUniform2f("uScreen", screenW, screenH);
    }

    public void end() {
        shader2d.unbind();
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    public void fillRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        float[] data = {
                x, y,       r, g, b, a,
                x + w, y,   r, g, b, a,
                x + w, y + h, r, g, b, a,
                x, y,       r, g, b, a,
                x + w, y + h, r, g, b, a,
                x, y + h,   r, g, b, a
        };
        shader2d.bind();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, data);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    public void text(String s, float x, float y, float r, float g, float b, float a,
                     int screenW, int screenH) {
        if (font == null) return;
        font.draw(s, x, y, r, g, b, a, screenW, screenH);
    }

    public void drawCrosshair(int w, int h) {
        float cx = w / 2f;
        float cy = h / 2f;
        float len = 12;
        float thick = 2;
        fillRect(cx - len, cy - thick / 2, len * 2, thick, 1, 1, 1, 0.85f);
        fillRect(cx - thick / 2, cy - len, thick, len * 2, 1, 1, 1, 0.85f);
    }

    public void drawHotbar(int w, int h, Block[] slots, int selected) {
        int n = slots.length;
        float slotSize = 48;
        float pad = 4;
        float totalW = n * slotSize + (n - 1) * pad;
        float startX = (w - totalW) / 2f;
        float y = h - slotSize - 16;

        fillRect(startX - 6, y - 6, totalW + 12, slotSize + 12, 0.1f, 0.1f, 0.1f, 0.6f);

        for (int i = 0; i < n; i++) {
            float x = startX + i * (slotSize + pad);
            if (i == selected) {
                fillRect(x - 3, y - 3, slotSize + 6, slotSize + 6, 1, 1, 1, 1);
            }
            float[] c = slots[i].color;
            fillRect(x, y, slotSize, slotSize, c[0], c[1], c[2], 1);
        }

        // имя выбранного блока над хотбаром
        if (font != null) {
            String name = slots[selected].name();
            float tw = name.length() * 9f; // примерно
            text(name, (w - tw) / 2f, y - 32, 1, 1, 1, 1, w, h);
        }
    }

    public void drawDebug(int w, int h, String[] lines) {
        if (font == null) return;
        float y = 6;
        for (String line : lines) {
            // тень для читаемости
            text(line, 7, y + 1, 0, 0, 0, 1, w, h);
            text(line, 6, y, 1, 1, 1, 1, w, h);
            y += 20;
        }
    }

    public void dispose() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        shader2d.dispose();
        if (font != null) font.dispose();
    }
}