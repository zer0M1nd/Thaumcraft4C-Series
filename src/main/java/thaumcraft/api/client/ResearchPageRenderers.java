package thaumcraft.api.client;

import java.util.*;
import java.util.function.Function;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import tc4revival.client.ResearchReaderScreen;
import tc4revival.research.ResearchPage;

/** Client-only. Register factories in client setup. Every visible page owns a separate renderer.
 * All coordinates are GUI-scaled absolute screen coordinates; content is clipped to Bounds.
 * close() runs on navigation, screen close/resize, or after resource reload. No world mutation.
 */
public class ResearchPageRenderers {
    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double mx, double my) { return mx >= x && my >= y && mx < x + width && my < y + height; }
    }
    public record Context(ResearchReaderScreen screen, Bounds bounds) {}
    public interface Renderer extends AutoCloseable {
        void render(Context context, GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
        default boolean mouseClicked(Context context, double x, double y, int button) { return false; }
        default boolean mouseDragged(Context context, double x, double y, int button, double dx, double dy) { return false; }
        default boolean mouseReleased(Context context, double x, double y, int button) { return false; }
        default boolean mouseScrolled(Context context, double x, double y, double delta) { return false; }
        @Override default void close() {}
    }
    private static final Map<ResourceLocation, Function<JsonObject, Renderer>> FACTORIES = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile long generation;
    public static void register(ResourceLocation id, Function<JsonObject, Renderer> factory) {
        if (FACTORIES.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(factory)) != null)
            throw new IllegalArgumentException("Duplicate research page renderer " + id);
    }
    public static void reload() { generation++; }
    public static long generation() { return generation; }
    public static Renderer create(ResearchPage.Custom page) {
        var factory = FACTORIES.get(page.id());
        if (factory != null) return Objects.requireNonNull(factory.apply(page.data()));
        return (context, graphics, mx, my, partial) -> graphics.drawWordWrap(net.minecraft.client.Minecraft.getInstance().font,
                Component.literal("Missing page renderer: " + page.id()), context.bounds.x, context.bounds.y, context.bounds.width, 0x802020);
    }
}
