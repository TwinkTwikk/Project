package com.example.mc;

public enum Block {
    AIR   (0, new float[]{0, 0, 0},             false),
    GRASS (1, new float[]{0.30f, 0.75f, 0.25f}, true),
    DIRT  (2, new float[]{0.55f, 0.35f, 0.20f}, true),
    STONE (3, new float[]{0.50f, 0.50f, 0.50f}, true),
    WOOD  (4, new float[]{0.45f, 0.28f, 0.15f}, true),
    LEAVES(5, new float[]{0.20f, 0.55f, 0.18f}, true),
    SAND  (6, new float[]{0.85f, 0.80f, 0.55f}, true),
    WATER (7, new float[]{0.20f, 0.45f, 0.85f}, false),
    BRICK (8, new float[]{0.70f, 0.30f, 0.25f}, true),
    GLASS (9, new float[]{0.75f, 0.90f, 0.95f}, true);

    public final byte id;
    public final float[] color;
    public final boolean solid;

    Block(int id, float[] color, boolean solid) {
        this.id = (byte) id;
        this.color = color;
        this.solid = solid;
    }

    private static final Block[] BY_ID = new Block[16];
    static {
        for (Block b : values()) BY_ID[b.id] = b;
    }

    public static Block byId(byte id) {
        if (id < 0 || id >= BY_ID.length || BY_ID[id] == null) return AIR;
        return BY_ID[id];
    }
}