package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ExcavationMiningServerTests {
    @GameTest(template="wand_test_empty")
    public static void excavationActualMiningDebitsOnlyLegacyEarthCost(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var box=new AABB(pos).inflate(2);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ExcavationAudit"));player.setGameMode(GameType.SURVIVAL);
        player.moveTo(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5,0,90);
        var focus=TCItems.EXCAVATION_FOCUS.get();var item=TCItems.CASTING_WAND.get();var earth=TCAspects.catalog().require("terra");int cases=0;
        try {
            for(var ore:new net.minecraft.world.level.block.Block[]{Blocks.IRON_ORE,Blocks.GOLD_ORE})for(boolean silk:new boolean[]{false,true})for(boolean dowsing:new boolean[]{false,true}){
                var core=new ItemStack(focus);if(silk)focus.applyUpgrade(core,FocusUpgrade.SILK_TOUCH,5);if(dowsing)focus.applyUpgrade(core,FocusUpgrade.DOWSING,3);
                for(int sample=0;sample<16;sample++){
                    level.getEntitiesOfClass(ItemEntity.class,box).forEach(ItemEntity::discard);
                    level.setBlock(pos,ore.defaultBlockState(),2);
                    var wand=item.create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,core);player.setItemInHand(InteractionHand.MAIN_HAND,wand);
                    var before=wand.getTag().copy();focus.cast(wand,level,player,InteractionHand.MAIN_HAND,null);
                    for(int tick=0;tick<40&&!level.isEmptyBlock(pos);tick++)focus.usingTick(wand,player,100-tick);
                    test.assertTrue(level.isEmptyBlock(pos),"Real ray and progress finish old ore");
                    var drops=level.getEntitiesOfClass(ItemEntity.class,box);var cluster=TCItems.cluster(ore==Blocks.IRON_ORE?ClusterType.IRON:ClusterType.GOLD);
                    test.assertTrue(drops.size()==1&&drops.get(0).getItem().getCount()==1,"One real world output");
                    var result=drops.get(0).getItem();test.assertTrue(result.is(silk?ore.asItem():ore==Blocks.IRON_ORE?Items.RAW_IRON:Items.RAW_GOLD)||!silk&&dowsing&&result.is(cluster),"Raw metal or dowsing cluster; Silk Touch preserves ore");
                    test.assertTrue(item.vis(wand,earth)==before.getInt("terra")-(silk?16:15),"Completed block pays earth cost for its installed upgrade");
                    for(String tag:new String[]{"aer","ignis","aqua","ordo","perditio"})test.assertTrue(wand.getTag().getInt(tag)==before.getInt(tag)-(silk?1:dowsing&&(tag.equals("ignis")||tag.equals("ordo"))?2:0),"Silk Touch and Dowsing pay their additional aspects");
                    focus.release(wand,level,player,0);cases++;
                }
            }
        } finally {focus.release(player.getMainHandItem(),level,player,0);level.getEntitiesOfClass(ItemEntity.class,box).forEach(ItemEntity::discard);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
        tc4revival.TC4Revival.LOGGER.info("PASS excavationActualMiningDebitsOnlyLegacyEarthCost: {} real ray/progress/drop/cost cases",cases);test.succeed();
    }
}
