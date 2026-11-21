package automagy.content;
import automagy.logic.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.visnet.VisNetHandler;
public final class NethermindBlockEntity extends InventoryBlockEntity implements NethermindHost {
 private final NethermindCycle cycle=new NethermindCycle();private NethermindStructure.Result structure;private BlockPos activeRune;private int validation=60,instability;
 private boolean conjuring;private int visualLureDepth;
 public NethermindBlockEntity(BlockPos pos,BlockState state){super(Content.NETHERMIND_TILE.get(),pos,state,0);}
 public int age(){return cycle.age();}public boolean enabled(){return getBlockState().getValue(NethermindBlock.ACTIVE);}
 @Override public boolean conjuring(){return enabled()&&conjuring;}
 public int visualLureDepth(){return visualLureDepth;}
 @Override public void requireValidation(){validation=60;}
 private NethermindStructure.Result scan(){return NethermindStructure.scan(new NethermindStructure.World(){
  private BlockPos at(int x,int y,int z){return worldPosition.offset(x,y,z);}
  public boolean loaded(int x,int y,int z){var p=at(x,y,z);return !level.isOutsideBuildHeight(p)&&level.hasChunkAt(p);}
  public NethermindStructure.Rune rune(int x,int y,int z){var state=level.getBlockState(at(x,y,z));var block=state.getBlock();if(block instanceof RunedObsidianBlock)return new NethermindStructure.Rune("basic",true,false,false,6);return block instanceof automagy.api.NetherRune rune?rune.netherRune(state).structure():null;}
  public int lure(int x,int y,int z){return level.getBlockEntity(at(x,y,z)) instanceof DimensionLureBlockEntity lure?lure.enabled()?2:1:0;}
  public boolean obstructs(int x,int y,int z){var p=at(x,y,z);var s=level.getBlockState(p);return !s.isAir()&&!s.is(TCArcane.FLUX_GOO.get())&&!s.is(TCArcane.FLUX_GAS.get())&&!s.getCollisionShape(level,p).isEmpty();}
 });}
 private DimensionLureBlockEntity lure(){if(level==null||structure==null||structure.lureDepth()==0)return null;var p=worldPosition.below(structure.lureDepth());return level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof DimensionLureBlockEntity lure?lure:null;}
 public boolean enable(ServerPlayer player,net.minecraft.world.item.ItemStack wand){if(level==null||enabled()||worldPosition.getY()<10)return false;var candidate=scan();if(!candidate.valid())return false;var cost=new AspectList().add(Aspect.FIRE,75).add(Aspect.ORDER,25);if(!player.getAbilities().instabuild&&!thaumcraft.api.ThaumcraftApiHelper.consumeVisFromWandCrafting(wand,player,cost,true))return false;structure=candidate;validation=0;cycle.clear();level.setBlockAndUpdate(worldPosition,getBlockState().setValue(NethermindBlock.ACTIVE,true));sync();level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);return true;}
 private void runeLight(boolean active){if(activeRune==null||level==null||!level.hasChunkAt(activeRune))return;var state=level.getBlockState(activeRune);if(state.getBlock() instanceof automagy.api.NetherRune rune)rune.setNetherRuneActive(level,activeRune,state,active);}
 public void cleanup(){conjuring=false;visualLureDepth=0;runeLight(false);var lure=lure();if(lure!=null)lure.clearActive();cycle.clear();activeRune=null;}
 public void disable(){if(level==null||level.isClientSide)return;cleanup();level.setBlockAndUpdate(worldPosition,getBlockState().setValue(NethermindBlock.ACTIVE,false));sync();}
 private NetherRuneCatalog.Definition selected(){
  if(cycle.rune().equals("basic_rock"))return automagy.NetherConfig.apply(NetherRuneCatalog.basic(false));
  if(cycle.rune().equals("basic_lava"))return automagy.NetherConfig.apply(NetherRuneCatalog.basic(true));
  if(activeRune==null||!level.hasChunkAt(activeRune))return null;
  var state=level.getBlockState(activeRune);
  if(!(state.getBlock() instanceof automagy.api.NetherRune rune))return null;
  var definition=rune.netherRune(state);
  return definition.key().equals(cycle.rune())?automagy.NetherConfig.apply(definition):null;
 }
 private void select(){runeLight(false);activeRune=null;NetherRuneCatalog.Definition definition;String key;
  if(NethermindStructure.chooseBasic(structure.selected().size(),level.random.nextInt(4))){boolean lava=level.random.nextInt(100)<automagy.NetherConfig.basicLavaChance();definition=NetherRuneCatalog.basic(lava);key=lava?"basic_lava":"basic_rock";}
  else {var offset=structure.selected().get(level.random.nextInt(structure.selected().size()));activeRune=worldPosition.offset(offset.x(),offset.y(),offset.z());if(!level.hasChunkAt(activeRune)||!(level.getBlockState(activeRune).getBlock() instanceof automagy.api.NetherRune rune)){requireValidation();return;}definition=rune.netherRune(level.getBlockState(activeRune));key=definition.key();}
  definition=automagy.NetherConfig.apply(definition);
  cycle.select(key,key.equals("blaze")&&level.random.nextInt(10)==0,NethermindStructure.duration(definition.duration(),structure.speedRunes()),definition.cost());if(definition.creature()){var lure=lure();if(lure==null){requireValidation();cycle.complete();return;}lure.select(definition);}runeLight(true);sync();
 }
 public BlockPos emptyPosition(boolean small){int radius=small?2:4,minY=Math.max(level.getMinBuildHeight()+1,worldPosition.getY()-(small?6:11)),maxY=Math.max(level.getMinBuildHeight()+1,worldPosition.getY()-3);for(int i=0;i<33;i++){var p=new BlockPos(worldPosition.getX()+level.random.nextInt(radius*2+1)-radius,minY+level.random.nextInt(maxY-minY+1),worldPosition.getZ()+level.random.nextInt(radius*2+1)-radius);if(level.hasChunkAt(p)&&level.isEmptyBlock(p))return p;}for(int x=-radius;x<=radius;x++)for(int y=minY;y<=maxY;y++)for(int z=-radius;z<=radius;z++){var p=new BlockPos(worldPosition.getX()+x,y,worldPosition.getZ()+z);if(level.hasChunkAt(p)&&level.isEmptyBlock(p))return p;}return null;}
 public static void tick(Level level,BlockPos pos,BlockState state,NethermindBlockEntity tile){
  if(level.isClientSide)return;
  boolean previous=tile.conjuring;int previousDepth=tile.visualLureDepth;tile.conjuring=false;
  try{tickServer(level,pos,state,tile);}finally{
   tile.visualLureDepth=tile.enabled()&&tile.structure!=null?tile.structure.lureDepth():0;
   if(previous!=tile.conjuring||previousDepth!=tile.visualLureDepth)tile.sync();
  }
 }
 private static void tickServer(Level level,BlockPos pos,BlockState state,NethermindBlockEntity tile){if(!tile.enabled())return;
  if(tile.structure==null||tile.validation++>=60){var candidate=tile.scan();if(candidate.unloaded())return;if(!candidate.valid()){tile.disable();return;}tile.structure=candidate;tile.validation=0;}
  for(var offset:NethermindStructure.RUNES)if(!level.hasChunkAt(pos.offset(offset.x(),offset.y(),offset.z())))return;
  var definition=tile.selected();boolean creature=tile.structure.creatureRunes()>0;var lure=tile.lure();if(creature&&(lure==null||!lure.enabled())){tile.requireValidation();return;}
  if(tile.activeRune!=null&&!NethermindStructure.RUNES.stream().anyMatch(o->pos.offset(o.x(),o.y(),o.z()).equals(tile.activeRune))){tile.disable();return;}
  boolean powered=tile.cycle.powerTick(creature,amount->VisNetHandler.drainVis(level,pos.above(),Aspect.FIRE,amount));tile.setChanged();if(!powered)return;
  if(tile.instability>0&&tile.cycle.age()%20==0&&level.random.nextBoolean()&&level.random.nextInt(5000)<tile.instability)tile.discharge(false);
  if(!tile.cycle.selected()){tile.select();return;}if(definition==null||definition.creature()!=creature){tile.disable();return;}
  if(tile.activeRune!=null&&(!(level.getBlockState(tile.activeRune).getBlock() instanceof automagy.api.NetherRune rune)||!rune.netherRune(level.getBlockState(tile.activeRune)).key().equals(definition.key()))){tile.disable();return;}
  if(!tile.cycle.needed().isEmpty()||creature&&!lure.ready()){if(tile.cycle.acquisitionTick(tile.structure.speedRunes())){if(creature)lure.acquire();tile.cycle.acquire(key->tc4revival.aspect.TCAspects.catalog().find(key).map(a->EssentiaSources.drain(tile,a,null,12,false).isPresent()).orElse(false));tile.sync();}return;}
  tile.conjuring=true;
  if(!tile.cycle.advance())return;
  var runeState=tile.activeRune==null?null:level.getBlockState(tile.activeRune);
  // Builtins retain their legacy randomized variants; external blocks supply their own factories.
  var extension=runeState!=null&&runeState.getBlock() instanceof automagy.api.NetherRune custom&&!(runeState.getBlock() instanceof NetherRuneBlock)?custom:null;
  if(creature){int count=extension==null?tile.cycle.doubleBlaze()?2:1:Math.max(1,Math.min(64,extension.creatureCount(runeState)));
   if(tile.cycle.checkInstabilityOnce()){int extra=definition.instability()+(tile.cycle.doubleBlaze()?200:0);tile.instability=NethermindStructure.instability(tile.instability,tile.structure.speedRunes(),lure.stability(),extra,level.random.nextInt(10));if(tile.instability>0&&level.random.nextInt(500)<tile.instability&&tile.discharge(true))count--;}
   int delay=NetherSummoning.spawn((ServerLevel)level,pos,tile.structure.lureDepth(),definition,count,extension,runeState);if(delay>0){tile.cycle.retry(delay);return;}lure.consumeBait();NetherBiomeSpread.apply((ServerLevel)level,pos,true);tile.cycle.complete();tile.select();return;
  }
  var target=tile.emptyPosition(false);BlockState output=null;
  if(target!=null){
   if(extension!=null)output=extension.conjuredBlock(runeState,(ServerLevel)level,target);
   else{var id=net.minecraft.resources.ResourceLocation.tryParse(definition.output());if(id!=null&&net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(id))output=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();}
  }
  if(output==null||output.isAir()||target==null||!level.isEmptyBlock(target)||!level.setBlockAndUpdate(target,output)){tile.cycle.retry(60);return;}
  if(level instanceof ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,target.getX()+.5,target.getY()+.5,target.getZ()+.5,12,.4,.4,.4,.01);NetherBiomeSpread.apply((ServerLevel)level,pos,false);tile.cycle.complete();tile.select();
 }
 private boolean discharge(boolean spawning){if(instability<=0)return false;boolean prevent=false;int amount;
  if(spawning&&instability>=350){if(level.random.nextInt(instability)>175){amount=350;var lure=lure();if(lure!=null)amount=Math.min(instability-1,amount+lure.stability());prevent=true;var p=emptyPosition(true);if(p!=null)level.setBlockAndUpdate(p,(level.random.nextInt(3)==0?TCArcane.FLUX_GOO.get():TCArcane.FLUX_GAS.get()).defaultBlockState());}else amount=1;}
  else amount=level.random.nextInt(Math.min(instability,10))+1;
  instability=Math.max(0,instability-amount);setChanged();if(!prevent)instabilityEffect(false);return prevent;
 }
 private void instabilityEffect(boolean lowCap){int r=level.random.nextInt(21);if(r<10){spark();return;}if(r<17){var lure=lure();int bottom=lure==null?worldPosition.getY():lure.getBlockPos().getY();var box=new net.minecraft.world.phys.AABB(worldPosition.getX(),bottom,worldPosition.getZ(),worldPosition.getX()+1,worldPosition.getY()+1,worldPosition.getZ()+1).inflate(10);var players=level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,box);if(!players.isEmpty()){if(r<15)players=java.util.List.of(players.get(level.random.nextInt(players.size())));for(var player:players){player.hurt(level.damageSources().magic(),4+level.random.nextInt(4));if(level instanceof ServerLevel server)tc4revival.network.FocusEffectPacket.lightning(level,worldPosition.getCenter(),player.getEyePosition());}return;}if(r<15){spark();return;}}
  if(lowCap||r<19){var lure=lure();double middle=lure==null?worldPosition.getY():(worldPosition.getY()+lure.getBlockPos().getY()+1)/2.0;level.explode(null,worldPosition.getX()+.5+level.random.nextInt(4)-level.random.nextInt(4),(int)middle+level.random.nextInt(3)-level.random.nextInt(3),worldPosition.getZ()+.5+level.random.nextInt(4)-level.random.nextInt(4),1,Level.ExplosionInteraction.NONE);return;}
  if(r==19){var lure=lure();if(instability<100||level.random.nextInt(4)==0){instabilityEffect(true);return;}if(lure!=null&&lure.stability()<20&&NetherSummoning.spawn((ServerLevel)level,worldPosition,structure.lureDepth(),NetherRuneCatalog.BUILTINS.get("wisp"),1)!=0)instabilityEffect(true);return;}
  instabilityEffect(true);instabilityEffect(true);
 }
 private void spark(){if(level instanceof ServerLevel server)tc4revival.network.FocusEffectPacket.lightning(level,worldPosition.getCenter(),worldPosition.getCenter().add(level.random.nextDouble()*6-3,level.random.nextDouble()*6-3,level.random.nextDouble()*6-3));}
 @Override public CompoundTag getUpdateTag(){var tag=super.getUpdateTag();tag.putBoolean("Conjuring",conjuring);tag.putInt("VisualLureDepth",visualLureDepth);return tag;}
 @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Cycle",NetherProgressData.cycle(cycle));tag.putInt("Instability",instability);if(activeRune!=null)tag.putLong("ActiveRune",activeRune.asLong());}
 @Override public void load(CompoundTag tag){super.load(tag);NetherProgressData.cycle(cycle,tag.getCompound("Cycle"));instability=Math.max(0,Math.min(1000,tag.getInt("Instability")));activeRune=tag.contains("ActiveRune")?BlockPos.of(tag.getLong("ActiveRune")):null;structure=null;validation=60;conjuring=tag.getBoolean("Conjuring");visualLureDepth=Math.max(0,Math.min(10,tag.getInt("VisualLureDepth")));}
}
