package com.example.mc;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Player {
    public final Vector3f position = new Vector3f(8, 40, 8);
    public final Vector3f velocity = new Vector3f();

    private float yaw = -90f;
    private float pitch = 0f;

    public static final float WIDTH = 0.6f;
    public static final float HEIGHT = 1.8f;
    public static final float EYE = 1.62f;

    private static final float GRAVITY = -28f;
    private static final float JUMP = 9f;
    private static final float SPEED = 5.5f;
    private static final float FLY_SPEED = 14f;

    public boolean flyMode = false;
    public boolean onGround = false;

    public void rotate(float dx, float dy) {
        yaw += dx;
        pitch += dy;
        pitch = Math.max(-89f, Math.min(89f, pitch));
    }

    public Vector3f forward() {
        float yawR = (float) Math.toRadians(yaw);
        float pitchR = (float) Math.toRadians(pitch);
        return new Vector3f(
                (float) (Math.cos(pitchR) * Math.cos(yawR)),
                (float) Math.sin(pitchR),
                (float) (Math.cos(pitchR) * Math.sin(yawR))
        ).normalize();
    }

    public Vector3f forwardFlat() {
        float yawR = (float) Math.toRadians(yaw);
        return new Vector3f((float) Math.cos(yawR), 0, (float) Math.sin(yawR)).normalize();
    }

    public Vector3f right() {
        return forwardFlat().cross(new Vector3f(0, 1, 0)).normalize();
    }

    public Vector3f eyePos() {
        return new Vector3f(position.x, position.y - HEIGHT / 2f + EYE, position.z);
    }

    public Matrix4f getViewMatrix() {
        Vector3f eye = eyePos();
        Vector3f target = new Vector3f(eye).add(forward());
        return new Matrix4f().lookAt(eye, target, new Vector3f(0, 1, 0));
    }

    public void update(float dt, World world, boolean w, boolean s, boolean a, boolean d,
                       boolean space, boolean shift) {
        Vector3f fwd = forwardFlat();
        Vector3f rgt = right();

        float moveSpeed = flyMode ? FLY_SPEED : SPEED;
        Vector3f wish = new Vector3f();
        if (w) wish.add(fwd);
        if (s) wish.sub(fwd);
        if (d) wish.add(rgt);
        if (a) wish.sub(rgt);
        if (wish.lengthSquared() > 0) wish.normalize().mul(moveSpeed);

        if (flyMode) {
            velocity.x = wish.x;
            velocity.z = wish.z;
            velocity.y = 0;
            if (space) velocity.y = FLY_SPEED;
            if (shift) velocity.y = -FLY_SPEED;
        } else {
            velocity.x = wish.x;
            velocity.z = wish.z;
            velocity.y += GRAVITY * dt;

            if (space && onGround) {
                velocity.y = JUMP;
                onGround = false;
            }
        }

        moveAxis(velocity.x * dt, 0, 0, world);
        moveAxis(0, velocity.y * dt, 0, world);
        moveAxis(0, 0, velocity.z * dt, world);
    }

    private void moveAxis(float dx, float dy, float dz, World world) {
        if (dx != 0) position.x += dx;
        if (dy != 0) position.y += dy;
        if (dz != 0) position.z += dz;

        float minX = position.x - WIDTH / 2f;
        float maxX = position.x + WIDTH / 2f;
        float minY = position.y - HEIGHT / 2f;
        float maxY = position.y + HEIGHT / 2f;
        float minZ = position.z - WIDTH / 2f;
        float maxZ = position.z + WIDTH / 2f;

        int x0 = (int) Math.floor(minX);
        int x1 = (int) Math.floor(maxX);
        int y0 = (int) Math.floor(minY);
        int y1 = (int) Math.floor(maxY);
        int z0 = (int) Math.floor(minZ);
        int z1 = (int) Math.floor(maxZ);

        boolean collided = false;
        outer:
        for (int bx = x0; bx <= x1; bx++)
            for (int by = y0; by <= y1; by++)
                for (int bz = z0; bz <= z1; bz++) {
                    Block b = world.getBlock(bx, by, bz);
                    if (b != Block.AIR && b.solid) {
                        collided = true;
                        break outer;
                    }
                }

        if (collided) {
            if (dx != 0) { position.x -= dx; velocity.x = 0; }
            if (dy != 0) {
                if (dy < 0) onGround = true;
                position.y -= dy;
                velocity.y = 0;
            }
            if (dz != 0) { position.z -= dz; velocity.z = 0; }
        } else if (dy < 0) {
            onGround = false;
        }
    }
}