package n2d;

/**
 * Rigid body: milli-pixel center, half extents in px, velocity in px/s.
 * Static bodies have infinite mass; sensors report overlap without
 * resolving. Handler callbacks carry the push-apart normal for self.
 */
public final class Body {

    public interface Handler {
        void collided(Body self, Body other, int nx, int ny);
    }

    /** Center in milli-pixels. */
    public int x;
    public int y;
    /** Velocity in px/s. */
    public int vx;
    public int vy;
    /** Half extents in px. */
    public int hw;
    public int hh;
    /** Gravity scale, 1024 = full engine gravity. */
    public int gscale = 1024;
    /** Ground friction in px/s^2, applied when supported. */
    public int friction;
    /** Restitution 0..1024 (0 = dead, 1024 = bouncy). */
    public int rest;
    public boolean statik;
    public boolean sensor;
    /** 1 if resting on something below after the last step. */
    public int supported;
    /** Approach speed px/s of the last impulse (0 if none). */
    public int impact;
    public int tag;
    public Handler handler;

    public Body(int cxMilliPx, int cyMilliPx, int halfWpx, int halfHpx) {
        x = cxMilliPx;
        y = cyMilliPx;
        hw = halfWpx;
        hh = halfHpx;
    }

    public int left() {
        return (x - hw * 1000) / 1000;
    }

    public int top() {
        return (y - hh * 1000) / 1000;
    }
}
