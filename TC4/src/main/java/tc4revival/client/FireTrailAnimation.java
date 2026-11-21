package tc4revival.client;

/** Pure animation rules from the non-looping TC4 FXGeneric fire trail. */
public  class FireTrailAnimation {
    private FireTrailAnimation() {}
    public static int frame(int age, int lifetime) {
        return (int)Math.min(9f * (age / (float)lifetime), 8f);
    }
    public static float alpha(int age, int lifetime) {
        return age <= 1 || age >= lifetime - 1 ? .4f : .8f;
    }
}
