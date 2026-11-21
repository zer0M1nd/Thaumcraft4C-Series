package thmodern.alchemy;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class AlchemiteServerTests {
 @GameTest(template="empty")
 public static void ignitionCreatesCustomEntityAndPersistsFuse(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));var state=Horizons.ALCHEMITE.get().defaultBlockState();state.onCaughtFire(level,pos,net.minecraft.core.Direction.UP,null);
  var entities=level.getEntitiesOfClass(AlchemiteEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1));test.assertTrue(entities.size()==1&&entities.get(0).getFuse()==80,"Ignition creates one alchemite entity with legacy fuse");var entity=entities.get(0);entity.setFuse(17);var saved=new net.minecraft.nbt.CompoundTag();entity.saveWithoutId(saved);var restored=Horizons.ALCHEMITE_ENTITY.get().create(level);restored.load(saved);test.assertTrue(restored.getFuse()==17,"Fuse survives save/load");entity.discard();test.succeed();
 }
 @GameTest(template="empty")
 public static void explosionConvertsBlockAndPreservesNodeBranchRules(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));var source=Horizons.ALCHEMITE_ENTITY.get().create(level);source.ignite(pos.getX(),pos.getY(),pos.getZ(),null);var explosion=new AlchemiteExplosion(level,source);
  level.setBlock(pos,Blocks.COBBLESTONE.defaultBlockState(),3);explosion.transform(pos);test.assertTrue(level.isEmptyBlock(pos),"Ordinary block is removed after conversion");var crystals=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1),e->e.getItem().getItem() instanceof EssenceCrystalItem);test.assertTrue(!crystals.isEmpty()&&crystals.stream().allMatch(e->e.isInvulnerable()),"Conversion emits protected essence crystals");crystals.forEach(net.minecraft.world.entity.Entity::discard);
  var air=tc4revival.aspect.TCAspects.catalog().require("aer");for(boolean fading:new boolean[]{true,false}){level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),3);var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);node.configure(new tc4revival.aspect.AspectAmounts().add(air,20),AuraNodeType.NORMAL,AuraNodeModifier.BRIGHT,"alchemite-test",false);
   long seed=0;for(;;seed++){double roll=net.minecraft.util.RandomSource.create(seed).nextDouble();if(fading?roll>=.25&&roll<.5:roll>=.5)break;}level.random.setSeed(seed);explosion.transform(pos);test.assertTrue(level.getBlockEntity(pos)==node&&node.permanentBase().amount(air)==20,"Surviving node retains capacity");test.assertTrue(fading?node.permanentModifier()==AuraNodeModifier.FADING&&node.permanentType()==AuraNodeType.NORMAL:node.permanentModifier()==null&&node.permanentType()==AuraNodeType.UNSTABLE,"Selected branch edits permanent type/quality correctly");level.removeBlock(pos,false);
  }test.succeed();
 }
 @GameTest(template="empty")
 public static void realExplosionHonorsForgeCancellation(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));level.setBlock(pos,Blocks.COBBLESTONE.defaultBlockState(),3);var source=Horizons.ALCHEMITE_ENTITY.get().create(level);source.ignite(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,null);var explosion=new AlchemiteExplosion(level,source);
  java.util.function.Consumer<net.minecraftforge.event.level.ExplosionEvent.Start> listener=event->{if(event.getExplosion()==explosion)event.setCanceled(true);};net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(listener);try{explosion.detonate();}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);}test.assertTrue(level.getBlockState(pos).is(Blocks.COBBLESTONE)&&explosion.getToBlow().isEmpty(),"Cancelled explosion performs neither ray tracing nor transformation");test.succeed();
 }
}
