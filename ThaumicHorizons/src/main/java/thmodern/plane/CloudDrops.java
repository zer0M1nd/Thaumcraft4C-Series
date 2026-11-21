package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
/** Periodic products and planting; invoked only while the authoritative rain flag is true. */
public final class CloudDrops {
 public static void release(CloudEntity cloud,ServerLevel level){
  var pos=cloud.getBlockPos();double x=pos.getX()+level.random.nextDouble(),y=pos.getY()+.5,z=pos.getZ()+level.random.nextDouble();
  switch(cloud.type().id()){
   case "thunder_cloud","animus_cloud"->{
    var bolt=CloudBlocks.LIGHTNING.get().create(level);if(bolt==null)return;bolt.moveTo(cloud.ground().getX()+.5,cloud.ground().getY(),cloud.ground().getZ()+.5);bolt.length(cloud.distance());
    if(level.addFreshEntity(bolt)&&cloud.type().id().equals("animus_cloud"))thmodern.golem.AnimatedGolems.animateNaturally(level,cloud.ground(),1200);
   }
   case "alloy_cloud"->drop(level,x,y,z,alloy(level.random.nextInt(75)));
   case "flesh_cloud"->{int roll=level.random.nextInt(10);Item item=switch(roll){case 0->Items.BEEF;case 1->Items.PORKCHOP;case 2->Items.CHICKEN;case 3->List.of(Items.COD,Items.SALMON,Items.TROPICAL_FISH).get(level.random.nextInt(3));default->CloudBlocks.MEAT.get();};drop(level,x,y,z,new ItemStack(item));}
   case "vis_cloud"->level.addFreshEntity(new tc4revival.content.AspectOrbEntity(level,x,y,z,tc4revival.aspect.TCAspects.catalog().primals().get(level.random.nextInt(6)),1));
   case "glyph_cloud"->level.addFreshEntity(new ExperienceOrb(level,x,y,z,level.random.nextInt(4)));
   case "spore_cloud"->plant(level,cloud.ground());
  }
 }
 private static void drop(ServerLevel level,double x,double y,double z,ItemStack stack){var entity=new ItemEntity(level,x,y,z,stack);entity.setInvulnerable(true);level.addFreshEntity(entity);}
 public static ItemStack alloy(int roll){
  if(roll<0||roll>=75)throw new IllegalArgumentException("Invalid alloy roll");
  if(roll<6)return new ItemStack(Items.GOLD_NUGGET);
  if(roll>=50)return new ItemStack(Items.IRON_NUGGET);
  if(roll>=40)return new ItemStack(tc4revival.content.TCItems.nugget(tc4revival.content.NuggetType.QUICKSILVER));
  String material=roll<12?"silver":roll<20?"copper":roll<30?"tin":"lead";
  var ingots=ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(ResourceLocation.parse("forge:ingots/"+material)));
  var nuggets=ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(ResourceLocation.parse("forge:nuggets/"+material)));
  return new ItemStack(ingots.isEmpty()?Items.IRON_NUGGET:nuggets.stream().sorted(Comparator.comparing(i->ForgeRegistries.ITEMS.getKey(i).toString())).findFirst().orElse(Items.IRON_NUGGET));
 }
 private static BlockState block(String id){var key=ResourceLocation.parse(id);if(!ForgeRegistries.BLOCKS.containsKey(key))throw new IllegalStateException("Missing cloud plant "+id);return ForgeRegistries.BLOCKS.getValue(key).defaultBlockState();}
 public static void plant(ServerLevel level,BlockPos ground){
  if(level.isOutsideBuildHeight(ground)||!level.isEmptyBlock(ground.above()))return;
  var state=level.getBlockState(ground);var random=level.random;BlockState next=null;
  if(state.is(Blocks.FARMLAND)){int roll=random.nextInt(8);next=(roll==1?Blocks.MELON_STEM:roll==2?Blocks.PUMPKIN_STEM:Blocks.WHEAT).defaultBlockState();}
  else if(state.is(Blocks.DIRT)){level.setBlockAndUpdate(ground,(random.nextInt(10)==4?Blocks.MYCELIUM:Blocks.GRASS_BLOCK).defaultBlockState());return;}
  else if(state.is(Blocks.STONE)||state.is(Blocks.MYCELIUM))next=(random.nextInt(3)==1?Blocks.BROWN_MUSHROOM:Blocks.RED_MUSHROOM).defaultBlockState();
  else if(state.is(Blocks.GRASS_BLOCK)){
   int roll=random.nextInt(1000);
   if(roll==666)next=block("thaumcraft:silverwood_sapling");
   else if(roll<750)next=block("minecraft:"+List.of("grass","fern","dandelion","poppy","poppy","blue_orchid","allium","azure_bluet","red_tulip","orange_tulip","white_tulip","pink_tulip","oxeye_daisy","sugar_cane").get(random.nextInt(14)));
   else if(roll<950){int tree=random.nextInt(100);next=block(tree<10?"thaumcraft:greatwood_sapling":"minecraft:"+List.of("oak","spruce","birch","jungle","acacia","dark_oak").get(Math.min(5,(tree-10)/15))+"_sapling");}
   else next=block(roll<975?"thaumcraft:shimmerleaf":"thaumcraft:vishroom");
  }else if(state.is(Blocks.SAND))next=random.nextInt(10)==4?block("thaumcraft:cinderpearl"):Blocks.CACTUS.defaultBlockState();
  else if(state.is(Blocks.SOUL_SAND))next=Blocks.NETHER_WART.defaultBlockState();
  if(next!=null&&next.canSurvive(level,ground.above()))level.setBlockAndUpdate(ground.above(),next);
 }
}
