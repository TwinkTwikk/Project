package com.example.mc;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    private long window;
    private final int width = 1280;
    private final int height = 720;
    private Camera camera;
    private World world;
    private Shader shader;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;

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
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        camera = new Camera(new Vector3f(8, 40, 8));
        world = new World();
        shader = new Shader();

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            if (firstMouse) {
                lastMouseX = x;
                lastMouseY = y;
                firstMouse = false;
            }
            float dx = (float) (x - lastMouseX);
            float dy = (float) (lastMouseY - y);
            lastMouseX = x;
            lastMouseY = y;
            camera.rotate(dx * 0.1f, dy * 0.1f);
        });
    }

    private void loop() {
        double last = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            handleInput(dt);

            glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            shader.bind();
            Matrix4f projection = new Matrix4f().perspective(
                    (float) Math.toRadians(70),
                    (float) width / height,
                    0.1f,
                    1000f);
            shader.setUniform("uProjection", projection);
            shader.setUniform("uView", camera.getViewMatrix());
            world.render();
            shader.unbind();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void handleInput(float dt) {
        float speed = 20f * dt;
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) camera.moveForward(speed);
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) camera.moveForward(-speed);
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) camera.moveRight(-speed);
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) camera.moveRight(speed);
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) camera.moveUp(speed);
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) camera.moveUp(-speed);
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
            glfwSetWindowShouldClose(window, true);
    }

    private void cleanup() {
        shader.dispose();
        world.dispose();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}