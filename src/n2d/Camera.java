package n2d;

/** Pixel camera with smooth follow, clamp and shake. Integer only. */
public final class Camera {

    public int x;
    public int y;
    public int minX;
    public int minY;
    public int maxX;
    public int maxY;
    private int shakeT;

    public Camera(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public void snap(int tx, int ty) {
        x = Fx.clamp(tx, minX, maxX);
        y = Fx.clamp(ty, minY, maxY);
    }

    /** Ease toward target; rate 1..8 (4 = snappy, 2 = floaty). */
    public void follow(int tx, int ty, int rate) {
        tx = Fx.clamp(tx, minX, maxX);
        ty = Fx.clamp(ty, minY, maxY);
        int dx = tx - x;
        int dy = ty - y;
        x += dx / rate + (dx != 0 && dx / rate == 0 ? (dx > 0 ? 1 : -1) : 0);
        y += dy / rate + (dy != 0 && dy / rate == 0 ? (dy > 0 ? 1 : -1) : 0);
    }

    public void shake(int frames) {
        shakeT = frames;
    }

    public void tick() {
        if (shakeT > 0) shakeT--;
    }

    /** Screen-space jitter while shaking (deterministic, no Random alloc). */
    public int shakeX(long timeMs) {
        if (shakeT <= 0) return 0;
        return ((int) (timeMs / 50) % 2 == 0 ? 2 : -2) * (shakeT > 8 ? 2 : 1);
    }

    public int shakeY(long timeMs) {
        if (shakeT <= 0) return 0;
        return ((int) (timeMs / 50) % 3 == 0 ? 2 : -1);
    }
}
