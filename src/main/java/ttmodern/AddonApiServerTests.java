package ttmodern;

import com.google.gson.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.research.*;
import thaumcraft.api.research.ResearchPrerequisites;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class AddonApiServerTests {
    private static String key(String key){return Tinkerer.ID+":"+key;}
    private static JsonObject raw() throws Exception {
        var json=ResearchDefinitions.server().json().deepCopy();
        try(var reader=new java.io.InputStreamReader(AddonApiServerTests.class.getResourceAsStream("/data/thaumic_tinkerer/research_catalog/main.json"),java.nio.charset.StandardCharsets.UTF_8)){
            var original=JsonParser.parseReader(reader).getAsJsonObject();var rows=new HashMap<String,JsonObject>();
            for(var c:original.getAsJsonArray("categories"))for(var row:c.getAsJsonObject().getAsJsonArray("research"))rows.put(row.getAsJsonObject().get("key").getAsString(),row.getAsJsonObject());
            for(var c:json.getAsJsonArray("categories")){var entries=c.getAsJsonObject().getAsJsonArray("research");for(int i=0;i<entries.size();i++){var replacement=rows.get(entries.get(i).getAsJsonObject().get("key").getAsString());if(replacement!=null)entries.set(i,replacement.deepCopy());}}
        }return json;
    }
    @GameTest(template="wand_test_empty") public static void tinkererApiConditionalAndFixedIchorResearch(GameTestHelper test) throws Exception {
        boolean fire=TinkererConfig.FIRE.get(),mirrors=tc4revival.config.TCConfig.SERVER.allowMirrors.get();
        try{
            var raw=raw();JsonObject clone=null;JsonArray target=null;
            for(var c:raw.getAsJsonArray("categories"))if(c.getAsJsonObject().get("key").getAsString().equals("TT_CATEGORY")){target=c.getAsJsonObject().getAsJsonArray("research");clone=target.get(0).getAsJsonObject().deepCopy();break;}
            clone.addProperty("key",key("API_ADDED"));clone.addProperty("pageCount",0);clone.add("parents",new JsonArray());clone.add("parentsHidden",new JsonArray());target.add(clone);
            var pages=new JsonObject();pages.addProperty("key",key("API_ADDED"));pages.add("pages",new JsonArray());raw.getAsJsonArray("pages").add(pages);
            for(boolean enabled:new boolean[]{true,false,true}){
                TinkererConfig.FIRE.set(enabled);tc4revival.config.TCConfig.SERVER.allowMirrors.set(enabled);
                var resolved=ResearchPrerequisites.materialize(raw);var snapshot=ResearchDefinitions.decode(JsonParser.parseString(resolved.toString()).getAsJsonObject());
                var crop=snapshot.catalog().require(key("INFUSED_POTIONS"));test.assertTrue(crop.parents().size()==(enabled?6:0)&&crop.hiddenParents().contains("INFUSION"),"Fire configuration removes only the six fire prerequisites");
                var knowledge=new PlayerKnowledge(tc4revival.aspect.TCAspects.catalog());knowledge.rememberResearch("INFUSION");
                test.assertTrue(new ResearchRules(snapshot.catalog()).prerequisites(knowledge,key("INFUSED_POTIONS"))!=enabled,"Authoritative research eligibility follows materialized prerequisites");
                var ichor=snapshot.catalog().require(key("ICHOR"));test.assertTrue(!ichor.hiddenParents().contains(key("API_ADDED")),"New datapack research cannot change the fixed Ichor gate");
                test.assertTrue(!ichor.hiddenParents().contains("REVIVAL_RESEARCH_ASSISTANCE")&&!ichor.hiddenParents().contains("ELDRITCHMINOR"),"Modern and Eldritch research do not enter the fixed gate");
                test.assertTrue(ichor.hiddenParents().contains("THAUMIUM")&&ichor.hiddenParents().contains("FOCUSHELLBAT"),"Original hidden research remains in the author's fixed inventory");
                test.assertTrue(ichor.hiddenParents().contains("MIRROR")==enabled&&ichor.hiddenParents().contains(key("FIRE_IGNIS"))==enabled,"Disabled base/addon research is omitted from KAMI gate");
                test.assertTrue(!ichor.hiddenParents().contains(key("ICHORIUM"))&&snapshot.catalog().require(key("ICHORIUM")).hiddenParents().contains(key("ICHOR_CLOTH")),"KAMI keeps its own progression without cycles");
                knowledge.rememberResearch(key("ICHOR"));
                test.assertTrue(new ResearchRules(snapshot.catalog()).prerequisites(knowledge,key("ICHOR_CLOTH")),"Existing Ichor unlocks its child without rechecking the Ichor gate");
                test.assertTrue(resolved.getAsJsonArray("disabledResearch").contains(new JsonPrimitive(key("FIRE_IGNIS")))!=enabled,"Synchronized definition includes authoritative disabled state");
            }test.succeed();
        }finally{TinkererConfig.FIRE.set(fire);tc4revival.config.TCConfig.SERVER.allowMirrors.set(mirrors);}
    }
    @GameTest(template="wand_test_empty") public static void tinkererApiPouchFocusInventoryAndCurios(GameTestHelper test){
        for(boolean worn:new boolean[]{false,true}){
            var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TTFocus"));
            var pouch=new ItemStack(Tinkerer.ICHOR_POUCH.get());var contents=IchorPouchItem.contents(pouch);
            for(int i=0;i<116;i++)contents.set(i,new ItemStack(Items.DIAMOND,64));
            var incoming=new ItemStack(Tinkerer.FLIGHT_FOCUS.get());incoming.setHoverName(net.minecraft.network.chat.Component.literal("Preserved focus"));contents.set(116,incoming);IchorPouchItem.save(pouch,contents);
            if(worn){var curios=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow();curios.reset();curios.getStacksHandler("belt").orElseThrow().getStacks().setStackInSlot(0,pouch);}else player.getInventory().setItem(9,pouch);
            var wand=TCItems.CASTING_WAND.get().create(thaumcraft.api.wands.WandRod.get(key("ichorcloth")),thaumcraft.api.wands.WandCap.get(key("ichor")),false,true);player.setItemInHand(InteractionHand.MAIN_HAND,wand);
            var available=FocusSelection.available(player);test.assertTrue(available.foci().size()==1,"Selector discovers last slot of inventory/Curios pouch");
            FocusSelection.change(player,InteractionHand.MAIN_HAND,available.foci().firstKey());test.assertTrue(ItemStack.isSameItemSameTags(WandFocusItem.attached(wand),incoming)&&IchorPouchItem.contents(pouch).get(116).isEmpty(),"Selection transfers focus with NBT");
            FocusSelection.change(player,InteractionHand.MAIN_HAND,"REMOVE");test.assertTrue(WandFocusItem.attached(wand).isEmpty()&&ItemStack.isSameItemSameTags(IchorPouchItem.contents(pouch).get(116),incoming),"Removal returns to last free pouch slot");
            for(int i=0;i<116;i++)test.assertTrue(IchorPouchItem.contents(pouch).get(i).getCount()==64,"Ordinary pouch contents are never overwritten");
            test.assertTrue(FocusSelection.available(player).foci().size()==1,"Exactly one focus remains after roundtrip");
        }test.succeed();
    }
}
