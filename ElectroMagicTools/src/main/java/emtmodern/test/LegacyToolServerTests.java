package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.item.*;
import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class LegacyToolServerTests {
    public static ItemStack stack(String id){return new ItemStack(ElectroMagicTools.CONTENT.get(id).get());}
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void originalToolIdentityAndBoreModeUseUnmodifiedCoreMenu(GameTestHelper test){
        var level=test.getLevel();var player=GogglesServerTests.player(test);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var pos=test.absolutePos(new BlockPos(2,4,2));level.setBlockAndUpdate(pos,tc4revival.content.TCArcane.ARCANE_BORE.get().defaultBlockState());var bore=(tc4revival.content.ArcaneBoreBlockEntity)level.getBlockEntity(pos);var menu=new tc4revival.content.ArcaneBoreMenu(1,player.getInventory(),bore);
        var focus=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumcraft","focus_excavation")));boolean old=LegacyToolConfig.TOOLS_IN_BORE.get();
        try{
            LegacyToolConfig.TOOLS_IN_BORE.set(false);
            for(var profile:ToolProfile.ALL){var stack=stack(profile.id());test.assertTrue(menu.getSlot(1).mayPlace(stack)==profile.pick(),"Original pickaxe identity controls bore slot: "+profile.id());double damage=stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(m->m.getAmount()).sum();test.assertTrue(damage==(profile.pick()?4:6),"Original diamond pick/axe base attack modifier: "+profile.id());test.assertTrue(stack.getMaxDamage()==27,"Default old charge-display durability");}
            LegacyToolConfig.TOOLS_IN_BORE.set(true);
            var expected=java.util.Map.of("thaumium_drill",240,"iron_omnitool",200,"diamond_omnitool",350,"thaumium_omnitool",467,"rockbreaker_drill",2571);
            for(var entry:expected.entrySet()){
                var stack=stack(entry.getKey());var item=(ElectricPickaxe)stack.getItem();ElectricItem.manager.charge(stack,item.getMaxCharge(stack),Integer.MAX_VALUE,true,false);test.assertTrue(stack.getMaxDamage()==entry.getValue()&&tc4revival.content.ArcaneBoreEquipment.inspect(focus,stack).canDig(stack),"Real bore accepts charged extended-durability tool: "+entry.getKey());
                tc4revival.content.ArcaneBoreOutput.damagePick(stack,level);test.assertTrue(stack.getDamageValue()==1,"Core bore spends one real durability");double before=ElectricItem.manager.getCharge(stack);player.setShiftKeyDown(true);item.mineBlock(stack,level,Blocks.STONE.defaultBlockState(),pos,player);test.assertTrue(ElectricItem.manager.getCharge(stack)==before-1,"Bore option retains original one EU manual mining: "+entry.getKey()+" before="+before+" after="+ElectricItem.manager.getCharge(stack)+" cost="+item.breakCost());
            }
            for(String id:java.util.List.of("diamond_chainsaw","thaumium_chainsaw","stream_chainsaw","electric_growth_hoe","industrial_electric_hoe"))test.assertTrue(!menu.getSlot(1).mayPlace(stack(id)),"Non-pickaxe cannot enter bore: "+id);
        }finally{LegacyToolConfig.TOOLS_IN_BORE.set(old);}test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void legacyEnchantToggleAndUnpoweredChainsawShearing(GameTestHelper test){
        boolean old=emtmodern.energy.EmtEnergyConfig.ENCHANTING.get();
        try{for(boolean enabled:new boolean[]{false,true}){emtmodern.energy.EmtEnergyConfig.ENCHANTING.set(enabled);for(var profile:ToolProfile.ALL){var stack=stack(profile.id());test.assertTrue(stack.getItem().getEnchantmentValue()==(enabled?4:0)&&stack.getItem().isBookEnchantable(stack,new ItemStack(Items.ENCHANTED_BOOK))==enabled,"Original enchantment option applies: "+profile.id());if(enabled)test.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack,Enchantments.BLOCK_EFFICIENCY),"Correct vanilla tool class allows digging enchantments");}}
        }finally{emtmodern.energy.EmtEnergyConfig.ENCHANTING.set(old);}
        var player=GogglesServerTests.player(test);player.setPos(test.absolutePos(new BlockPos(2,5,2)).getCenter());
        for(String id:java.util.List.of("diamond_chainsaw","thaumium_chainsaw")){
            var stack=stack(id);var saw=(ElectricChainsaw)stack.getItem();var sheep=EntityType.SHEEP.create(test.getLevel());sheep.setPos(player.position());saw.interactLivingEntity(stack,player,sheep,InteractionHand.MAIN_HAND);test.assertTrue(sheep.isSheared()&&ElectricItem.manager.getCharge(stack)==0,"Original sheep shearing works without EU: "+id);sheep.setSheared(false);stack.getOrCreateTag().putBoolean("shearsMode",false);saw.interactLivingEntity(stack,player,sheep,InteractionHand.MAIN_HAND);test.assertTrue(!sheep.isSheared(),"Per-stack no-shear mode works: "+id);
        }
        test.succeed();
    }
}
