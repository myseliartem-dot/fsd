package dev.larp.client;

public final class Theme {
    public static final String[] NAMES = {"Violet", "Ocean", "Sunset", "Mint", "Rose", "Mono"};
    public static final int[][] P = {
            {0xA78BFA, 0x22D3EE}, {0x38BDF8, 0x6366F1}, {0xFB923C, 0xF43F5E},
            {0x34D399, 0x22D3EE}, {0xF472B6, 0xA78BFA}, {0xF3F4F6, 0x9CA3AF}
    };
    public static int index = 0;

    private Theme() {}

    public static int c1() { return 0xFF000000 | P[index][0]; }
    public static int c2() { return 0xFF000000 | P[index][1]; }

    /** Animated accent gradient; phase offsets the wave so neighbours get different colours. */
    public static int grad(double phase) {
        double s = System.nanoTime() / 1e9;
        float t = (float) (0.5 + 0.5 * Math.sin(s * 1.4 + phase * 6.0));
        return R.lerp(c1(), c2(), t);
    }

    public static int rainbow(double phase) {
        return R.hsb((float) ((System.nanoTime() / 1e9 * 0.15 + phase) % 1.0), 0.65f, 1f);
    }
}
