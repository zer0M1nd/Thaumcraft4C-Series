package tc4revival.research;

import java.util.ArrayList;
import java.util.List;

/** Sampled legacy research-line shape; GPU-independent so zero-length and fading rules can be checked. */
public  class ResearchBrowserLines {
    public record Point(float x, float y, float red, float green, float blue, float alpha) {}
    private ResearchBrowserLines() {}
    public static List<Point> sample(ResearchBrowser.Link link, float ticks) {
        double deltaX = (double) link.x() - link.targetX(), deltaY = (double) link.y() - link.targetY();
        int steps = (int) (Math.hypot(deltaX, deltaY) / 2);
        if (steps == 0) return List.of(); // No visible line; avoid the legacy division by zero.
        boolean horizontal = Math.abs(deltaX) > Math.abs(deltaY);
        boolean animated = link.state() != ResearchBrowser.LinkState.COMPLETE;
        float red = animated ? 0 : .1f;
        float green = animated ? link.state() == ResearchBrowser.LinkState.TARGET_COMPLETE ? 1 : 0 : .1f;
        float blue = animated ? link.state() == ResearchBrowser.LinkState.INCOMPLETE ? 1 : 0
                : link.kind() == ResearchBrowser.LinkKind.SIBLING ? .2f : .1f;
        double stepX = deltaX / steps * (horizontal ? 2 : 1), stepY = deltaY / steps * (horizontal ? 1 : 2);
        double decay = 1 - 1 / (steps * 1.5);
        var points = new ArrayList<Point>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            float phase = (float) i / steps, remaining = 1 - phase;
            float waveX = animated ? (float) Math.sin((ticks + i) / 7f) * 5 * remaining : 0;
            float waveY = animated ? (float) Math.sin((ticks + i) / 5f) * 5 * remaining : 0;
            double curve = Math.pow(decay, i);
            float x = (float) (link.x() - stepX * i * (horizontal ? curve : 1)) + waveX;
            float y = (float) (link.y() - stepY * i * (horizontal ? 1 : curve)) + waveY;
            float tint = animated ? remaining : 1;
            points.add(new Point(x, y, red * tint, green * tint, blue * tint, animated ? .6f * phase : .6f));
        }
        return List.copyOf(points);
    }
}
