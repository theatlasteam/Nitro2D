package n2d;

/** Frame timing + AABB overlap. Pure integer, no LCDUI. */
public final class Anim {

    private Anim() {}

    /** Looping frame index for timeMs with frameMs per frame over n frames. */
    public static int frameAt(long timeMs, int frameMs, int n) {
        if (n <= 0 || frameMs <= 0) return 0;
        long f = timeMs / frameMs;
        return (int) (f - (f / n) * n);
    }

    /** Clamped frame index (plays once, holds last). */
    public static int frameOnce(long timeMs, int frameMs, int n) {
        if (n <= 0 || frameMs <= 0) return 0;
        long f = timeMs / frameMs;
        return f >= n ? n - 1 : (int) f;
    }
}
