package com.example.mc;

import org.joml.Vector3f;

public class Raycast {
    public static class Hit {
        public int x, y, z;
        public int nx, ny, nz;
        public Block block = Block.AIR;
        public boolean hit = false;
    }

    public static Hit cast(Vector3f origin, Vector3f dir, float maxDist, World world) {
        Hit hit = new Hit();

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        int stepX = dir.x > 0 ? 1 : -1;
        int stepY = dir.y > 0 ? 1 : -1;
        int stepZ = dir.z > 0 ? 1 : -1;

        float tDeltaX = Math.abs(1f / dir.x);
        float tDeltaY = Math.abs(1f / dir.y);
        float tDeltaZ = Math.abs(1f / dir.z);

        float tMaxX = ((dir.x > 0 ? (x + 1 - origin.x) : (origin.x - x))) * tDeltaX;
        float tMaxY = ((dir.y > 0 ? (y + 1 - origin.y) : (origin.y - y))) * tDeltaY;
        float tMaxZ = ((dir.z > 0 ? (z + 1 - origin.z) : (origin.z - z))) * tDeltaZ;

        int nx = 0, ny = 0, nz = 0;
        float t = 0;

        while (t <= maxDist) {
            Block b = world.getBlock(x, y, z);
            if (b != Block.AIR && b.solid) {
                hit.hit = true;
                hit.x = x; hit.y = y; hit.z = z;
                hit.nx = nx; hit.ny = ny; hit.nz = nz;
                hit.block = b;
                return hit;
            }

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX; t = tMaxX; tMaxX += tDeltaX;
                nx = -stepX; ny = 0; nz = 0;
            } else if (tMaxY < tMaxZ) {
                y += stepY; t = tMaxY; tMaxY += tDeltaY;
                nx = 0; ny = -stepY; nz = 0;
            } else {
                z += stepZ; t = tMaxZ; tMaxZ += tDeltaZ;
                nx = 0; ny = 0; nz = -stepZ;
            }
        }
        return hit;
    }
}