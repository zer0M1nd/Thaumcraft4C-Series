package tc4revival.content;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class DetectionToolsServerTests {
    @GameTest(template="wand_test_empty")
    public static void detectionSinisterConeAndExpiry(GameTestHelper test) {
        var eye=new Vec3(.5,.5,.5);var forward=new Vec3(0,0,1);
        test.assertTrue(SinisterNodeMemory.inCone(eye,forward,eye.add(0,0,255))&&!SinisterNodeMemory.inCone(eye,forward,eye.add(0,0,256)),"Strict cone axial range is below 256");
        test.assertTrue(SinisterNodeMemory.inCone(eye,forward,eye.add(50,0,250)),"Axial range permits diagonal distances greater than 256");
        test.assertTrue(!SinisterNodeMemory.inCone(eye,forward,eye.add(100,0,100))&&!SinisterNodeMemory.inCone(eye,forward,eye.add(0,0,-5))&&!SinisterNodeMemory.inCone(eye,forward,eye),"Outside aperture, behind and zero-distance points do not activate");
        test.assertTrue(SinisterNodeMemory.inCone(eye,new Vec3(0,1,0),eye.add(0,100,0)),"Vertical aim is part of the cone");
        var cache=new SinisterNodeMemory();var overworld=ResourceLocation.parse("minecraft:overworld");var nether=ResourceLocation.parse("minecraft:the_nether");cache.observe(overworld,new BlockPos(0,0,20),1000);
        test.assertTrue(!cache.active(nether,eye,forward,1001)&&cache.active(overworld,eye,forward,11000)&&cache.size()==1,"Dimension isolation and inclusive ten-second boundary");
        test.assertTrue(!cache.active(overworld,eye,forward,11001)&&cache.size()==0&&!cache.active(overworld,eye,forward,11002),"Expired nodes stop contributing immediately");
        cache.observe(overworld,new BlockPos(0,0,20),20000);cache.observe(overworld,new BlockPos(0,0,20),25000);test.assertTrue(cache.size()==1&&cache.active(overworld,eye,forward,34000),"Repeated node observations refresh the same entry");pass(test,"sinister-cone-cache");
    }
    @GameTest(template="wand_test_empty")
    public static void detectionSanityGaugeAndKnowledge(GameTestHelper test) {
        var empty=SanityGauge.of(0,0,0);test.assertTrue(empty.gap()==48&&empty.temporary()==0&&!empty.saturated(),"Empty gauge");
        var mixed=SanityGauge.of(20,30,10);test.assertTrue(mixed.gap()==19&&mixed.temporary()==4&&mixed.sticky()==14&&!mixed.saturated(),"Three warp categories retain old truncation");
        var saturated=SanityGauge.of(60,40,20);test.assertTrue(saturated.gap()==0&&saturated.temporary()==8&&saturated.sticky()==16&&saturated.saturated(),"Above one hundred warp scales proportionally and shows saturation");
        test.assertTrue(SanityGauge.of(100,0,0).saturated()&&SanityGauge.of(0,0,1).temporary()==0,"Exact saturation and subpixel old values");
        var knowledge=tc4revival.research.PlayerKnowledge.firstLogin(tc4revival.aspect.TCAspects.catalog(),bound->0);knowledge.setWarp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT,20);knowledge.setWarp(tc4revival.research.PlayerKnowledge.WarpKind.STICKY,30);knowledge.setWarp(tc4revival.research.PlayerKnowledge.WarpKind.TEMPORARY,10);
        test.assertTrue(SanityGauge.of(knowledge.warp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT),knowledge.warp(tc4revival.research.PlayerKnowledge.WarpKind.STICKY),knowledge.warp(tc4revival.research.PlayerKnowledge.WarpKind.TEMPORARY)).equals(mixed),"Gauge reads the real synchronized knowledge categories");pass(test,"sanity-gauge");
    }
    @GameTest(template="wand_test_empty")
    public static void detectionToolInfusionsAndIdentity(GameTestHelper test) {
        for(boolean sanity:new boolean[]{true,false}) {
            var center=new ItemStack(sanity?TCItems.THAUMOMETER.get():Items.FLINT);var parts=sanity?java.util.List.of(new ItemStack(TCItems.material(MaterialType.MIRROR_GLASS)),new ItemStack(TCItems.ZOMBIE_BRAIN.get()),new ItemStack(Items.DIAMOND)):java.util.List.of(new ItemStack(TCItems.NITOR.get()),new ItemStack(TCItems.shard(ShardType.ORDER)),new ItemStack(TCItems.KNOWLEDGE_FRAGMENT.get()),new ItemStack(TCItems.shard(ShardType.ENTROPY)));
            String research=sanity?"SANITYCHECK":"SINSTONE";var input=new InfusionRecipe.Input(center,parts,k->k.equals(research));var recipe=InfusionRecipe.find(test.getLevel(),input).orElseThrow();var plan=recipe.plan(center);var output=recipe.assemble(input,test.getLevel().registryAccess());
            test.assertTrue(output.is(sanity?TCItems.SANITY_CHECKER.get():TCItems.SINISTER_STONE.get())&&plan.instability()==(sanity?4:5)&&plan.aspects().total()==(sanity?56:32)&&!recipe.matches(new InfusionRecipe.Input(center,parts,k->false),test.getLevel()),"Original infusion, instability, aspects and research gate");
            test.assertTrue(output.getMaxStackSize()==1&&output.getRarity()==(sanity?Rarity.UNCOMMON:Rarity.RARE),"Original item limits and rarity");
        }
        var stone=new ItemStack(TCItems.SINISTER_STONE.get());test.assertTrue(tc4revival.aspect.item.ItemStackAspects.identity(stone).orElseThrow().metadata()==0,"Normal stone has metadata-zero scan identity");test.assertTrue(WarpingGear.contribution(stone,null)==1,"Normal sinister stone contributes one held warp");stone.getOrCreateTag().putBoolean("LegacyActive",true);test.assertTrue(WarpingGear.contribution(stone,null)==0,"Display subtype retains original separate warp identity");test.assertTrue(SinisterStoneItem.legacyMetadata(stone)==1&&tc4revival.aspect.item.ItemStackAspects.identity(stone).orElseThrow().metadata()==1,"Legacy forced-active subtype remains representable");pass(test,"infusions-identity");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Detection tools test passed: {}",name);test.succeed(); }
}
