package dev.larp.client;

public final class Anim {
    private static long last = System.nanoTime();
    public static float dt = 0.016f;

    private Anim() {}

    /** Call once per frame from any renderer; repeated calls inside the same frame are ignored. */
    public static void frame() {
        long now = System.nanoTime();
        float d = (now - last) / 1e9f;
        if (d > 0.001f) {
            dt = Math.min(d, 0.1f);
            last = now;
        }
    }

    public static float approach(float cur, float target, float speed, float dt) {
        return cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
    }
}
