package thmodern.focus;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
/** Server-authoritative continuous melting; progress belongs to a player and exact block state. */
public final class LiquefactionFocusItem extends thaumcraft.api.wands.ItemFocusBasic {
 private record Work(BlockPos pos,BlockState state,float amount,long tick){}
 private final Map<ServerPlayer,Work> work=new WeakHashMap<>();
 @Override public int color(ItemStack focus){return 16711680;}
 // Must stay unique across every mod: the focus wheel and pouch/backpack selection use this key as identity.
 // The original addon reused the core excavation prefix ("BE"), which hid one of the two foci. Core startup
 // validation (FocusKeyValidation) now refuses duplicate keys.
 @Override public String sortingPrefix(){return "BQL";}
 @Override public Animation animation(ItemStack focus){return Animation.CHARGE;}
 @Override public Map<Aspect,Integer> costs(ItemStack focus){return cost(40);}
 private static Map<Aspect,Integer> cost(int amount){return Map.of(TCAspects.catalog().require("ignis"),amount);}
 @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){if(rank<1||rank>5)return List.of();return List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,rank==3?FocusUpgrade.ENLARGE:FocusUpgrade.require(rank==5?"thaumichorizons:purity":"thaumichorizons:nuggets"));}
 @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server)work.remove(server);player.startUsingItem(hand);return InteractionResultHolder.consume(stack);}
 @Override public void release(ItemStack stack,Level level,Player player,int remaining){if(player instanceof ServerPlayer server)work.remove(server);}
 public static ItemStack smelt(Level level,ItemStack input){return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(input),level).map(recipe->recipe.assemble(new SimpleContainer(input),level.registryAccess())).orElse(ItemStack.EMPTY);}
 public static String behavior(BlockState state){
  if(state.is(Blocks.STONE))return "lava";if(state.is(Blocks.TNT))return "tnt";if(state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.MYCELIUM))return "dirt";
  if(state.is(Blocks.ICE)||state.is(Blocks.PACKED_ICE)||state.is(Blocks.BLUE_ICE))return "water";
  if(state.is(BlockTags.LEAVES)||state.is(BlockTags.FLOWERS)||state.is(BlockTags.SAPLINGS)||state.is(Blocks.SNOW)||state.is(Blocks.SNOW_BLOCK)||state.is(Blocks.VINE)||state.is(Blocks.COBWEB)||state.is(Blocks.GRASS)||state.is(Blocks.TALL_GRASS)||state.is(Blocks.FERN)||state.is(Blocks.LARGE_FERN)||state.is(Blocks.DEAD_BUSH))return "air";
  return "";
 }
 private static boolean flammable(BlockState state,Level level,BlockPos pos){for(var side:Direction.values())if(state.isFlammable(level,pos,side))return true;return false;}
 @Override public void usingTick(ItemStack stack,Player player,int remaining){
  if(!(player instanceof ServerPlayer server)||!(stack.getItem() instanceof CastingWandItem wand)||!player.isAlive()||player.isSpectator()||player.getUseItem()!=stack)return;
  var level=server.serverLevel();var focus=WandFocusItem.attached(stack);if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,false)){work.remove(server);player.stopUsingItem();return;}
  var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(10));var block=level.clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
  var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,player,start,end,player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.1),e->e.isAlive()&&thaumcraft.api.wands.WandActors.canTarget(player,e)&&(e instanceof Mob||e instanceof ItemEntity));
  if(hit!=null)end=hit.getLocation();if(level.getGameTime()%3==0)thaumcraft.api.fx.LegacyEffects.wandBeam(player,end,0,16729156,1,false);
  if(hit!=null&&hit.getEntity() instanceof Mob mob){work.remove(server);if(wand.consumeForPlayer(stack,player,cost(15),false,false,true))mob.hurt(level.damageSources().inFire(),1+.5f*wand.focusPotency(stack,player));return;}
  if(hit!=null&&hit.getEntity() instanceof ItemEntity item&&!smelt(level,item.getItem()).isEmpty()){work.remove(server);meltItem(server,stack,wand,item);return;}
  if(block.getType()!=HitResult.Type.BLOCK){work.remove(server);return;}
  var pos=block.getBlockPos();var state=level.getBlockState(pos);String action=behavior(state);if(action.isEmpty()&&!flammable(state,level,pos)&&smelt(level,new ItemStack(state.getBlock())).isEmpty()){work.remove(server);return;}
  long tick=level.getGameTime();var previous=work.get(server);if(previous!=null&&previous.tick==tick)return;
  float progress=previous!=null&&previous.tick==tick-1&&previous.pos.equals(pos)&&previous.state.equals(state)?previous.amount:0;float hardness=action.equals("lava")?20:!action.isEmpty()||flammable(state,level,pos)?.25f:2;
  progress+=.15f+.05f*wand.focusPotency(stack,player);work.put(server,new Work(pos.immutable(),state,progress,tick));
  if(progress>=hardness){work.remove(server);meltBlock(server,stack,wand,pos);if(wand.focusUpgrade(stack,FocusUpgrade.ENLARGE)>0)for(var neighbor:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))if(!neighbor.equals(pos))meltBlock(server,stack,wand,neighbor.immutable());}
 }
 public boolean meltItem(ServerPlayer player,ItemStack stack,CastingWandItem wand,ItemEntity entity){
  var input=entity.getItem();var output=smelt(player.level(),input);if(output.isEmpty()||!entity.isAlive()||!wand.consumeForPlayer(stack,player,costs(WandFocusItem.attached(stack)),false,false,false))return false;
  // Legacy replaces one input item with one output, irrespective of recipe output count.
  int count=input.getCount();var spawned=new ArrayList<ItemEntity>();while(count>output.getMaxStackSize()){var split=new ItemEntity(player.level(),entity.getX(),entity.getY(),entity.getZ(),output.copyWithCount(output.getMaxStackSize()));if(!player.level().addFreshEntity(split)){spawned.forEach(Entity::discard);return false;}spawned.add(split);count-=output.getMaxStackSize();}
  if(!entity.isAlive()||entity.getItem()!=input||!wand.consumeForPlayer(stack,player,costs(WandFocusItem.attached(stack)),false,false,true)){spawned.forEach(Entity::discard);return false;}entity.setItem(output.copyWithCount(count));return true;
 }
 public boolean meltBlock(ServerPlayer player,ItemStack stack,CastingWandItem wand,BlockPos pos){
  var level=player.serverLevel();if(!player.mayBuild()||player.isSpectator()||!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,stack))return false;
  var state=level.getBlockState(pos);if(state.isAir()||state.hasBlockEntity()||state.getDestroySpeed(level,pos)<0)return false;
  var focus=WandFocusItem.attached(stack);var input=new ItemStack(state.getBlock());var output=state.is(Blocks.STONE)?ItemStack.EMPTY:smelt(level,input);String action=behavior(state);boolean burning=flammable(state,level,pos);if(output.isEmpty()&&action.isEmpty()&&!burning)return false;
  if(net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)==-1||!level.getBlockState(pos).equals(state)||!wand.consumeForPlayer(stack,player,costs(focus),false,false,false))return false;
  if(!output.isEmpty()&&getUpgradeLevel(focus,"thaumichorizons:purity")>0){var pure=smelt(level,SpecialMining.apply(input,9999,level.random));if(!pure.isEmpty())output=pure;}
  var drops=new ArrayList<ItemStack>();BlockState replacement=Blocks.AIR.defaultBlockState();boolean ignite=false;
  if(!output.isEmpty()){
   if(output.getItem() instanceof BlockItem blockItem){replacement=blockItem.getBlock().defaultBlockState();if(output.getCount()>1)drops.add(output.copyWithCount(output.getCount()-1));}else drops.add(output.copy());
   int nuggets=getUpgradeLevel(focus,"thaumichorizons:nuggets");var bonus=InfernalBonusRecipe.find(level,input);if(nuggets>0&&bonus.isPresent()){int count=0;for(int i=0;i<nuggets;i++)if(level.random.nextFloat()<.45f)count++;if(count>0)drops.add(bonus.get().getResultItem(level.registryAccess()).copyWithCount(count));}
  }else replacement=switch(action){case "lava"->Blocks.LAVA.defaultBlockState();case "water"->level.dimensionType().ultraWarm()?Blocks.AIR.defaultBlockState():Blocks.WATER.defaultBlockState();case "dirt"->Blocks.DIRT.defaultBlockState();case "tnt"->{ignite=true;yield Blocks.AIR.defaultBlockState();}case "air"->Blocks.AIR.defaultBlockState();default->Blocks.FIRE.defaultBlockState();};
  var spawned=new ArrayList<Entity>();for(var drop:drops){var entity=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,drop);if(!level.addFreshEntity(entity)){spawned.forEach(Entity::discard);return false;}spawned.add(entity);}
  if(ignite){var tnt=new net.minecraft.world.entity.item.PrimedTnt(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,player);if(!level.addFreshEntity(tnt)){spawned.forEach(Entity::discard);return false;}spawned.add(tnt);}
  if(!level.getBlockState(pos).equals(state)||!level.setBlock(pos,replacement,3)){spawned.forEach(Entity::discard);return false;}
  if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,true)){level.setBlock(pos,state,3);spawned.forEach(Entity::discard);return false;}level.levelEvent(2001,pos,Block.getId(state));return true;
 }
}

