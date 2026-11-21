package thmodern.focus;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.content.*;
public final class DisintegrationFocusItem extends thaumcraft.api.wands.ItemFocusBasic {
 private record Work(BlockPos pos,BlockState state,float progress,long tick){}
 private final Map<ServerPlayer,Work> work=new WeakHashMap<>();
 @Override public int color(ItemStack focus){return 7930047;}
 @Override public String sortingPrefix(){return "DS";}
 @Override public Animation animation(ItemStack focus){return Animation.CHARGE;}
 private static Map<Aspect,Integer> cost(int n){var c=TCAspects.catalog();return Map.of(c.require("ignis"),n,c.require("perditio"),n,c.require("terra"),n);}
 @Override public Map<Aspect,Integer> costs(ItemStack focus){return cost(30);}
 @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){if(rank<1||rank>5)return List.of();var result=new ArrayList<>(List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY));if(rank==3)result.add(FocusUpgrade.ENLARGE);if(rank==5)result.add(FocusUpgrade.require("thaumichorizons:enervation"));return result;}
 @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server)work.remove(server);player.startUsingItem(hand);return InteractionResultHolder.consume(stack);}
 @Override public void release(ItemStack stack,Level level,Player player,int remaining){if(player instanceof ServerPlayer server)work.remove(server);}
 public static AspectAmounts aspects(ItemStack input){return input.isEmpty()||input.getItem() instanceof EssenceCrystalItem?new AspectAmounts():ItemStackAspects.resolve(input.copyWithCount(1)).orElseGet(AspectAmounts::new);}
 @Override public void usingTick(ItemStack stack,Player player,int remaining){
  if(!(player instanceof ServerPlayer server)||!(stack.getItem() instanceof CastingWandItem wand)||!player.isAlive()||player.isSpectator()||player.getUseItem()!=stack)return;
  var level=server.serverLevel();var focus=WandFocusItem.attached(stack);if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,false)){work.remove(server);player.stopUsingItem();return;}
  var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(10));var block=level.clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
  var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,player,start,end,player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.1),e->e.isAlive()&&thaumcraft.api.wands.WandActors.canTarget(player,e)&&(e instanceof Mob||e instanceof ItemEntity item&&!(item.getItem().getItem() instanceof EssenceCrystalItem)));
  if(hit!=null)end=hit.getLocation();if(level.getGameTime()%3==0)thaumcraft.api.fx.LegacyEffects.wandBeam(player,end,0,7930047,1,false);
  if(hit!=null&&hit.getEntity() instanceof Mob mob){work.remove(server);if(wand.consumeForPlayer(stack,player,cost(12),false,false,true)){int enervation=getUpgradeLevel(focus,"thaumichorizons:enervation");if(enervation>0)mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,40,0));var type=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.parse("thaumcraft:dissolve"));mob.hurt(new net.minecraft.world.damagesource.DamageSource(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(type)),.5f+.25f*wand.focusPotency(stack,player)+.5f*enervation);}return;}
  if(hit!=null&&hit.getEntity() instanceof ItemEntity item&&!aspects(item.getItem()).isEmpty()){work.remove(server);dissolveItem(server,stack,wand,item);return;}
  if(block.getType()!=HitResult.Type.BLOCK){work.remove(server);return;}var pos=block.getBlockPos();var state=level.getBlockState(pos);float hardness=state.getDestroySpeed(level,pos);if(hardness<0||state.hasBlockEntity()||aspects(new ItemStack(state.getBlock())).isEmpty()){work.remove(server);return;}
  long tick=level.getGameTime();var previous=work.get(server);if(previous!=null&&previous.tick==tick)return;float progress=previous!=null&&previous.tick==tick-1&&previous.pos.equals(pos)&&previous.state.equals(state)?previous.progress:0;progress+=.05f+.015f*wand.focusPotency(stack,player);work.put(server,new Work(pos.immutable(),state,progress,tick));
  if(progress>=hardness){work.remove(server);dissolveBlock(server,stack,wand,pos);if(wand.focusUpgrade(stack,FocusUpgrade.ENLARGE)>0)for(var neighbor:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))if(!neighbor.equals(pos))dissolveBlock(server,stack,wand,neighbor.immutable());}
 }
 /** Output count is conserved; vanilla invulnerability persists without a custom entity renderer/type. */
 private static List<ItemEntity> spawnCrystals(ServerPlayer player,Vec3 pos,AspectAmounts aspects,int multiplier){
  var spawned=new ArrayList<ItemEntity>();for(var entry:aspects.view().entrySet()){long count=(long)entry.getValue()*multiplier;if(count<=0)continue;while(count>0){int size=(int)Math.min(64,count);var item=new ItemEntity(player.level(),pos.x,pos.y,pos.z,TCItems.ESSENCE_CRYSTAL.get().of(entry.getKey(),size));item.setInvulnerable(true);item.setDefaultPickUpDelay();if(!player.level().addFreshEntity(item)){spawned.forEach(Entity::discard);return null;}spawned.add(item);count-=size;}}return spawned;
 }
 public boolean dissolveItem(ServerPlayer player,ItemStack stack,CastingWandItem wand,ItemEntity entity){
  var input=entity.getItem();var values=aspects(input);if(!entity.isAlive()||values.isEmpty()||!wand.consumeForPlayer(stack,player,costs(WandFocusItem.attached(stack)),false,false,false))return false;
  var snapshot=input.copy();var outputs=spawnCrystals(player,entity.position(),values,input.getCount());if(outputs==null)return false;
  if(!entity.isAlive()||!ItemStack.matches(snapshot,entity.getItem())||!wand.consumeForPlayer(stack,player,costs(WandFocusItem.attached(stack)),false,false,true)){outputs.forEach(Entity::discard);return false;}entity.discard();return true;
 }
 public boolean dissolveBlock(ServerPlayer player,ItemStack stack,CastingWandItem wand,BlockPos pos){
  var level=player.serverLevel();if(!player.mayBuild()||player.isSpectator()||!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,net.minecraft.core.Direction.UP,stack))return false;
  var state=level.getBlockState(pos);if(state.isAir()||state.hasBlockEntity()||state.getDestroySpeed(level,pos)<0)return false;var values=aspects(new ItemStack(state.getBlock()));if(values.isEmpty()||net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)==-1||!level.getBlockState(pos).equals(state)||!wand.consumeForPlayer(stack,player,costs(WandFocusItem.attached(stack)),false,false,false))return false;
  var outputs=spawnCrystals(player,pos.getCenter(),values,1);if(outputs==null)return false;if(!level.getBlockState(pos).equals(state)||!level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)){outputs.forEach(Entity::discard);return false;}
  if(!wand.consumeForPlayer(stack,player,costs(WandFocusItem.attached(stack)),false,false,true)){level.setBlock(pos,state,3);outputs.forEach(Entity::discard);return false;}level.levelEvent(2001,pos,Block.getId(state));return true;
 }
}
