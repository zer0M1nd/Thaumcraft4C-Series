package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class EnchantmentServerTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test){var p=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTEnchant"));p.moveTo(test.absolutePos(new BlockPos(2,2,2)).getCenter());return p;}
    private static ItemStack enchanted(Item item,String type,int rank){var stack=new ItemStack(item);stack.enchant(TinkererEnchantments.get(type),rank);return stack;}
    @GameTest(template="wand_test_empty")
    public static void tinkererEnchantmentRegistryCostsAndCompatibility(GameTestHelper test){
        var authored=new java.util.HashSet<net.minecraft.resources.ResourceLocation>();
        try(var reader=test.getLevel().getServer().getResourceManager().openAsReader(net.minecraft.resources.ResourceLocation.parse("thaumic_tinkerer:osmotic_enchantments/main.json"))){
            for(var value:com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("entries")){
                var row=value.getAsJsonObject();var id=net.minecraft.resources.ResourceLocation.parse(row.get("enchantment").getAsString());authored.add(id);
                var loaded=EnchanterCatalog.entries().get(id);var costs=new java.util.TreeMap<String,Integer>();row.getAsJsonObject("aspects").entrySet().forEach(e->costs.put(e.getKey(),e.getValue().getAsInt()));
                test.assertTrue(loaded!=null&&loaded.base().equals(costs)&&loaded.research().equals(row.has("research")?row.get("research").getAsString():"")&&loaded.icon().toString().equals(row.get("icon").getAsString()),"Authored osmotic definition remains authoritative: "+id);
            }
        }catch(java.io.IOException failure){throw new RuntimeException(failure);}
        test.assertTrue(TinkererEnchantments.names().size()==14&&authored.size()==40,"All 14 legacy enchantments and 40 authored osmotic definitions loaded");
        var expected=new java.util.HashSet<>(authored);
        for(var resource:test.getLevel().getServer().getResourceManager().listResources("osmotic_enchantments",id->id.getPath().endsWith(".json")).values()){
            try(var reader=resource.openAsReader()){
                for(var value:com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("entries"))
                    expected.add(net.minecraft.resources.ResourceLocation.parse(value.getAsJsonObject().get("enchantment").getAsString()));
            }catch(java.io.IOException failure){throw new RuntimeException(failure);}
        }
        if(net.minecraftforge.fml.ModList.get().isLoaded("thaumicbases"))expected.addAll(ttmodern.compat.BasesEnchantments.entries().keySet());
        test.assertTrue(EnchanterCatalog.entries().keySet().equals(expected),"Catalog contains exactly all loaded datapack definitions plus installed Bases compatibility entries: actual="+EnchanterCatalog.entries().keySet()+", expected="+expected);
        var jump=TinkererEnchantments.get("ascent_boost");test.assertTrue(jump.getMaxLevel()==4&&!jump.isDiscoverable()&&!jump.isTradeable()&&!jump.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_LEGGINGS)),"Exclusive enchantment progression");
        test.assertTrue(jump.canEnchant(new ItemStack(Items.DIAMOND_LEGGINGS))&&!jump.canEnchant(new ItemStack(Items.DIAMOND_BOOTS)),"Equipment category enforced");
        var costs=EnchanterCatalog.find(jump).cost(4);test.assertTrue(costs.get("aer")==72&&costs.get("perditio")==57,"Legacy exponential costs rounded down");
        test.assertTrue(EnchanterCatalog.find(TinkererEnchantments.get("focused_strike")).research().equals(Tinkerer.ID+":TTENCH_VAMPIRISM")&&EnchanterCatalog.find(TinkererEnchantments.get("focused_strike")).researchAlternatives().contains(Tinkerer.ID+":TTENCH_FOCUSED"),"Legacy advanced gate and existing-save alternative are retained");
        test.assertTrue(!TinkererEnchantments.get("quick_draw").isCompatibleWith(Enchantments.PUNCH_ARROWS)&&!TinkererEnchantments.get("tunnel").isCompatibleWith(TinkererEnchantments.get("shatter")),"Symmetric incompatibilities");
        test.assertTrue(TinkererEnchantments.get("final_strike").isCompatibleWith(Enchantments.BANE_OF_ARTHROPODS),"Repeated smite entry repaired to all three damage enchants");
        test.assertTrue(!TinkererEnchantments.get("flaming_touch").canEnchant(new ItemStack(tc4revival.content.TCItems.ELEMENTAL_AXE.get())),"Elemental axe exclusion preserved");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererEnchantmentCombatCapsAndTargetReset(GameTestHelper test){
        var p=player(test);var target=EntityType.COW.create(test.getLevel());var other=EntityType.COW.create(test.getLevel());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,enchanted(Items.DIAMOND_SWORD,"focused_strike",1));
        var source=test.getLevel().damageSources().playerAttack(p);float first=0,last=0;
        for(int i=0;i<12;i++){var event=new LivingHurtEvent(target,source,10);MinecraftForge.EVENT_BUS.post(event);if(i==0)first=event.getAmount();last=event.getAmount();}
        test.assertTrue(first==5&&last==17.5f&&p.getMainHandItem().getTag().getInt("TTConsecutive")==5,"Focused hits begin weak and cap at five follow-ups");
        var changed=new LivingHurtEvent(other,source,10);MinecraftForge.EVENT_BUS.post(changed);test.assertTrue(changed.getAmount()==5,"Changing target resets consecutive bonus");
        p.setHealth(5);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,enchanted(Items.DIAMOND_SWORD,"vampirism",2));MinecraftForge.EVENT_BUS.post(new LivingHurtEvent(target,source,4));test.assertTrue(p.getHealth()==7,"Vampirism heals the original two health");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,enchanted(Items.DIAMOND_SWORD,"valiance",5));var valiance=new LivingHurtEvent(target,source,10);MinecraftForge.EVENT_BUS.post(valiance);test.assertTrue(valiance.getAmount()==15,"Low-health damage bonus");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererEnchantmentMovementAndDraw(GameTestHelper test){
        var p=player(test);p.setItemSlot(EquipmentSlot.LEGS,enchanted(Items.DIAMOND_LEGGINGS,"ascent_boost",4));p.setDeltaMovement(0,.42,0);MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(p));test.assertTrue(Math.abs(p.getDeltaMovement().y-1.26)<.0001,"Ascent IV triples jump impulse");
        p.setShiftKeyDown(true);p.setDeltaMovement(0,.42,0);MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(p));test.assertTrue(Math.abs(p.getDeltaMovement().y-.42)<.0001,"Sneaking disables jump boost");p.setShiftKeyDown(false);
        p.setItemSlot(EquipmentSlot.FEET,enchanted(Items.DIAMOND_BOOTS,"slow_fall",3));p.setDeltaMovement(0,-1,0);p.fallDistance=8;MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(p));test.assertTrue(p.getDeltaMovement().y>-.51&&p.fallDistance==7,"Slow fall modifies velocity and fall distance");
        var bow=enchanted(Items.BOW,"quick_draw",2);var draw=new LivingEntityUseItemEvent.Tick(p,bow,72000);MinecraftForge.EVENT_BUS.post(draw);test.assertTrue(draw.getDuration()==71999,"Quick draw II adds one use tick each four remaining ticks");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererEnchantmentMiningSpeedHasNoDurabilitySideEffects(GameTestHelper test){
        var p=player(test);var pos=p.blockPosition();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,enchanted(Items.DIAMOND_PICKAXE,"disintegrate",1));
        for(int i=0;i<20;i++){var event=new PlayerEvent.BreakSpeed(p,Blocks.STONE.defaultBlockState(),2,pos);MinecraftForge.EVENT_BUS.post(event);test.assertTrue(event.getNewSpeed()==Float.MAX_VALUE,"Soft block instant mining");}
        test.assertTrue(p.getMainHandItem().getDamageValue()==0,"Repeated speed queries do not damage item");var hard=new PlayerEvent.BreakSpeed(p,Blocks.OBSIDIAN.defaultBlockState(),2,pos);MinecraftForge.EVENT_BUS.post(hard);test.assertTrue(hard.isCanceled(),"Disintegrate cannot mine hard blocks");
        var tool=enchanted(Items.DIAMOND_PICKAXE,"tunnel",5);tool.getOrCreateTag().putFloat("TTTunnelDirection",359);p.setYRot(1);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);var seam=new PlayerEvent.BreakSpeed(p,Blocks.STONE.defaultBlockState(),2,pos);MinecraftForge.EVENT_BUS.post(seam);test.assertTrue(seam.getNewSpeed()==4,"Tunnel direction wraps across zero degrees");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,enchanted(Items.DIAMOND_PICKAXE,"shatter",2));var shatter=new PlayerEvent.BreakSpeed(p,Blocks.OBSIDIAN.defaultBlockState(),2,pos);MinecraftForge.EVENT_BUS.post(shatter);test.assertTrue(shatter.getNewSpeed()==12,"Shatter uses actual target hardness");test.succeed();
    }
}
