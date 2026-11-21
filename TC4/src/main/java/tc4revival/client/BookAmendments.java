package tc4revival.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import tc4revival.research.*;

/** The sole client boundary for local preferences and book-only text amendments. */
public final class BookAmendments {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Set<String> warned = new HashSet<>();
    private static final java.util.regex.Pattern SUMMARY_END=java.util.regex.Pattern.compile("[\\r\\n：:。；;]|[.!?](?:\\s|$)");
    private BookAmendments() {}

    public static MutableComponent summary(Component body,Component heading,Component label) {
        String text=net.minecraft.ChatFormatting.stripFormatting(label==null?body.getString():label.getString()).strip();
        if(label==null)text=SUMMARY_END.split(text,2)[0].strip();
        text=BookTypography.text(text.replace("\uE000","").replace("\u200b","").replaceAll("\\s+"," ")).getString();
        var font=net.minecraft.client.Minecraft.getInstance().font;
        if(font.width(text)>139)text=font.plainSubstrByWidth(text,139-font.width("…")).stripTrailing()+"…";
        var details=Component.empty();
        body.visit((style,part)->{
            details.append(Component.literal(net.minecraft.ChatFormatting.stripFormatting(part)).setStyle(style.withColor(net.minecraft.ChatFormatting.WHITE)
                    .withUnderlined(false).withHoverEvent(null)));
            return Optional.empty();
        },Style.EMPTY);
        var tooltip=Component.empty().append(heading).append("\n").append(details);
        return Component.literal(text).withStyle(style->style.withUnderlined(true)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,tooltip)));
    }

    private static boolean visible(JsonObject properties) {
        return CraftingHelper.processConditions(properties, "client_conditions", ICondition.IContext.EMPTY);
    }

    /** Reader-local cache: ordinary frames evaluate only local predicates, without copying whole pages. */
    public static final class View {
        private List<ResearchPage> source = List.of(), result = List.of();
        private final List<JsonObject> localConditions = new ArrayList<>();
        private List<Boolean> state = List.of();
        public List<ResearchPage> visible(List<ResearchPage> pages) {
            boolean changed = source.size() != pages.size();
            if (!changed) for (int i = 0; i < pages.size(); i++) if (source.get(i) != pages.get(i)) { changed = true; break; }
            if (changed) {
                source = List.copyOf(pages); localConditions.clear();
                for (var page : pages) {
                    var properties = page.properties();
                    if (properties.has("client_conditions")) localConditions.add(properties);
                    if (properties.has(ResearchAmendments.REPLACEMENTS))
                        for (var rule : properties.getAsJsonArray(ResearchAmendments.REPLACEMENTS))
                            if (rule.getAsJsonObject().has("client_conditions")) localConditions.add(rule.getAsJsonObject());
                }
            }
            var current = localConditions.stream().map(BookAmendments::visible).toList();
            if (changed || !current.equals(state)) { state = current; result = BookAmendments.visible(pages); }
            return result;
        }
    }

    /** Called before the reader's layout cache, so local configuration changes invalidate the cached flow. */
    public static List<ResearchPage> visible(List<ResearchPage> source) {
        var result = new ArrayList<ResearchPage>();
        boolean precedingAmendment = false;
        for (var page : source) {
            var properties = page.properties();
            if (properties.has("client_conditions") && !visible(properties)) continue;
            boolean changed = false;
            if (properties.has("amendment_join_start") && precedingAmendment) { properties.remove("page_break"); changed = true; }
            if (properties.has(ResearchAmendments.REPLACEMENTS)) {
                var rules = new JsonArray();
                for (var value : properties.getAsJsonArray(ResearchAmendments.REPLACEMENTS))
                    if (visible(value.getAsJsonObject())) rules.add(value);
                properties.add(ResearchAmendments.REPLACEMENTS, rules);
                changed = true;
            }
            if (changed) page = new ResearchPage(page.type(), page.text(), page.research(), page.image(), page.aspects(), page.output(),
                    page.recipe(), page.id(), page.component(), properties);
            precedingAmendment = properties.has("amendment");
            result.add(page);
        }
        return group(result);
    }

    /** Filter individual preferences first, then collapse only entries from the same source. */
    private static List<ResearchPage> group(List<ResearchPage> pages) {
        var groups=new LinkedHashMap<String,List<ResearchPage>>();
        for(var page:pages) {
            var p=page.properties();
            if(p.has("amendment_provider")) {
                String key=p.get("amendment_kind").getAsString()+":"+p.get("amendment_provider").getAsString();
                groups.computeIfAbsent(key,ignored->new ArrayList<>()).add(page);
            }
        }
        var result=new ArrayList<ResearchPage>();var emitted=new HashSet<String>();
        for(var page:pages) {
            var p=page.properties();
            if(!p.has("amendment_provider")){result.add(page);continue;}
            String key=p.get("amendment_kind").getAsString()+":"+p.get("amendment_provider").getAsString();
            var members=groups.get(key);
            if(members.size()<2){result.add(page);continue;}
            if(!emitted.add(key))continue;
            var content=new JsonObject();content.addProperty("amendment_group",true);
            content.add("provider",p.get("amendment_provider_name").deepCopy());
            content.addProperty("continuation",p.get("amendment_kind").getAsString().equals("continuation"));
            var entries=new JsonArray();for(var member:members)entries.add(compact(member.component(),member.properties()));
            content.add("members",entries);p.addProperty("amendment_group",true);
            result.add(new ResearchPage(page.type(),page.text(),page.research(),page.image(),page.aspects(),page.output(),
                    page.recipe(),page.id(),content,p));
        }
        return List.copyOf(result);
    }

    public static MutableComponent groupSummary(JsonObject json) {
        var tooltip=Component.empty();int count=0;
        for(var member:json.getAsJsonArray("members")) {
            var decoded=BookComponents.decode(member);
            if(count++>0)tooltip.append("\n\n");
            decoded.visit((style,part)->{
                var hover=style.getHoverEvent();
                if(hover!=null && hover.getAction()==HoverEvent.Action.SHOW_TEXT) {
                    tooltip.append(hover.getValue(HoverEvent.Action.SHOW_TEXT));return Optional.of(true);
                }
                return Optional.empty();
            },Style.EMPTY);
        }
        boolean own=json.get("continuation").getAsBoolean(),chinese=BookTypography.locale().startsWith("zh");
        String label=own?(chinese?"承续版修改":"Continuation changes"):
                (chinese?"来自 ":"Changes from ")+BookComponents.decode(json.get("provider")).getString()+(chinese?" 的修改":"");
        label+=(chinese?"（":" (")+count+(chinese?" 项）":")");
        var font=net.minecraft.client.Minecraft.getInstance().font;
        if(font.width(label)>139)label=font.plainSubstrByWidth(label,139-font.width("…")).stripTrailing()+"…";
        return Component.literal(label).withStyle(style->style.withColor(own?net.minecraft.ChatFormatting.DARK_GREEN:net.minecraft.ChatFormatting.BLUE)
                .withUnderlined(true).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,tooltip))).append("\n");
    }

    private static JsonArray rules(ResearchPage page) {
        var properties = page.properties();
        return properties.has(ResearchAmendments.REPLACEMENTS) ? properties.getAsJsonArray(ResearchAmendments.REPLACEMENTS) : new JsonArray();
    }

    public static List<ResearchPage> expand(ResearchPage page, String research) {
        if (page.properties().has("import_legacy") && page.properties().get("import_legacy").getAsBoolean())
            return BookLegacyImport.expand(page, research);
        var rules = rules(page);
        var properties=page.properties();
        if(properties.has("amendment_group"))return List.of(page);
        boolean amendment=properties.has("amendment")&&properties.get("amendment").getAsBoolean();
        if(rules.isEmpty()&&!amendment)return List.of(page);
        var content=rules.isEmpty()?page.component():revise(page.component(),rules);
        if(amendment)content=compact(content,properties);
        return List.of(new ResearchPage(page.type(), page.text(), page.research(), page.image(), page.aspects(), page.output(),
                page.recipe(), page.id(), content, page.properties()));
    }

    /** Client-side compaction also covers snapshots from earlier servers. */
    private static JsonObject compact(JsonElement content,JsonObject properties) {
        var label=content.getAsJsonObject().deepCopy();
        var body=label.getAsJsonArray("extra");
        int last=body.size()-1;
        if(last>=0&&body.get(last).isJsonObject()) {
            var tail=body.get(last).getAsJsonObject();
            if(tail.has("text")&&tail.get("text").getAsString().equals("\n"))body.remove(last);
        }
        label.addProperty("amendment_summary",true);
        label.addProperty("underlined",true);
        if(properties.has("summary"))label.add("summary",properties.get("summary").deepCopy());
        var heading=label.getAsJsonObject("hoverEvent").getAsJsonObject("contents");
        heading.addProperty("color",label.get("color").getAsString().equals("dark_green")?"green":"blue");
        heading.addProperty("underlined",false);
        var result=new JsonObject();result.addProperty("text","");
        var extra=new JsonArray();extra.add(label);
        var suffix=new JsonObject();suffix.addProperty("text","\n");extra.add(suffix);
        result.add("extra",extra);
        return result;
    }

    /** Legacy text is corrected before semantic aliases turn its words into links. */
    public static List<ResearchPage> legacy(ResearchPage source, List<ResearchPage> parts, String research) {
        var text = new ArrayList<Component>(); var indices = new ArrayList<Integer>();
        for (int i = 0; i < parts.size(); i++) if (parts.get(i).type() == ResearchPage.Type.TEXT) {
            indices.add(i); text.add(BookComponents.decode(parts.get(i).component(),null,AspectTextColorMode.DARK));
        }
        var corrected = apply(text, rules(source), source.text().orElse(""));
        var result = new ArrayList<>(parts);
        for (int i = 0; i < indices.size(); i++) {
            int index = indices.get(i); var page = parts.get(index);
            var linked = BookLegacyImport.prose(corrected.get(i),research,AspectTextColorMode.DARK);
            result.set(index, new ResearchPage(page.type(), page.text(), page.research(), page.image(), page.aspects(), page.output(),
                    page.recipe(), page.id(), Component.Serializer.toJsonTree(linked), page.properties()));
        }
        return List.copyOf(result);
    }

    private static List<Component> apply(List<Component> text, JsonArray rules, String key) {
        return BookTextAmendments.apply(text, rules, key, BookTypography.locale(),
                value -> BookComponents.decode(value,null,AspectTextColorMode.DARK), (id, reason) -> {
            // One warning per rule/language/reason, even when a page is opened every frame.
            if (warned.add(id + ":" + BookTypography.locale() + ":" + reason)) LOGGER.warn("Book amendment {}: {}", id, reason);
        });
    }

    private static JsonElement revise(JsonElement input, JsonArray rules) {
        if (input.isJsonArray()) {
            var out = new JsonArray(); for (var child : input.getAsJsonArray()) out.add(revise(child, rules)); return out;
        }
        if (!input.isJsonObject()) return input.deepCopy();
        var json = input.getAsJsonObject().deepCopy();
        for (String field : List.of("extra", "with")) if (json.has(field)) json.add(field, revise(json.get(field), rules));
        if (json.has("localized")) for (var entry : json.getAsJsonObject("localized").entrySet()) entry.setValue(revise(entry.getValue(), rules));
        if (!json.has("translate")) return json;
        String key = json.get("translate").getAsString();
        var own = json.deepCopy(); own.remove("extra"); var identity = own.remove("id");
        Component original = BookComponents.decode(own,null,AspectTextColorMode.DARK);
        Component amended = apply(List.of(original), rules, key).get(0);
        if (amended == original) return json;
        var out = Component.Serializer.toJsonTree(amended).getAsJsonObject();
        if (identity != null) out.add("id", identity);
        if (json.has("extra")) {
            if (!out.has("extra")) out.add("extra", new JsonArray());
            out.getAsJsonArray("extra").addAll(json.getAsJsonArray("extra"));
        }
        return out;
    }
}
