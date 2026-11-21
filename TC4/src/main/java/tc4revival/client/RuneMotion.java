package tc4revival.client;

/** The old rune's discrete age, opacity and gravity, independent from graphics and wall-clock time. */
public  class RuneMotion {
    private final int lifetime;
    private int age;
    private double y, previousY, velocity;
    private float alpha;
    private boolean removed;
    public RuneMotion(int duration, double initialY) {
        if (duration < 0 || !Double.isFinite(initialY)) throw new IllegalArgumentException("Invalid rune lifetime or origin");
        lifetime = Math.multiplyExact(duration, 3);
        y = previousY = initialY;
    }
    public void tick() {
        if (removed) return;
        previousY = y;
        float threshold = lifetime / 5f;
        alpha = age <= threshold ? age / threshold : (float) (lifetime - age) / lifetime;
        if (age++ >= lifetime) removed = true;
        velocity -= 0.04 * (double) 0.03f;
        y += velocity;
    }
    public boolean removed() { return removed; }
    public float alpha() { return alpha; }
    public double y(float partial) { return previousY + (y - previousY) * partial; }
}
