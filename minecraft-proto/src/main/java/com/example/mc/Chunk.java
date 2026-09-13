package com.example.mc;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 64;
    public static final int SIZE_Z = 16;

    private final int chunkX, chunkZ;
    private final byte[] blocks = new byte[SIZE_X * SIZE_Y * SIZE_Z];
    private int vao, vbo, vertexCount;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        generateTerrain();
        buildMesh();
    }

    private int idx(int x, int y, int z) {
        return (y * SIZE_Z + z) * SIZE_X + x;
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE_X || y >= SIZE_Y || z >= SIZE_Z) return 0;
        return blocks[idx(x, y, z)];
    }

    private void setBlock(int x, int y, int z, byte type) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE_X || y >= SIZE_Y || z >= SIZE_Z) return;
        blocks[idx(x, y, z)] = type;
    }

    private void generateTerrain() {
        int baseX = chunkX * SIZE_X;
        int baseZ = chunkZ * SIZE_Z;
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                double h = 16
                        + 6 * Math.sin((baseX + x) * 0.15)
                        + 6 * Math.cos((baseZ + z) * 0.15)
                        + 3 * Math.sin((baseX + x + baseZ + z) * 0.07);
                int height = (int) h;
                for (int y = 0; y <= height && y < SIZE_Y; y++) {
                    byte type = (y == height) ? (byte) 1 : (byte) 2; // 1=трава, 2=земля
                    setBlock(x, y, z, type);
                }
            }
        }
    }

    private float[] colorFor(byte type) {
        return switch (type) {
            case 1 -> new float[]{0.3f, 0.75f, 0.25f}; // трава
            case 2 -> new float[]{0.55f, 0.35f, 0.2f}; // земля
            case 3 -> new float[]{0.5f, 0.5f, 0.5f};   // камень
            default -> new float[]{1f, 0f, 1f};
        };
    }

    private void buildMesh() {
        List<Float> verts = new ArrayList<>();

        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    byte b = getBlock(x, y, z);
                    if (b == 0) continue;
                    float[] c = colorFor(b);

                    if (getBlock(x + 1, y, z) == 0) addFace(verts, x, y, z,  1, 0, 0, c);
                    if (getBlock(x - 1, y, z) == 0) addFace(verts, x, y, z, -1, 0, 0, c);
                    if (getBlock(x, y + 1, z) == 0) addFace(verts, x, y, z,  0, 1, 0, c);
                    if (getBlock(x, y - 1, z) == 0) addFace(verts, x, y, z,  0,-1, 0, c);
                    if (getBlock(x, y, z + 1) == 0) addFace(verts, x, y, z,  0, 0, 1, c);
                    if (getBlock(x, y, z - 1) == 0) addFace(verts, x, y, z,  0, 0,-1, c);
                }
            }
        }

        vertexCount = verts.size() / 6; // 3 позиция + 3 цвет
        float[] data = new float[verts.size()];
        for (int i = 0; i < data.length; i++) data[i] = verts.get(i);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buf = BufferUtils.createFloatBuffer(data.length);
        buf.put(data).flip();
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);

        int stride = 6 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    private void addFace(List<Float> v, int x, int y, int z,
                         int nx, int ny, int nz, float[] c) {
        float shade = 1.0f;
        if (ny == -1) shade = 0.5f;
        else if (nx != 0) shade = 0.75f;
        else if (nz != 0) shade = 0.85f;

        float r = c[0] * shade;
        float g = c[1] * shade;
        float b = c[2] * shade;

        float[][] corners = cornersFor(x, y, z, nx, ny, nz);
        int[] order = {0, 1, 2, 0, 2, 3};
        for (int i : order) {
            float[] p = corners[i];
            v.add(p[0]); v.add(p[1]); v.add(p[2]);
            v.add(r);    v.add(g);    v.add(b);
        }
    }

    private float[][] cornersFor(int x, int y, int z, int nx, int ny, int nz) {
        if (nx == 1) return new float[][]{
                {x + 1, y, z}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y + 1, z}};
        if (nx == -1) return new float[][]{
                {x, y, z + 1}, {x, y, z}, {x, y + 1, z}, {x, y + 1, z + 1}};
        if (ny == 1) return new float[][]{
                {x, y + 1, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y + 1, z}, {x, y + 1, z}};
        if (ny == -1) return new float[][]{
                {x, y, z}, {x + 1, y, z}, {x + 1, y, z + 1}, {x, y, z + 1}};
        if (nz == 1) return new float[][]{
                {x + 1, y, z + 1}, {x, y, z + 1}, {x, y + 1, z + 1}, {x + 1, y + 1, z + 1}};
        return new float[][]{ // nz == -1
                {x, y, z}, {x + 1, y, z}, {x + 1, y + 1, z}, {x, y + 1, z}};
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}