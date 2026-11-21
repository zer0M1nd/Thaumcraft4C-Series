package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocalStartServerTests {
    @GameTest(template="wand_test_empty")
    public static void focalFiveRanksValidateExperienceAndPersistPayment(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FocalStart"));player.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        level.setBlock(pos,TCArcane.FOCAL_BLOCK.get().defaultBlockState(),2);var tile=(FocalManipulatorBlockEntity)level.getBlockEntity(pos);int cases=0;
        try {
            for(int rank=1;rank<=5;rank++)for(boolean creative:new boolean[]{false,true})for(int surplus:new int[]{-1,0,8}){
                tile.clearContent();var focus=new ItemStack(TCItems.EXCAVATION_FOCUS.get());
                for(int previous=1;previous<rank;previous++)TCItems.EXCAVATION_FOCUS.get().applyUpgrade(focus,FocusUpgrade.POTENCY,previous);
                tile.setItem(0,focus);player.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);int xp=14+surplus;
                player.experienceLevel=0;player.totalExperience=0;player.experienceProgress=0;
                player.giveExperiencePoints(FocalUpgradePlan.experiencePoints(xp));
                test.assertTrue(!tile.start(player,999)&&player.experienceLevel==xp,"Unknown upgrade rejected without charge");
                boolean accepted=tile.start(player,FocusUpgrade.FRUGAL.id);
                test.assertTrue(accepted==(creative||surplus>=0),"One selected upgrade requires 280 XP (level fourteen); creative is exempt");
                test.assertTrue(player.totalExperience==FocalUpgradePlan.experiencePoints(xp)-(accepted&&!creative?280:0),"Fixed survival XP-point debit; creative is exempt");
                if(accepted){
                    int amount=200<<(rank-1);var remaining=tile.remaining();
                    // Old fames = victus + vacuos = aqua + terra + aer + perditio.
                    test.assertTrue(remaining.size()==4&&tile.size()==amount*4&&tile.rank()==rank,"Exponential rank cost and sum reduction");
                    for(String primal:new String[]{"aer","aqua","terra","perditio"})test.assertTrue(remaining.amount(TCAspects.catalog().require(primal))==amount,"Exact fames primal cost");
                    var saved=tile.saveWithoutMetadata();tile.load(saved);test.assertTrue(saved.equals(tile.saveWithoutMetadata()),"Active payment and focus survive NBT round trip");
                    int after=player.experienceLevel;test.assertTrue(!tile.start(player,FocusUpgrade.POTENCY.id)&&player.experienceLevel==after,"Busy transaction rejects repeated charge");
                    var replacement=new ItemStack(TCItems.EXCAVATION_FOCUS.get());tile.setItem(0,replacement);
                    test.assertTrue(tile.size()==0&&tile.remaining().isEmpty()&&TCItems.EXCAVATION_FOCUS.get().nextRank(replacement)==1,"Replacement cancels original payment without free upgrade");
                }else test.assertTrue(tile.size()==0,"Rejected start has no in-flight cost");
                cases++;
            }
        } finally {level.removeBlockEntity(pos);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
        tc4revival.TC4Revival.LOGGER.info("PASS focalFiveRanksValidateExperienceAndPersistPayment: {} rank/creative/XP starts with NBT/busy/replacement boundaries",cases);test.succeed();
    }
}
