package tc4revival.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.research.*;

/** Book-only extensions of vanilla's component JSON. Vanilla style/hover/click/extra/with remain intact. */
public final class BookComponents {
    public static final String LINK="thaumcraft:book/", ICON="thaumcraft:aspect_icon/";
    public static final int RESEARCH_COLOR=net.minecraft.ChatFormatting.LIGHT_PURPLE.getColor(), ICON_WIDTH=9;
    public static final ResourceLocation ICON_FONT=ResourceLocation.parse("thaumcraft:book_inline");
    private BookComponents() {}
    public static Component decode(JsonElement value) {
        return decode(value,null);
    }
    public static Component decode(JsonElement value,String identity) {
        return decode(value,identity,AspectTextColorMode.DEFAULT);
    }
    public static Component decode(JsonElement value,String identity,AspectTextColorMode defaultColorMode) {
        Objects.requireNonNull(defaultColorMode);
        String id=value!=null&&value.isJsonObject()&&value.getAsJsonObject().has("id")?value.getAsJsonObject().get("id").getAsString():identity;
        Component component=content(value,id,defaultColorMode);
        return id==null?component:Component.empty().append(BookFlow.marker(id)).append(component);
    }
    private static Component content(JsonElement value,String identity,AspectTextColorMode defaultColorMode) {
        if(value==null||value.isJsonNull())return Component.empty();
        if(value.isJsonArray()) {
            var result=Component.empty();
            int index=0;
            for(var element:value.getAsJsonArray())
                result.append(decode(element,identity==null?null:identity+"/"+index++,defaultColorMode));
            return result;
        }
        if(value.isJsonPrimitive())return Component.literal(value.getAsString());
        var json=value.getAsJsonObject().deepCopy();
        json.remove("id");
        if(json.has("amendment_group"))return BookAmendments.groupSummary(json);
        if (json.has("hoverEvent")) {
            var hover = json.getAsJsonObject("hoverEvent");
            if (hover.has("action") && hover.get("action").getAsString().equals("show_text")) {
                String field = hover.has("contents") ? "contents" : "value";
                if (hover.has(field)) hover.add(field, Component.Serializer.toJsonTree(decode(hover.get(field))));
            }
        }
        MutableComponent result;
        if(json.has("amendment_summary")) {
            json.remove("amendment_summary");
            var summary=json.remove("summary");
            var body=decode(json.remove("extra"),null,defaultColorMode);
            var header=json.remove("hoverEvent").getAsJsonObject();
            var heading=Component.Serializer.fromJson(header.get("contents"));
            var label=summary==null?null:decode(summary,null,defaultColorMode);
            result=BookAmendments.summary(body,heading,label);
        } else if (json.has("localized")) {
            var languages = json.remove("localized").getAsJsonObject();
            String locale = BookTypography.locale();
            JsonElement selected = languages.has(locale) ? languages.get(locale) : languages.get("en_us");
            result = decode(selected, identity == null ? null : identity + "/localized", defaultColorMode).copy();
        } else if(json.has("aspect")) {
            String tag=json.remove("aspect").getAsString();
            var aspect=tc4revival.aspect.TCAspects.catalog().require(tag);
            var colorMode=json.has("color_mode")
                    ? AspectTextColorMode.valueOf(json.remove("color_mode").getAsString().toUpperCase(Locale.ROOT)) : defaultColorMode;
            var appearance=thaumcraft.api.client.ClientAspectViews.view(aspect);
            var name=Component.literal(appearance.displayName())
                    .withStyle(s->s.withColor(colorMode.color(appearance.color())).withUnderlined(true));
            result=Component.empty().append(Component.literal("\uE000").withStyle(s->s.withFont(ICON_FONT)
                    .withInsertion(ICON+tag).withUnderlined(false).withBold(false).withItalic(false))).append(name);
            result.withStyle(s->s.withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE,LINK+"aspect/"+tag)));
        } else if(json.has("research")&&!json.has("item")) {
            String key=ResearchRequirements.canonical(json.remove("research").getAsString());
            String target=json.has("target")?json.remove("target").getAsString():"";
            boolean hidden=hidden(key);
            result=hidden?Component.literal("???"):Component.translatable("tc.research_name."+key);
            result.withStyle(s->s.withColor(RESEARCH_COLOR).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE,
                    LINK+"research/"+encode(key)+"#"+encode(target))));
        } else if(json.has("item")) {
            ItemStack stack=item(json.remove("item"));
            String key=json.has("research")?json.remove("research").getAsString():firstResearch(stack);
            String target=json.has("target")?json.remove("target").getAsString():"";
            result=stack.getHoverName().copy().withStyle(s->s.withUnderlined(true)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,new HoverEvent.ItemStackInfo(stack))));
            if(!key.isEmpty())result.withStyle(s->s.withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE,
                    LINK+"research/"+encode(key)+"#"+encode(target))));
        } else {
            if(json.has("extra"))json.add("extra",resolveArray(json.getAsJsonArray("extra"),identity==null?null:identity+"/extra",defaultColorMode));
            if(json.has("with"))json.add("with",resolveArray(json.getAsJsonArray("with"),identity==null?null:identity+"/with",defaultColorMode));
            var decoded=Component.Serializer.fromJson(json);
            return decoded==null?Component.empty():decoded;
        }
        var extra=json.remove("extra");
        json.addProperty("text","");
        var styled=Component.Serializer.fromJson(json);
        if(styled!=null)result.setStyle(styled.getStyle().applyTo(result.getStyle()));
        if(extra!=null) {
            int index=0;
            for(var element:extra.getAsJsonArray())
                result.append(decode(element,identity==null?null:identity+"/extra/"+index++,defaultColorMode));
        }
        return result;
    }
    private static JsonArray resolveArray(JsonArray input,String identity,AspectTextColorMode defaultColorMode) {
        var output=new JsonArray();
        int index=0;
        for(var element:input)
            output.add(Component.Serializer.toJsonTree(decode(element,identity==null?null:identity+"/"+index++,defaultColorMode)));
        return output;
    }
    public static boolean hidden(String key) {
        key=ResearchRequirements.canonical(key);
        var catalog=ResearchCatalogs.bundled();var entry=catalog.find(key);
        if(entry.isEmpty())return true;
        String original=key;
        return ClientKnowledge.displaySnapshot().map(k->!k.researchMarkers().contains(original)
                &&!new ResearchBookActions(tc4revival.aspect.TCAspects.catalog(),catalog,ResearchCatalogs::enabled).visible(k,entry.get())).orElse(true);
    }
    public static String encode(String value){return java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8);}
    public static String uncode(String value){return java.net.URLDecoder.decode(value,java.nio.charset.StandardCharsets.UTF_8);}
    public static ItemStack item(JsonElement input) {
        var json=input.isJsonObject()?input.getAsJsonObject():new JsonObject();
        String id=input.isJsonPrimitive()?input.getAsString():json.get(json.has("id")?"id":"item").getAsString();
        var location=ResourceLocation.tryParse(id);
        if(location==null||!ForgeRegistries.ITEMS.containsKey(location))return ItemStack.EMPTY;
        var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(location),json.has("count")?json.get("count").getAsInt():1);
        if(json.has("nbt"))try {stack.setTag(net.minecraft.nbt.TagParser.parseTag(json.get("nbt").getAsString()));}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalArgumentException("Invalid book item NBT",e);}
        return stack;
    }
    /** Resolve the earliest prerequisite stage with an actual recipe output, including modern recipe IDs. */
    public static String firstResearch(ItemStack stack) {
        return BookItemResearch.first(stack);
    }
}
