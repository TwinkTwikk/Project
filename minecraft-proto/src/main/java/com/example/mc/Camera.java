package com.example.mc;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Vector3f position;
    private float yaw = -90f;
    private float pitch = 0f;

    public Camera(Vector3f position) {
        this.position = position;
    }

    public void rotate(float dx, float dy) {
        yaw += dx;
        pitch += dy;
        pitch = Math.max(-89f, Math.min(89f, pitch));
    }

    private Vector3f forward() {
        float yawR = (float) Math.toRadians(yaw);
        float pitchR = (float) Math.toRadians(pitch);
        return new Vector3f(
                (float) (Math.cos(pitchR) * Math.cos(yawR)),
                (float) Math.sin(pitchR),
                (float) (Math.cos(pitchR) * Math.sin(yawR))
        ).normalize();
    }

    private Vector3f right() {
        return forward().cross(new Vector3f(0, 1, 0)).normalize();
    }

    public void moveForward(float amount) { position.fma(amount, forward()); }
    public void moveRight(float amount)   { position.fma(amount, right()); }
    public void moveUp(float amount)      { position.y += amount; }

    public Matrix4f getViewMatrix() {
        Vector3f target = new Vector3f(position).add(forward());
        return new Matrix4f().lookAt(position, target, new Vector3f(0, 1, 0));
    }
}