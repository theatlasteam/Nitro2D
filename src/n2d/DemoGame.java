package n2d;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * Demo runner on rigid bodies: parallax backdrop, tiled ground, animated
 * hero, coins (sensors), crates (statics), pooled particles, camera
 * follow + shake, ToneControl SFX, FPS HUD.
 * D-pad UP / FIRE / key 2 jumps (hold to bunny-hop), key 0 mutes.
 */
public final class DemoGame extends Game {

    private static final int SCREEN_W = 240;
    private static final int SURFACE_Y = 248;
    private static final int RUN_SPEED = 140;
    private static final int JUMP_V = -560;
    private static final int JUMP_MASK = GameCanvas.UP_PRESSED | GameCanvas.FIRE_PRESSED;

    private static final int TAG_HERO = 1;
    private static final int TAG_GROUND = 2;
    private static final int TAG_CRATE = 3;
    private static final int TAG_DYN = 4;
    private static final int TAG_COIN = 100;

    private Atlas atlas;
    private Backdrop back;
    private Tilemap tiles;
    private Camera cam;
    private Particles parts;
    private Font hud;
    private Physics world;
    private Body hero;

    private int invulnMs;
    private int score;
    private int coinsTaken;
    private long simMs;
    private boolean jumpQueued;
    private boolean numHeld;
    private boolean deadPending;

    private int[] heroFrames = new int[4];
    private int[] coinFrames = new int[4];
    private int dotRegion;
    private int sparkRegion;
    private int crateRegion;

    private int[] coinX;
    private int[] coinY;
    private boolean[] coinGone;
    private int[] crateX;
    private Body[] dynBodies = new Body[6];

    private String hudLeft = "";
    private String hudRight = "";
    private int lastScore = -1;
    private int lastCoins = -1;
    private int lastFps = -1;
    private int lastQ = -1;
    private boolean lastMuted;

    private boolean ready;

    private final class HeroHandler implements Body.Handler {
        public void collided(Body self, Body other, int nx, int ny) {
            if (other.tag == TAG_CRATE) {
                // Crates are platforms from above, lethal from the side.
                if (nx != 0) deadPending = true;
                return;
            }
            if (other.tag == TAG_GROUND && ny == -1 && self.impact > 650) {
                int fx = self.x / 1000;
                int fy = (self.y + self.hh * 1000) / 1000;
                for (int i = 0; i < 4; i++) {
                    parts.spawn(fx, fy - 2, -40 + i * 24, -60 - i * 10, 350, dotRegion);
                }
                Sfx.thud();
            }
        }
    }

    private final class CoinHandler implements Body.Handler {
        public void collided(Body self, Body other, int nx, int ny) {
            if (other.tag != TAG_HERO) return;
            int idx = self.tag - TAG_COIN;
            if (idx < 0 || idx >= coinGone.length || coinGone[idx]) return;
            coinGone[idx] = true;
            coinsTaken++;
            score += 10;
            parts.burst(self.x / 1000, self.y / 1000, 6, 110, 400, dotRegion,
                    simMs + idx);
            Sfx.coin();
        }
    }

    public boolean boot() {
        try {
            atlas = Atlas.load("/atlas.png", "/atlas.tbl");
            back = Backdrop.load("/bg_far.png", "/bg_near.png");
            Image tileImg = Image.createImage("/tiles.png");
            int cols = 240;
            int[] map = new int[cols * 3];
            for (int c = 0; c < cols; c++) {
                map[c] = 1;
                map[cols + c] = 1;
                map[2 * cols + c] = 1;
            }
            tiles = new Tilemap(tileImg, 24, 24, map, cols, 3);
            for (int i = 0; i < 4; i++) {
                heroFrames[i] = atlas.find("hero" + i);
                coinFrames[i] = atlas.find("coin" + i);
            }
            dotRegion = atlas.find("dot");
            sparkRegion = atlas.find("spark");
            crateRegion = atlas.find("crate0");
            hud = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            cam = new Camera(0, 0, cols * 24 - SCREEN_W, 0);

            world = new Physics(80);
            Body ground = new Body(2880 * 1000, (SURFACE_Y + 36) * 1000, 2880, 36);
            ground.statik = true;
            ground.tag = TAG_GROUND;
            world.add(ground);

            crateX = new int[]{640, 1290, 2160, 3030, 3900, 4770};
            for (int i = 0; i < crateX.length; i++) {
                Body c = new Body((crateX[i] + 12) * 1000, (SURFACE_Y - 12) * 1000, 12, 12);
                c.statik = true;
                c.tag = TAG_CRATE;
                world.add(c);
            }

            // Smashable pyramid of live dynamic boxes in the runner's path.
            int[][] pyr = {{1750, 0}, {1774, 0}, {1798, 0}, {1762, 1}, {1786, 1}, {1774, 2}};
            for (int i = 0; i < pyr.length; i++) {
                Body b = new Body(pyr[i][0] * 1000,
                        (SURFACE_Y - 12 - pyr[i][1] * 24) * 1000, 12, 12);
                b.friction = 700;
                b.rest = 50;
                b.tag = TAG_DYN;
                world.add(b);
                dynBodies[i] = b;
            }

            int arcs = 12;
            coinX = new int[arcs * 5];
            coinY = new int[arcs * 5];
            coinGone = new boolean[arcs * 5];
            CoinHandler ch = new CoinHandler();
            for (int a = 0; a < arcs; a++) {
                int baseX = 420 + a * 430;
                for (int k = 0; k < 5; k++) {
                    int lift = k == 2 ? 74 : (k == 1 || k == 3 ? 52 : 26);
                    int idx = a * 5 + k;
                    coinX[idx] = baseX + k * 30;
                    coinY[idx] = SURFACE_Y - 30 - lift;
                    Body s = new Body(coinX[idx] * 1000, coinY[idx] * 1000, 8, 8);
                    s.statik = true;
                    s.sensor = true;
                    s.tag = TAG_COIN + idx;
                    s.handler = ch;
                    world.add(s);
                }
            }

            hero = new Body(14 * 1000, (SURFACE_Y - 14) * 1000, 14, 14);
            hero.tag = TAG_HERO;
            hero.handler = new HeroHandler();
            world.add(hero);

            parts = new Particles(64);
            parts.gravity = 900;
            ready = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected void keyPressed(int keyCode) {
        if (keyCode == KEY_NUM0) {
            Sfx.enabled = !Sfx.enabled;
            if (!Sfx.enabled) Sfx.stopMusic();
            else Sfx.click();
            return;
        }
        if (keyCode == KEY_NUM2 || keyCode == KEY_NUM5 || gameAction(keyCode) == FIRE) {
            jumpQueued = true;
            numHeld = true;
        }
    }

    protected void keyReleased(int keyCode) {
        if (keyCode == KEY_NUM2 || keyCode == KEY_NUM5 || gameAction(keyCode) == FIRE) {
            numHeld = false;
        }
    }

    private int gameAction(int keyCode) {
        try {
            return getGameAction(keyCode);
        } catch (Exception e) {
            return 0;
        }
    }

    private void doJump() {
        hero.vy = JUMP_V;
        Sfx.jump();
    }

    protected void update() {
        if (!ready) return;
        simMs += SIM_DT;
        hero.vx = RUN_SPEED;
        boolean upFire = (keys() & JUMP_MASK) != 0;
        boolean grounded = hero.supported != 0;
        if (jumpQueued) {
            jumpQueued = false;
            if (grounded) doJump();
        } else if ((upFire || numHeld || hit(JUMP_MASK)) && grounded) {
            doJump();
        }
        world.step(SIM_DT);
        if (invulnMs > 0) invulnMs -= SIM_DT;
        if (deadPending) {
            deadPending = false;
            if (invulnMs <= 0) {
                parts.burst(hero.x / 1000, hero.y / 1000, 14, 160, 600,
                        sparkRegion, simMs);
                Sfx.death();
                cam.shake(14);
                hero.x -= 120 * 1000;
                if (hero.x < 14 * 1000) hero.x = 14 * 1000;
                hero.y = (SURFACE_Y - 14) * 1000;
                hero.vy = 0;
                invulnMs = 1200;
                score -= 25;
                if (score < 0) score = 0;
            }
        }
        cam.follow(hero.x / 1000 - 74, 0, 4);
        cam.tick();
        parts.update(SIM_DT);
    }

    protected void render(Graphics g) {
        if (!ready) {
            g.setColor(0x101828);
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        int cx = cam.x + cam.shakeX(simMs);
        int cy = cam.shakeY(simMs);
        back.paint(g, cx, SCREEN_W, getHeight(), quality);
        tiles.paint(g, cx, -SURFACE_Y - cy);
        int spin = Anim.frameAt(simMs, 120, 4);
        for (int i = 0; i < coinX.length; i++) {
            if (coinGone[i]) continue;
            int sx = coinX[i] - cx;
            if (sx < -24 || sx > SCREEN_W + 24) continue;
            atlas.draw(g, coinFrames[(spin + i) % 4], sx, coinY[i] + cy);
        }
        for (int i = 0; i < crateX.length; i++) {
            int sx = crateX[i] - cx;
            if (sx < -32 || sx > SCREEN_W + 32) continue;
            atlas.draw(g, crateRegion, sx + 12, SURFACE_Y + cy);
        }
        for (int i = 0; i < dynBodies.length; i++) {
            Body b = dynBodies[i];
            if (b == null) continue;
            int sx = b.x / 1000 - cx;
            if (sx < -32 || sx > SCREEN_W + 32) continue;
            atlas.draw(g, crateRegion, sx, (b.y + 12000) / 1000 + cy);
        }
        if (invulnMs <= 0 || (simMs / 90) % 2 == 0) {
            int f = hero.supported != 0 ? Anim.frameAt(simMs, 110, 4) : 1;
            atlas.draw(g, heroFrames[f], hero.x / 1000 - cx,
                    (hero.y + hero.hh * 1000) / 1000 + cy);
        }
        int cap = quality == 2 ? 64 : quality == 1 ? 32 : 12;
        parts.renderCapped(g, atlas, cx, -cy, cap);
        if (score != lastScore || coinsTaken != lastCoins) {
            lastScore = score;
            lastCoins = coinsTaken;
            hudLeft = "S:" + score + " C:" + coinsTaken;
        }
        int fps = fpsX10 / 10;
        boolean muted = !Sfx.enabled;
        if (fps != lastFps || quality != lastQ || muted != lastMuted) {
            lastFps = fps;
            lastQ = quality;
            lastMuted = muted;
            hudRight = "FPS:" + fps + " Q:" + quality + (muted ? " M" : "");
        }
        g.setFont(hud);
        g.setColor(0xffffff);
        g.drawString(hudLeft, 4, 2, Graphics.TOP | Graphics.LEFT);
        g.drawString(hudRight, SCREEN_W - 4, 2, Graphics.TOP | Graphics.RIGHT);
    }
}
