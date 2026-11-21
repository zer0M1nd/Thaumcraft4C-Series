package dev.tc4.download.ui;

import com.google.gson.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;

import java.util.*;
import java.util.function.*;

/** Shared native startup screen. The transport receives 'shown' only after a visible frame. */
public final class DownloadScreen extends Screen {
    public interface Controller {
        String snapshot();

        void action(String action, String id);

        boolean proceed();
    }

    private final Controller controller;
    private JsonObject view;
    private boolean painted, started, leaving;
    private String layout = "";
    private int first, ticks;
    private static final int TOP = 32, ROW = 64;

    public DownloadScreen(Controller controller) {
        super(Component.literal("Thaumcraft downloads"));
        this.controller = controller;
        this.view = JsonParser.parseString(controller.snapshot()).getAsJsonObject();
    }

    private String t(String zh, String en) {
        return minecraft.getLanguageManager().getSelected().startsWith("zh") ? zh : en;
    }

    private JsonArray rows() {
        return view.getAsJsonArray("rows");
    }

    private static String str(JsonObject obj, String key) {
        return obj.get(key).getAsString();
    }

    private static JsonObject item(JsonObject row) {
        return row.getAsJsonObject("item");
    }

    private int bottom() {
        return height - 57;
    }

    private int visible() {
        return Math.max(1, (bottom() - TOP) / ROW);
    }

    private Button button(Component label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(
                Button.builder(label, b -> action.run()).bounds(x, y, w, 18).build());
    }

    private void command(String action, String id) {
        if (leaving) return;
        controller.action(action, id);
        refresh();
    }

    private void refresh() {
        view = JsonParser.parseString(controller.snapshot()).getAsJsonObject();
        StringBuilder next = new StringBuilder();
        for (var elem : rows()) next.append(str(elem.getAsJsonObject(), "state")).append(',');
        if (!layout.contentEquals(next)) {
            layout = next.toString();
            rebuildWidgets();
        }
    }

    @Override
    public void tick() {
        if (leaving || minecraft.getOverlay() != null) return;
        if (painted && !started) {
            started = true;
            controller.action("shown", "");
        }
        if (++ticks % 3 != 0) return;
        refresh();
        if (started && view.get("auto").getAsBoolean()) proceed();
    }

    private void proceed() {
        if (leaving || !view.get("ready").getAsBoolean()) return;
        if (view.get("confirm").getAsBoolean()) {
            minecraft.setScreen(
                    new ConfirmScreen(
                            yes -> {
                                minecraft.setScreen(this);
                                if (yes) finish();
                            },
                            Component.literal(t("仍有模组未下载", "Some mods were not downloaded")),
                            Component.literal(
                                    t(
                                            "整合包可能不完整。仍要跳过这些模组并继续加载吗？",
                                            "This modpack may be incomplete. Skip these mods and"
                                                + " continue loading?"))));
        } else finish();
    }

    private void finish() {
        if (!leaving) {
            leaving = controller.proceed();
            if (!leaving) refresh();
        }
    }

    private void scroll(int n) {
        first = Math.max(0, Math.min(first + n, rows().size() - visible()));
        rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (delta != 0) scroll(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static boolean skippable(JsonObject row) {
        return !str(item(row), "kind").equals("RESOURCE");
    }

    private static boolean live(String s) {
        return s.equals("ACTIVE") || s.equals("QUEUED");
    }

    private boolean manualLinks(String state) {
        return state.equals("FAILED")
                || state.equals("CANCELLED")
                || state.equals("SKIPPED")
                || state.equals("PAUSED");
    }

    private static Component link(String label, String uri) {
        return Component.literal(label)
                .withStyle(
                        style ->
                                style.withColor(ChatFormatting.AQUA)
                                        .withUnderlined(true)
                                        .withClickEvent(
                                                new ClickEvent(ClickEvent.Action.OPEN_URL, uri)));
    }

    private void linkButton(JsonObject row, int y, int room) {
        var data = item(row);
        var links = data.getAsJsonArray("links");
        if (links.isEmpty()) return;
        String url = links.get(0).getAsString();
        int more = links.size() > 1 ? 84 : 0;
        int w = Math.max(50, room - more - 2);
        var link = link(font.plainSubstrByWidth(url, w - 8), url);
        var open = button(link, 14, y, w, () -> handleComponentClicked(link.getStyle()));
        open.setTooltip(Tooltip.create(Component.literal(url + "\n" + str(data, "directory"))));
        if (more > 0)
            button(
                    Component.literal(t("更多下载地址", "More links")),
                    16 + w,
                    y,
                    more,
                    () -> minecraft.setScreen(new Links(this, data)));
    }

    @Override
    protected void init() {
        first = Math.max(0, Math.min(first, rows().size() - visible()));
        int right = Math.min(92, width / 4), left = width - 10 - right, listWidth = left - 18;
        for (int i = first; i < Math.min(rows().size(), first + visible()); i++) {
            var row = rows().get(i).getAsJsonObject();
            var data = item(row);
            String s = str(row, "state"), id = str(data, "id");
            int y = TOP + (i - first) * ROW;
            if (live(s))
                button(
                        Component.literal(t("暂停", "Pause")),
                        left,
                        y + 3,
                        right,
                        () -> command("pause", id));
            else if (s.equals("PAUSED"))
                button(
                        Component.literal(t("继续下载", "Resume")),
                        left,
                        y + 3,
                        right,
                        () -> command("resume", id));
            else if (s.equals("FAILED") || s.equals("CANCELLED") || s.equals("SKIPPED"))
                button(
                        Component.literal(t("重试", "Retry")),
                        left,
                        y + 3,
                        right,
                        () -> command("retry", id));
            if (skippable(row) && !List.of("DONE", "PREPARING", "SKIPPED").contains(s))
                button(
                        Component.literal(t("跳过一次", "Skip once")),
                        left,
                        y + 25,
                        right,
                        () -> command("skip", id));
            if (!skippable(row) && (live(s) || s.equals("PAUSED")))
                button(
                        Component.literal(t("取消下载", "Cancel")),
                        left,
                        y + 25,
                        right,
                        () -> command("cancel", id));
            if (manualLinks(s)) linkButton(row, y + 30, listWidth);
            else if (data.has("changelog")) {
                var changelog = link(t("更新日志", "Changelog"), str(data, "changelog"));
                button(
                        changelog,
                        14,
                        y + 30,
                        76,
                        () -> handleComponentClicked(changelog.getStyle()));
            }
        }
        boolean pause = false, resume = false, retry = false, skip = false, cancel = false;
        for (var element : rows()) {
            var row = element.getAsJsonObject();
            String s = str(row, "state");
            cancel |= !skippable(row) && (live(s) || s.equals("PAUSED"));
            pause |= live(s);
            resume |= s.equals("PAUSED");
            retry |= s.equals("FAILED") || s.equals("CANCELLED") || s.equals("SKIPPED");
            skip |= skippable(row) && !List.of("DONE", "PREPARING", "SKIPPED").contains(s);
        }
        var labels = new LinkedHashMap<String, String>();
        if (pause) labels.put("pause", t("全部暂停", "Pause all"));
        if (resume) labels.put("resume", t("全部继续", "Resume all"));
        if (retry) labels.put("retry", t("全部重试", "Retry all"));
        if (cancel) labels.put("cancel", t("全部取消", "Cancel all"));
        if (skip) labels.put("skip", t("全部跳过一次", "Skip once (all)"));
        if (!labels.isEmpty()) {
            int w = Math.min(104, (width - 24) / labels.size()),
                    x = (width - w * labels.size()) / 2;
            for (var entry : labels.entrySet()) {
                button(
                        Component.literal(entry.getValue()),
                        x,
                        height - 45,
                        w - 2,
                        () -> command(entry.getKey(), ""));
                x += w;
            }
        }
        button(
                                Component.literal(t("继续加载游戏", "Continue loading")),
                                width / 2 - 66,
                                height - 23,
                                132,
                                this::proceed)
                        .active =
                view.get("ready").getAsBoolean();
        if (rows().size() > visible()) {
            button(Component.literal("↑"), width - 53, 8, 18, () -> scroll(-1)).active = first > 0;
            button(Component.literal("↓"), width - 31, 8, 18, () -> scroll(1)).active =
                    first + visible() < rows().size();
        }
    }

    private String stateText(JsonObject row) {
        String s = str(row, "state");
        return switch (s) {
            case "ACTIVE" ->
                    t("下载中", "Downloading")
                            + " · "
                            + (row.get("source").getAsInt() + 1)
                            + "/"
                            + item(row).getAsJsonArray("links").size();
            case "QUEUED" -> t("等待下载", "Queued");
            case "PREPARING" -> t("正在应用", "Applying");
            case "CANCELLED" -> t("已取消", "Cancelled");
            case "FAILED" -> t("失败", "Failed");
            case "SKIPPED" -> t("已跳过", "Skipped");
            case "PAUSED" -> t("已暂停", "Paused");
            default -> t("已完成", "Complete");
        };
    }

    private static String amount(long n) {
        return n < 1024 * 1024
                ? n / 1024 + " KiB"
                : String.format(Locale.ROOT, "%.1f MiB", n / 1048576.0);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int count = 0, skipped = 0;
        for (var element : rows()) {
            String state = str(element.getAsJsonObject(), "state");
            if (state.equals("DONE")) count++;
            if (state.equals("SKIPPED")) skipped++;
        }
        String title = t("启动下载", "Startup downloads") + "  " + count + " / " + rows().size();
        if (skipped > 0) title += "  ·  " + t("已跳过 ", "Skipped ") + skipped;
        g.drawString(font, Component.literal(title), 12, 10, 0xffffff);
        int left = width - 10 - Math.min(92, width / 4), bar = left - 23;
        for (int i = first; i < Math.min(first + visible(), rows().size()); i++) {
            var row = rows().get(i).getAsJsonObject();
            var data = item(row);
            int y = TOP + (i - first) * ROW;
            String s = str(row, "state"), message = str(row, "message");
            long received = row.get("received").getAsLong(), total = row.get("total").getAsLong();
            g.fill(9, y, width - 8, y + ROW - 2, 0xb31a2029);
            String name = str(data, "name") + "  " + str(data, "detail");
            g.drawString(font, font.plainSubstrByWidth(name, bar), 14, y + 3, 0xffffff);
            String line = stateText(row);
            if (!message.isEmpty()) line += " · " + message;
            else if (s.equals("ACTIVE") || s.equals("PAUSED"))
                line += " · " + amount(received) + (total > 0 ? " / " + amount(total) : "");
            g.drawString(
                    font,
                    font.plainSubstrByWidth(line, bar),
                    14,
                    y + 15,
                    s.equals("FAILED") ? 0xff9587 : !message.isEmpty() ? 0xffd38a : 0xb7cbd7);
            g.fill(14, y + 25, 14 + bar, y + 28, 0xff3d4652);
            int fill =
                    s.equals("DONE")
                            ? bar
                            : total > 0 ? (int) (bar * Math.min(1, received / (double) total)) : 0;
            if (fill > 0) g.fill(14, y + 25, 14 + fill, y + 28, 0xff5dcea6);
            else if (s.equals("ACTIVE") && total <= 0) {
                int offset = (int) ((System.currentTimeMillis() / 25) % Math.max(1, bar - 14));
                g.fill(14 + offset, y + 25, 28 + offset, y + 28, 0xff65b9cc);
            }
            String directory = str(data, "directory");
            if (font.width(directory) > bar)
                directory = "…" + font.plainSubstrByWidth(directory, bar - font.width("…"), true);
            g.drawString(
                    font,
                    directory,
                    14,
                    y + (manualLinks(s) || data.has("changelog") ? 51 : 34),
                    0xa5acb3);
        }
        g.drawCenteredString(
                font,
                Component.literal(
                        leaving
                                ? t("正在应用并继续加载…", "Applying changes…")
                                : t(
                                        "每 5 秒检查手动放入的文件",
                                        "Checks for manually placed files every 5 seconds")),
                width / 2,
                height - 56,
                0xb8c3cd);
        super.render(g, mx, my, partial);
        for (int i = first; i < Math.min(first + visible(), rows().size()); i++) {
            int y = TOP + (i - first) * ROW;
            var row = rows().get(i).getAsJsonObject();
            if (mx >= 14 && mx < left && my >= y && my < y + 24) {
                String message = str(row, "message");
                g.renderComponentTooltip(
                        font,
                        fontLines(
                                str(item(row), "name") + " " + str(item(row), "detail"),
                                str(item(row), "directory"),
                                message),
                        mx,
                        my);
            }
        }
        if (minecraft.getOverlay() == null) painted = true;
    }

    private static List<Component> fontLines(String... lines) {
        return Arrays.stream(lines)
                .filter(s -> !s.isEmpty())
                .map(s -> (Component) Component.literal(s))
                .toList();
    }

    private static final class Links extends Screen {
        private final DownloadScreen parent;
        private final JsonObject item;
        private int first;

        Links(DownloadScreen parent, JsonObject item) {
            super(Component.literal(parent.t("手动下载地址", "Manual download links")));
            this.parent = parent;
            this.item = item;
        }

        private int capacity() {
            return Math.max(1, (height - 83) / 22);
        }

        @Override
        protected void init() {
            var urls = item.getAsJsonArray("links");
            for (int i = first; i < Math.min(urls.size(), first + capacity()); i++) {
                String uri = urls.get(i).getAsString();
                var label = link(font.plainSubstrByWidth(uri, width - 38), uri);
                var b =
                        addRenderableWidget(
                                Button.builder(
                                                label,
                                                ignored -> handleComponentClicked(label.getStyle()))
                                        .bounds(14, 45 + (i - first) * 22, width - 28, 20)
                                        .build());
                b.setTooltip(Tooltip.create(Component.literal(uri)));
            }
            addRenderableWidget(
                    Button.builder(Component.literal(parent.t("返回", "Back")), b -> onClose())
                            .bounds(width / 2 - 45, height - 28, 90, 20)
                            .build());
            if (urls.size() > capacity()) {
                addRenderableWidget(
                                        Button.builder(Component.literal("↑"), b -> scroll(-1))
                                                .bounds(width / 2 - 68, height - 28, 20, 20)
                                                .build())
                                .active =
                        first > 0;
                addRenderableWidget(
                                        Button.builder(Component.literal("↓"), b -> scroll(1))
                                                .bounds(width / 2 + 48, height - 28, 20, 20)
                                                .build())
                                .active =
                        first + capacity() < urls.size();
            }
        }

        private void scroll(int n) {
            first =
                    Math.max(
                            0,
                            Math.min(first + n, item.getAsJsonArray("links").size() - capacity()));
            rebuildWidgets();
        }

        @Override
        public boolean mouseScrolled(double x, double y, double delta) {
            scroll(delta > 0 ? -1 : 1);
            return true;
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }

        @Override
        public void render(GuiGraphics g, int x, int y, float p) {
            renderBackground(g);
            g.drawCenteredString(font, title, width / 2, 10, 0xffffff);
            g.drawString(
                    font,
                    font.plainSubstrByWidth(str(item, "directory"), width - 28),
                    14,
                    28,
                    0xb7cbd7);
            super.render(g, x, y, p);
        }
    }
}
