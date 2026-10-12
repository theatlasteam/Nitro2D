package n2d;

import javax.microedition.lcdui.Graphics;

/**
 * Fixed-capacity particle pool. Preallocated int arrays, zero garbage
 * after construction; render is one native blit per live particle.
 * Positions in 16.16 fixed point, gravity in px/s^2.
 */
public final class Particles {

    private final int[] x;
    private final int[] y;
    private final int[] vx;
    private final int[] vy;
    private final int[] life;
    private final int[] ttl;
    private final int[] region;
    private final int[][] rset;
    private final int capacity;
    private int cursor;
    public int gravity;

    public Particles(int capacity) {
        this.capacity = capacity < 1 ? 1 : capacity;
        x = new int[this.capacity];
        y = new int[this.capacity];
        vx = new int[this.capacity];
        vy = new int[this.capacity];
        life = new int[this.capacity];
        ttl = new int[this.capacity];
        region = new int[this.capacity];
        rset = new int[this.capacity][];
    }

    public int alive() {
        int n = 0;
        for (int i = 0; i < capacity; i++) if (life[i] > 0) n++;
        return n;
    }

    /** Spawn one particle. px/px integer pixels, v in px/s. */
    public void spawn(int px, int py, int vxPxS, int vyPxS, int ttlMs, int regionIdx) {
        spawnInto(cursor, px, py, vxPxS, vyPxS, ttlMs);
        region[cursor] = regionIdx;
        rset[cursor] = null;
        cursor = (cursor + 1) % capacity;
    }

    /**
     * Spawn with a shrink/fade frame set (index 0 = young/big). The array
     * is referenced, not copied: keep one static set per color, no garbage.
     */
    public void spawnAnim(int px, int py, int vxPxS, int vyPxS, int ttlMs, int[] frames) {
        spawnInto(cursor, px, py, vxPxS, vyPxS, ttlMs);
        region[cursor] = frames.length > 0 ? frames[0] : 0;
        rset[cursor] = frames;
        cursor = (cursor + 1) % capacity;
    }

    private void spawnInto(int i, int px, int py, int vxPxS, int vyPxS, int ttlMs) {
        x[i] = px << 16;
        y[i] = py << 16;
        vx[i] = vxPxS << 16;
        vy[i] = vyPxS << 16;
        life[i] = ttlMs;
        ttl[i] = ttlMs <= 0 ? 1 : ttlMs;
    }

    /** Burst of n particles with fixed-point spread. gravity applies. */
    public void burst(int px, int py, int n, int speedPxS, int ttlMs, int regionIdx, long seed) {
        for (int k = 0; k < n; k++) {
            int ang = (int) ((seed * 37 + k * (360 / (n == 0 ? 1 : n))) % 360);
            int sp = (speedPxS * (60 + (int) ((seed + k * 53) % 40))) / 100;
            int vxS = Fx.toInt(Fx.mul(Fx.fromInt(sp), Fx.cos(ang)));
            int vyS = Fx.toInt(Fx.mul(Fx.fromInt(sp), Fx.sin(ang)));
            spawn(px, py, vxS, vyS, ttlMs, regionIdx);
        }
    }

    /** Same burst with a shrink/fade frame set. */
    public void burstAnim(int px, int py, int n, int speedPxS, int ttlMs, int[] frames, long seed) {
        for (int k = 0; k < n; k++) {
            int ang = (int) ((seed * 37 + k * (360 / (n == 0 ? 1 : n))) % 360);
            int sp = (speedPxS * (60 + (int) ((seed + k * 53) % 40))) / 100;
            int vxS = Fx.toInt(Fx.mul(Fx.fromInt(sp), Fx.cos(ang)));
            int vyS = Fx.toInt(Fx.mul(Fx.fromInt(sp), Fx.sin(ang)));
            spawnAnim(px, py, vxS, vyS, ttlMs, frames);
        }
    }

    public void update(int dtMs) {
        if (dtMs <= 0) return;
        for (int i = 0; i < capacity; i++) {
            if (life[i] <= 0) continue;
            life[i] -= dtMs;
            if (life[i] <= 0) {
                life[i] = 0;
                continue;
            }
            vy[i] += gravity * dtMs / 1000;
            x[i] += vx[i] / 1000 * dtMs;
            y[i] += vy[i] / 1000 * dtMs;
        }
    }

    public void render(Graphics g, Atlas atlas, int camX, int camY) {
        renderCapped(g, atlas, camX, camY, capacity);
    }

    /** Draw at most cap live particles (quality gate for slow devices). */
    public void renderCapped(Graphics g, Atlas atlas, int camX, int camY, int cap) {
        int drawn = 0;
        for (int i = 0; i < capacity && drawn < cap; i++) {
            if (life[i] <= 0) continue;
            atlas.draw(g, frameOf(i), (x[i] >> 16) - camX, (y[i] >> 16) - camY);
            drawn++;
        }
    }

    /** Current region for slot i, shrinking through its frame set with age. */
    private int frameOf(int i) {
        int[] set = rset[i];
        if (set == null || set.length < 2) return region[i];
        int st = set.length - 1 - life[i] * (set.length - 1) / ttl[i];
        if (st < 0) st = 0;
        if (st > set.length - 1) st = set.length - 1;
        return set[st];
    }
}
