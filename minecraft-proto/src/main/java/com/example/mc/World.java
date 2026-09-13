package com.example.mc;

import java.util.ArrayList;
import java.util.List;

public class World {
    private final List<Chunk> chunks = new ArrayList<>();

    public World() {
        // сетка 4x4 чанков = 64x64 блока
        for (int cx = 0; cx < 4; cx++) {
            for (int cz = 0; cz < 4; cz++) {
                chunks.add(new Chunk(cx, cz));
            }
        }
    }

    public void render() {
        for (Chunk c : chunks) c.render();
    }

    public void dispose() {
        for (Chunk c : chunks) c.dispose();
    }
}