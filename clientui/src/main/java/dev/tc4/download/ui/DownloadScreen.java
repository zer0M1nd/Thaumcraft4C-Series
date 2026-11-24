package dev.tc4.download.ui;

import com.google.gson.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.util.FormattedCharSequence;
import java.nio.file.Path;
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
    private static final int TOP = 34;

    private int rowHeight;

    private int measureRowHeight() {
        int lines = 1;
        for (var element : rows()) {
            var row = element.getAsJsonObject();
            if (row.get("issue").getAsBoolean())
                lines = Math.max(lines, font.split(Component.literal(warning(row)), Math.max(80, width - 36)).size());
        }
        return 55 + lines * font.lineHeight;
    }

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
        return Math.max(1, (bottom() - TOP) / rowHeight);
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
        next.append(view.get("ready"));
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
            minecraft.setScreen(new FailureConfirmation(this));
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

    @Override
    public void onFilesDrop(List<Path> paths) {
        for (Path path : paths) command("drop", path.toAbsolutePath().toString());
    }

    private String warning(JsonObject row) {
        return switch (str(item(row), "kind")) {
            case "INSTALL" -> t("下载神秘时代系列 Mod 失败。进入游戏可能缺失 Mod。", "Failed to download a Thaumcraft series mod. Mods may be missing in game.");
            case "THIRD_PARTY_INSTALL" -> t("下载第三方 Mod 失败。进入游戏可能缺失整合包所需的 Mod。", "Failed to download a third-party mod required by this pack.");
            case "UPDATE" -> t("更新神秘时代系列 Mod 失败。进入游戏可能 Mod 与版本不匹配。", "Failed to update a Thaumcraft series mod. Mod versions may be incompatible.");
            case "OPTIONAL" -> t("下载中文补充包失败。进入游戏可能缺失少量本地化资源，可直接进入游玩。", "Failed to download a Chinese supplement. A few translations may be missing. You can play now.");
            default -> t("下载旧版资源失败。进入游戏可能缺失大量材质、音效、本地化资源。", "Failed to download legacy resources. Many textures, sounds and translations may be missing.");
        };
    }

    private static int warningColor(JsonObject row) {
        return switch (str(item(row), "kind")) {
            case "INSTALL", "THIRD_PARTY_INSTALL" -> 0xff5555;
            case "OPTIONAL" -> 0xffff55;
            default -> 0xffaa33;
        };
    }

    private String dropHint() {
        return t("手动下载后，将文件拖入 Minecraft 窗口。", "After downloading manually, drag the file into the Minecraft window.");
    }

    private static boolean skippable(JsonObject row) {
        return !str(item(row), "kind").equals("RESOURCE") || str(row, "state").equals("CANCELLED");
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
        var links = data.getAsJsonArray("manualLinks");
        if (links.isEmpty()) return;
        var page = links.get(0).getAsJsonObject();
        String url = str(page, "url");
        int more = links.size() > 1 ? 84 : 0;
        int w = Math.max(50, room - more - 2);
        var link = pageLabel(page, w - 8);
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

    private Component pageLabel(JsonObject page, int width) {
        String type = str(page, "type");
        if (type.equals("官网")) type = t("官网", "Official site");
        if (type.equals("MC 百科")) type = t("MC 百科", "MCMod.cn");
        String url = str(page, "url");
        return link(font.plainSubstrByWidth("【" + type + "】" + url, width), url);
    }

    @Override
    protected void init() {
        rowHeight = measureRowHeight();
        first = Math.max(0, Math.min(first, rows().size() - visible()));
        int right = Math.min(92, width / 4), left = width - 10 - right, listWidth = left - 18;
        for (int i = first; i < Math.min(rows().size(), first + visible()); i++) {
            var row = rows().get(i).getAsJsonObject();
            var data = item(row);
            String s = str(row, "state"), id = str(data, "id");
            int y = TOP + (i - first) * rowHeight;
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
        g.drawString(font, Component.literal(title), 12, 7, 0xffffff);
        String drop = str(view, "dropMessage");
        if (!drop.isEmpty()) g.drawString(font, font.plainSubstrByWidth(drop, width - 24), 12, 21, 0xffff55);
        int left = width - 10 - Math.min(92, width / 4), bar = left - 23;
        for (int i = first; i < Math.min(first + visible(), rows().size()); i++) {
            var row = rows().get(i).getAsJsonObject();
            var data = item(row);
            int y = TOP + (i - first) * rowHeight;
            String s = str(row, "state"), message = str(row, "message");
            long received = row.get("received").getAsLong(), total = row.get("total").getAsLong();
            g.fill(9, y, width - 8, y + rowHeight - 2, 0xb31a2029);
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
                    s.equals("FAILED") ? warningColor(row) : !message.isEmpty() ? 0xffd38a : 0xb7cbd7);
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
            if (row.get("issue").getAsBoolean()) {
                int lineY = y + 51;
                for (var part : font.split(Component.literal(warning(row)), Math.max(80, width - 36))) {
                    g.drawString(font, part, 14, lineY, warningColor(row));
                    lineY += font.lineHeight;
                }
            }
        }
        g.drawCenteredString(
                font,
                Component.literal(
                        leaving
                                ? t("正在应用并继续加载…", "Applying changes…")
                                : dropHint()),
                width / 2,
                height - 56,
                0xb8c3cd);
        super.render(g, mx, my, partial);
        for (int i = first; i < Math.min(first + visible(), rows().size()); i++) {
            int y = TOP + (i - first) * rowHeight;
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

    private static final class FailureConfirmation extends Screen {
        private record Line(FormattedCharSequence text, int color) {}
        private final DownloadScreen parent;
        private final long unlock = System.nanoTime() + 5_000_000_000L;
        private final List<Line> lines = new ArrayList<>();
        private int first;
        private Button proceed;

        FailureConfirmation(DownloadScreen parent) {
            super(Component.literal(parent.t("仍有下载问题，确定继续吗？", "Continue with these download problems?")));
            this.parent = parent;
        }

        @Override
        protected void init() {
            lines.clear();
            var groups = new LinkedHashMap<String,List<String>>();
            for (var element : parent.rows()) {
                var row = element.getAsJsonObject();
                if (row.get("issue").getAsBoolean())
                    groups.computeIfAbsent(parent.warning(row), key -> new ArrayList<>()).add(str(item(row), "name"));
            }
            for (var entry : groups.entrySet()) {
                var row = parent.rows().asList().stream().map(JsonElement::getAsJsonObject)
                        .filter(r -> parent.warning(r).equals(entry.getKey())).findFirst().orElseThrow();
                for (var line : font.split(Component.literal(entry.getKey()), width - 46))
                    lines.add(new Line(line, warningColor(row)));
                for (var line : font.split(Component.literal(String.join(", ", entry.getValue())), width - 46))
                    lines.add(new Line(line, 0xc8ced6));
            }
            proceed = addRenderableWidget(Button.builder(Component.literal(parent.t("继续", "Continue")), b -> {
                if (System.nanoTime() < unlock) return;
                minecraft.setScreen(parent);
                parent.finish();
            }).bounds(width / 2 - 104, height - 30, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal(parent.t("返回", "Back")), b -> onClose())
                    .bounds(width / 2 + 4, height - 30, 100, 20).build());
            tick();
        }

        @Override
        public void tick() {
            long seconds = Math.max(0, (unlock - System.nanoTime() + 999_999_999L) / 1_000_000_000L);
            proceed.active = seconds == 0;
            proceed.setMessage(Component.literal(parent.t("继续", "Continue") + (seconds == 0 ? "" : " (" + seconds + ")")));
        }

        private int capacity() { return Math.max(1, (height - 80) / (font.lineHeight + 3)); }

        @Override
        public boolean mouseScrolled(double x, double y, double amount) {
            first = Math.max(0, Math.min(Math.max(0, lines.size() - capacity()), first + (amount > 0 ? -1 : 1)));
            return true;
        }

        @Override
        public void onClose() { minecraft.setScreen(parent); }

        @Override
        public void render(GuiGraphics g, int mx, int my, float p) {
            renderBackground(g);
            g.drawCenteredString(font, title, width / 2, 12, 0xffffff);
            for (int i = first; i < Math.min(lines.size(), first + capacity()); i++) {
                var line = lines.get(i);
                g.drawString(font, line.text(), 20, 36 + (i-first) * (font.lineHeight + 3), line.color());
            }
            super.render(g, mx, my, p);
        }
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
            var urls = item.getAsJsonArray("manualLinks");
            for (int i = first; i < Math.min(urls.size(), first + capacity()); i++) {
                var page = urls.get(i).getAsJsonObject();
                String uri = str(page, "url");
                var label = parent.pageLabel(page, width - 38);
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
                            Math.min(first + n, item.getAsJsonArray("manualLinks").size() - capacity()));
            rebuildWidgets();
        }

        @Override
        public boolean mouseScrolled(double x, double y, double delta) {
            scroll(delta > 0 ? -1 : 1);
            return true;
        }

        @Override
        public void onFilesDrop(List<Path> paths) {
            minecraft.setScreen(parent);
            parent.onFilesDrop(paths);
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
                    font.plainSubstrByWidth(parent.dropHint(), width - 28),
                    14,
                    28,
                    0xb7cbd7);
            super.render(g, x, y, p);
        }
    }
}
