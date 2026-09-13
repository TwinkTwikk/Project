package com.example.mc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class World {
    private final Map<Long, Chunk> chunkMap = new HashMap<>();
    private final List<Chunk> chunks = new ArrayList<>();

    public World() {
        for (int cx = 0; cx < 4; cx++) {
            for (int cz = 0; cz < 4; cz++) {
                Chunk c = new Chunk(cx, cz);
                chunks.add(c);
                chunkMap.put(key(cx, cz), c);
            }
        }
    }

    private long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    public Chunk getChunkAt(int cx, int cz) {
        return chunkMap.get(key(cx, cz));
    }

    /** Глобальные координаты блока. */
    public Block getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.SIZE_Y) return Block.AIR;
        int cx = Math.floorDiv(x, Chunk.SIZE_X);
        int cz = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk c = getChunkAt(cx, cz);
        if (c == null) return Block.AIR;
        int lx = Math.floorMod(x, Chunk.SIZE_X);
        int lz = Math.floorMod(z, Chunk.SIZE_Z);
        return c.getBlock(lx, y, lz);
    }

    public boolean setBlock(int x, int y, int z, Block type) {
        if (y < 0 || y >= Chunk.SIZE_Y) return false;
        int cx = Math.floorDiv(x, Chunk.SIZE_X);
        int cz = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk c = getChunkAt(cx, cz);
        if (c == null) return false;
        int lx = Math.floorMod(x, Chunk.SIZE_X);
        int lz = Math.floorMod(z, Chunk.SIZE_Z);
        c.setBlock(lx, y, lz, type);
        c.markDirty();
        return true;
    }

    public void render() {
        for (Chunk c : chunks) c.render();
    }

    public void dispose() {
        for (Chunk c : chunks) c.dispose();
    }
}