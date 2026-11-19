package tc4revival.research;

import java.util.ArrayList;
import java.util.List;

/** Original construct-page projection, in logical book coordinates. */
public record ResearchCompoundLayout(float x, float y, float scale, int baseX, int baseY, List<Cell> cells) {
    public record Cell(int index, int x, int y, int depth) {}
    public ResearchCompoundLayout { cells = List.copyOf(cells); }
    public static ResearchCompoundLayout of(ResearchPageRecipes.Compound structure) {
        int width = structure.width(), height = structure.height(), depth = structure.depth();
        int offsetX = 64 - (width * 16 + depth * 16) / 2;
        float reduction = height > 3 ? (height - 3) * .2f : 0;
        var cells = new ArrayList<Cell>();
        int index = 0;
        for (int layer = 0; layer < height; layer++)
            for (int z = depth - 1; z >= 0; z--)
                for (int x = width - 1; x >= 0; x--)
                    cells.add(new Cell(index++, x * 16 + z * 16, -x * 8 + z * 8 + layer * 50, 60 - layer * 10));
        return new ResearchCompoundLayout(offsetX * (1 + reduction), 108 - height * 25 * (1 - reduction),
                1 - reduction, -8 - offsetX, -119 + Math.max(3 - width, 3 - depth) * 8 + width * 4 + depth * 4 + height * 50, cells);
    }
}
