package com.example.mc;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Main {
    private long window;
    private final int width = 1280;
    private final int height = 720;

    private World world;
    private Player player;
    private Shader shader;
    private Hud hud;

    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;
    private boolean paused = false;
    private boolean showDebug = true;
    private boolean f3Pressed = false;
    private boolean fPressed = false;

    private final Block[] hotbar = {
            Block.GRASS, Block.DIRT, Block.STONE, Block.WOOD, Block.LEAVES,
            Block.SAND, Block.WATER, Block.BRICK, Block.GLASS
    };
    private int selectedSlot = 0;

    private Raycast.Hit currentHit = new Raycast.Hit();

    private double lastFpsTime = 0;
    private int frames = 0;
    private int fps = 0;

    private int lineVao, lineVbo;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Minecraft Proto", 0, 0);
        if (window == 0) throw new RuntimeException("Window creation failed");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetInputMode(window, GLFW_STICKY_MOUSE_BUTTONS, GLFW_TRUE);

        org.lwjgl.opengl.GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        world = new World();
        player = new Player();
        shader = new Shader();
        hud = new Hud();

        setupLineBuffer();
        setupCallbacks();
    }

    private void setupLineBuffer() {
        lineVao = glGenVertexArrays();
        lineVbo = glGenBuffers();
        glBindVertexArray(lineVao);
        glBindBuffer(GL_ARRAY_BUFFER, lineVbo);
        glBufferData(GL_ARRAY_BUFFER, 24 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    private void setupCallbacks() {
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (paused) return;
            if (firstMouse) {
                lastMouseX = x;
                lastMouseY = y;
                firstMouse = false;
            }
            float dx = (float) (x - lastMouseX);
            float dy = (float) (lastMouseY - y);
            lastMouseX = x;
            lastMouseY = y;
            player.rotate(dx * 0.1f, dy * 0.1f);
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (action != GLFW_PRESS) return;
            if (paused) {
                paused = false;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                firstMouse = true;
                return;
            }
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                breakBlock();
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                placeBlock();
            }
        });

        glfwSetScrollCallback(window, (win, xOff, yOff) -> {
            if (paused) return;
            selectedSlot = (selectedSlot - (int) Math.signum(yOff) + hotbar.length) % hotbar.length;
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS) return;
            if (key == GLFW_KEY_ESCAPE && paused) {
                glfwSetWindowShouldClose(win, true);
            }
        });
    }

    private void breakBlock() {
        if (!currentHit.hit) return;
        world.setBlock(currentHit.x, currentHit.y, currentHit.z, Block.AIR);
    }

    private void placeBlock() {
        if (!currentHit.hit) return;
        int px = currentHit.x + currentHit.nx;
        int py = currentHit.y + currentHit.ny;
        int pz = currentHit.z + currentHit.nz;

        float minX = player.position.x - Player.WIDTH / 2f - 0.01f;
        float maxX = player.position.x + Player.WIDTH / 2f + 0.01f;
        float minY = player.position.y - Player.HEIGHT / 2f - 0.01f;
        float maxY = player.position.y + Player.HEIGHT / 2f + 0.01f;
        float minZ = player.position.z - Player.WIDTH / 2f - 0.01f;
        float maxZ = player.position.z + Player.WIDTH / 2f + 0.01f;
        if (px + 1 > minX && px < maxX &&
            py + 1 > minY && py < maxY &&
            pz + 1 > minZ && pz < maxZ) return;

        world.setBlock(px, py, pz, hotbar[selectedSlot]);
    }

    private void loop() {
        double last = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;
            if (dt > 0.1f) dt = 0.1f;

            handleInput(dt);
            updateRaycast();

            frames++;
            if (now - lastFpsTime >= 1.0) {
                fps = frames;
                frames = 0;
                lastFpsTime = now;
            }

            updateWindowTitle();

            glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (!paused) {
                shader.bind();
                Matrix4f projection = new Matrix4f().perspective(
                        (float) Math.toRadians(70),
                        (float) width / height,
                        0.1f, 1000f);
                shader.setUniform("uProjection", projection);
                shader.setUniform("uView", player.getViewMatrix());
                world.render();
                shader.unbind();

                renderBlockOutline();
            }

            // 2D HUD
            hud.begin(width, height);
            if (!paused) {
                hud.drawCrosshair(width, height);
                hud.drawHotbar(width, height, hotbar, selectedSlot);

                if (showDebug) {
                    hud.drawDebug(width, height, new String[]{
                            "Minecraft Proto (prototype)",
                            String.format("XYZ: %.2f / %.2f / %.2f",
                                    player.position.x, player.position.y, player.position.z),
                            String.format("Chunk: %d %d",
                                    (int) Math.floor(player.position.x / 16),
                                    (int) Math.floor(player.position.z / 16)),
                            String.format("FPS: %d", fps),
                            "Блок: " + hotbar[selectedSlot].name(),
                            "Полёт: " + (player.flyMode ? "ВКЛ" : "выкл") + " (F)",
                            "ЛКМ — копать, ПКМ — ставить",
                            "1-9 слот, колесо листать, F3 отладка, Esc пауза"
                    });
                }
            } else {
                hud.fillRect(0, 0, width, height, 0, 0, 0, 0.5f);
                hud.text("ПАУЗА", (width - 6 * 9f) / 2f, height / 2f - 20,
                        1, 1, 1, 1, width, height);
                hud.text("Клик — продолжить, Esc — выход", (width - 30 * 9f) / 2f, height / 2f + 10,
                        1, 1, 1, 1, width, height);
            }
            hud.end();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void updateRaycast() {
        if (paused) {
            currentHit = new Raycast.Hit();
            return;
        }
        Vector3f origin = player.eyePos();
        Vector3f dir = player.forward();
        currentHit = Raycast.cast(origin, dir, 6f, world);
    }

    private void renderBlockOutline() {
        if (!currentHit.hit) return;

        float e = 0.002f;
        float x0 = currentHit.x - e;
        float y0 = currentHit.y - e;
        float z0 = currentHit.z - e;
        float x1 = currentHit.x + 1 + e;
        float y1 = currentHit.y + 1 + e;
        float z1 = currentHit.z + 1 + e;

        float[] v = {
                x0, y0, z0,  x1, y0, z0,
                x1, y0, z0,  x1, y0, z1,
                x1, y0, z1,  x0, y0, z1,
                x0, y0, z1,  x0, y0, z0,
                x0, y1, z0,  x1, y1, z0,
                x1, y1, z0,  x1, y1, z1,
                x1, y1, z1,  x0, y1, z1,
                x0, y1, z1,  x0, y1, z0,
                x0, y0, z0,  x0, y1, z0,
                x1, y0, z0,  x1, y1, z0,
                x1, y0, z1,  x1, y1, z1,
                x0, y0, z1,  x0, y1, z1
        };

        glBindVertexArray(lineVao);
        glBindBuffer(GL_ARRAY_BUFFER, lineVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, v);

        shader.bind();
        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(70), (float) width / height, 0.1f, 1000f);
        shader.setUniform("uProjection", projection);
        shader.setUniform("uView", player.getViewMatrix());

        glLineWidth(2f);
        glDrawArrays(GL_LINES, 0, v.length / 3);
        glLineWidth(1f);
        glBindVertexArray(0);
        shader.unbind();
    }

    private void handleInput(float dt) {
        // Esc — пауза
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            if (!paused) {
                paused = true;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                firstMouse = true;
            }
        }

        // F — полёт (с debounce)
        if (glfwGetKey(window, GLFW_KEY_F) == GLFW_PRESS) {
            if (!fPressed) {
                player.flyMode = !player.flyMode;
                fPressed = true;
            }
        } else {
            fPressed = false;
        }

        // F3 — отладка (с debounce)
        if (glfwGetKey(window, GLFW_KEY_F3) == GLFW_PRESS) {
            if (!f3Pressed) {
                showDebug = !showDebug;
                f3Pressed = true;
            }
        } else {
            f3Pressed = false;
        }

        // хотбар 1-9
        for (int i = 0; i < 9; i++) {
            if (glfwGetKey(window, GLFW_KEY_1 + i) == GLFW_PRESS) {
                selectedSlot = i;
            }
        }

        if (paused) {
            player.update(0, world, false, false, false, false, false, false);
            return;
        }

        boolean w = glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS;
        boolean s = glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS;
        boolean a = glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS;
        boolean d = glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS;
        boolean space = glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS;
        boolean shift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS;

        player.update(dt, world, w, s, a, d, space, shift);
    }

    private void updateWindowTitle() {
        String title = String.format(
                "Minecraft Proto | XYZ: %.1f %.1f %.1f | FPS: %d | Блок: %s%s",
                player.position.x, player.position.y, player.position.z,
                fps, hotbar[selectedSlot].name(),
                paused ? " | ПАУЗА" : (player.flyMode ? " | ПОЛЁТ" : "")
        );
        glfwSetWindowTitle(window, title);
    }

    private void cleanup() {
        world.dispose();
        shader.dispose();
        hud.dispose();
        glDeleteBuffers(lineVbo);
        glDeleteVertexArrays(lineVao);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}