package tc4revival.content;

/** Recipe cost, signed environment contribution, and authoritative current instability. */
public record InfusionRisk(int recipe,int environment,int actual) {
    public static InfusionRisk preview(int recipe,int environment) {
        return new InfusionRisk(recipe,environment,Math.addExact(recipe,environment));
    }
    public boolean safe() { return actual<=0; }
    public int color() { return safe()?0xff55ff55:0xffff5555; }
}
