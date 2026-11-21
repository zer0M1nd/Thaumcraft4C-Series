package tc4revival.compat.jei;

/** Book-sized artwork with unscaled, interactive JEI ingredient slots. */
public class JeiRecipeLayout {
    public static boolean infusion(String kind){return kind.equals("infusion")||kind.equals("infusion_enchantment");}
    public record Position(int x, int y) {}
    public static int width(String kind) { return 174; }
    public static int height(String kind) { return kind.equals("item_aspects") ? 158 : kind.equals("aspect_combine") ? 64 : kind.equals("crucible") ? 132 : 150; }
    public static boolean scrollComponents(int inputs) { return inputs > 16; }
    public static Position item(String kind, int index, int inputs) {
        if (kind.equals("arcane")) return new Position(16 + index % 3 * 32, 16 + index / 3 * 32);
        if (kind.equals("crucible")) return new Position(26, 12);
        if (!infusion(kind)) return new Position(12, 24);
        if (index == 0) return new Position(48, 44);
        if (scrollComponents(inputs)) return new Position(8, 66);
        double angle = Math.toRadians(-90 + 360.0 * (index - 1) / (inputs - 1));
        return new Position(48 + (int)Math.round(Math.cos(angle) * 40), 44 + (int)Math.round(Math.sin(angle) * 40));
    }
    public static Position output(String kind) { return new Position(144,kind.equals("crucible")?12:40); }
    public static Position book(String kind) { return new Position(144,kind.equals("crucible")?66:84); }
    public static boolean overBook(String kind,double x,double y) {
        var p=book(kind);return !kind.equals("item_aspects")&&!kind.equals("aspect_combine")&&x>=p.x()&&x<p.x()+18&&y>=p.y()&&y<p.y()+18;
    }
    public static int costY(String kind) { return kind.equals("crucible") ? 68 : kind.equals("aspect_combine") ? 24 : 110; }
    public static int costCapacity(String kind) { return kind.equals("crucible") ? 6 : kind.equals("aspect_combine") ? 4 : 16; }
    public static Position cost(String kind, int index, int count) {
        index %= costCapacity(kind);
        int columns=kind.equals("crucible") ? 3 : kind.equals("aspect_combine") ? 4 : 8;
        int start=kind.equals("crucible") ? 28 : kind.equals("aspect_combine") ? 8 : (174-Math.min(count,columns)*20)/2;
        return new Position(start+index%columns*20,costY(kind)+index/columns*20);
    }
}
