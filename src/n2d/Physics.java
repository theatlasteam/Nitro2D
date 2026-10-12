package n2d;

/**
 * Tiny rigid-body world: semi-implicit Euler integration at a fixed
 * step, O(n^2) AABB pairs (plenty for <100 bodies at 50Hz), minimum-
 * translation push-apart with restitution impulse, Coulomb-ish ground
 * friction, and sensor (trigger) bodies. Integer only, zero garbage.
 *
 * Normals: (nx, ny) is the direction SELF must move to separate.
 * Landing on top of something reports ny == -1 on the lander.
 */
public final class Physics {

    /** Gravity in px/s^2. */
    public int gravity = 2600;

    private final Body[] bodies;
    private int n;

    public Physics(int capacity) {
        if (capacity < 4) capacity = 4;
        bodies = new Body[capacity];
    }

    public void add(Body b) {
        if (n < bodies.length) bodies[n++] = b;
    }

    public int count() {
        return n;
    }

    public void clear() {
        n = 0;
    }

    public void step(int dtMs) {
        // integrate dynamics
        for (int i = 0; i < n; i++) {
            Body b = bodies[i];
            if (b.statik) continue;
            b.supported = 0;
            b.impact = 0;
            b.vy += gravity * b.gscale / 1024 * dtMs / 1000;
            b.x += b.vx * dtMs;
            b.y += b.vy * dtMs;
        }
        // collide pairs
        for (int i = 0; i < n; i++) {
            Body a = bodies[i];
            for (int j = i + 1; j < n; j++) {
                Body b = bodies[j];
                collide(a, b);
            }
        }
        // ground friction on supported dynamics
        for (int i = 0; i < n; i++) {
            Body b = bodies[i];
            if (b.statik || b.supported == 0 || b.friction <= 0) continue;
            int d = b.friction * dtMs / 1000;
            if (b.vx > 0) {
                b.vx -= d;
                if (b.vx < 0) b.vx = 0;
            } else if (b.vx < 0) {
                b.vx += d;
                if (b.vx > 0) b.vx = 0;
            }
        }
    }

    private static void collide(Body a, Body b) {
        if (a.statik && b.statik) return;
        if (a.sensor && b.sensor) return;
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        int ox = (a.hw + b.hw) * 1000 - (dx < 0 ? -dx : dx);
        if (ox <= 0) return;
        int oy = (a.hh + b.hh) * 1000 - (dy < 0 ? -dy : dy);
        if (oy <= 0) return;
        // push-apart normal for a (minimum axis)
        int nax;
        int nay;
        int pen;
        if (ox < oy) {
            nax = dx > 0 ? -1 : 1;
            nay = 0;
            pen = ox;
        } else {
            nax = 0;
            nay = dy > 0 ? -1 : 1;
            pen = oy;
        }
        if (a.sensor || b.sensor) {
            if (a.handler != null) a.handler.collided(a, b, nax, nay);
            if (b.handler != null) b.handler.collided(b, a, -nax, -nay);
            return;
        }
        int ima = a.statik ? 0 : 1;
        int imb = b.statik ? 0 : 1;
        int m = ima + imb;
        // positional correction, split by inverse mass
        a.x += nax * pen * ima / m;
        a.y += nay * pen * ima / m;
        b.x -= nax * pen * imb / m;
        b.y -= nay * pen * imb / m;
        // supported flags (pushed upward = standing)
        if (nay == -1) a.supported = 1;
        if (nay == 1) b.supported = 1;
        // impulse along contact normal a->b
        int nx = -nax;
        int ny = -nay;
        int rvn = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny;
        int closing = -rvn;
        if (closing > 0) {
            int e = (a.rest + b.rest) >> 1;
            int j = (1024 + e) * closing / 1024 / m;
            a.vx -= j * nx * ima;
            a.vy -= j * ny * ima;
            b.vx += j * nx * imb;
            b.vy += j * ny * imb;
            a.impact = closing;
            b.impact = closing;
        }
        if (a.handler != null) a.handler.collided(a, b, nax, nay);
        if (b.handler != null) b.handler.collided(b, a, -nax, -nay);
    }
}
