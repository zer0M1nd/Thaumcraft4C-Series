package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class SpecialMiningAuditServerTests {
    @GameTest(template="wand_test_empty")
    public static void specialMiningBoundariesAndBoreInputs(GameTestHelper test){
        Item[] inputs={Items.IRON_ORE,Items.GOLD_ORE,Items.COPPER_ORE,Items.RAW_COPPER,TCArcane.ore(0).asItem()};
        ClusterType[] types={ClusterType.IRON,ClusterType.GOLD,ClusterType.COPPER,ClusterType.COPPER,ClusterType.CINNABAR};
        int cases=0;
        for(int i=0;i<inputs.length;i++)for(int fortune:new int[]{0,3}){
            float threshold=(.2f+fortune*.075f)*(i==1||i==4?.9f:1);
            for(float value:new float[]{0,threshold,Math.nextUp(threshold),.999f}){
                var input=new ItemStack(inputs[i],3);input.getOrCreateTag().putString("audit","preserve on failure");
                var snapshot=input.copy();var random=new FixedRandom(value);var output=SpecialMining.apply(input,fortune,random);
                boolean converted=value<=threshold;
                test.assertTrue(output.getCount()==3&&output.is(converted?TCItems.cluster(types[i]):inputs[i]),"Inclusive conversion threshold and stack multiplicity");
                test.assertTrue(ItemStack.matches(input,snapshot)&&output!=input,"Input not mutated, returns independent stack");
                test.assertTrue(converted?!output.hasTag():ItemStack.matches(output,snapshot),"Converted result uses cluster template; failed conversion preserves NBT");
                test.assertTrue(random.draws==1,"Exactly one float draw");cases++;
            }
        }
        for(var item:new Item[]{Items.DIRT}){
            var input=new ItemStack(item,3);var random=new FixedRandom(0);test.assertTrue(ItemStack.matches(input,SpecialMining.apply(input,0,random))&&random.draws==1,"Unmapped modern inputs remain unchanged but consume draw");cases++;
        }
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        var worker=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MiningAudit"));
        try {
            for(var ore:new Block[]{Blocks.IRON_ORE,Blocks.GOLD_ORE})for(boolean silk:new boolean[]{false,true})for(boolean dowsing:new boolean[]{false,true}){
                level.setBlock(pos,ore.defaultBlockState(),2);
                var equipment=new ArcaneBoreEquipment(true,true,20,0,0,silk,dowsing);
                var result=ArcaneBoreOutput.collect(level,pos,worker,equipment);
                var expected=!silk&&dowsing?TCItems.cluster(ore==Blocks.IRON_ORE?ClusterType.IRON:ClusterType.GOLD):silk?ore.asItem():ore==Blocks.IRON_ORE?Items.RAW_IRON:Items.RAW_GOLD;
                test.assertTrue(result.size()==1&&result.get(0).is(expected)&&result.get(0).getCount()>=1,"Bore retains modern raw drops, gates dowsing on silk, then converts");cases++;
            }
        } finally {level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
        tc4revival.TC4Revival.LOGGER.info("PASS specialMiningBoundariesAndBoreInputs: {} shared boundaries and actual bore collection cases",cases);test.succeed();
    }
    private static  class FixedRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource{
        private final float value;int draws;
        FixedRandom(float value){super(0);this.value=value;}
        @Override public float nextFloat(){draws++;return value;}
    }
}
