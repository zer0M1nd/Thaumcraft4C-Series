package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thmodern.Horizons;
public final class VortexEntity extends BlockEntity implements StabilizedRift {
 private int planeId=-1;private UUID allocation;private ItemStack pearlEscrow=ItemStack.EMPTY;
 private final StabilizerLeases leases=new StabilizerLeases();private RiftLifetime life=new RiftLifetime();private AspectAmounts aspects=new AspectAmounts();private boolean cheat,ateDevices;private int syncedBeams;private final List<ItemStack> outputs=new ArrayList<>();
 public VortexEntity(BlockPos pos,BlockState state){super(Horizons.VORTEX_ENTITY.get(),pos,state);}
 public int planeId(){return planeId;}public UUID allocation(){return allocation;}public boolean hasEscrow(){return !pearlEscrow.isEmpty();}
 public void bindPlane(PocketPlanes.Plane plane,boolean arrival){planeId=plane.id();allocation=plane.allocation();if(arrival){cheat=true;ateDevices=true;life=new RiftLifetime(50,false);}sync();}
 public void removedWithEscrow(){if(!(level instanceof ServerLevel server)||pearlEscrow.isEmpty()||allocation==null)return;var data=PocketPlanes.get(server.getServer());var plane=data.get(planeId).orElse(null);if(plane!=null&&plane.ready()){commitPearl();return;}PlaneRefunds.get(server.getServer()).enqueue(allocation,server.dimension().location(),worldPosition,pearlEscrow);if(plane!=null)data.progress(planeId,"cancelled",plane.cursor(),false);pearlEscrow=ItemStack.EMPTY;setChanged();}
 public void commitPearl(){pearlEscrow=ItemStack.EMPTY;sync();}
 public int beams(){return level==null?0:level.isClientSide?syncedBeams:leases.count(level.getGameTime());}public int age(){return life.count();}public boolean collapsing(){return life.collapsing();}public boolean cheat(){return cheat;}public AspectAmounts aspects(){return aspects.copy();}public int pendingOutputs(){return outputs.size();}
 public void initialize(AspectAmounts aspects,boolean cheat){this.aspects=aspects.copy();this.cheat=cheat;sync();}
 @Override public void renewStabilizer(String source,long tick){leases.renew(source,tick);}
 @Override public void removeStabilizer(String source){leases.remove(source);}
 private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
 public void tick(){if(level!=null&&level.isClientSide){life.tick(syncedBeams,cheat||hasEscrow());return;}if(!(level instanceof ServerLevel server))return;int beams=beams();boolean active=life.ready();boolean priorCollapse=life.collapsing();if(life.tick(beams,cheat||hasEscrow())){EnergizedNodeBlock.collapse(server,worldPosition);return;}if(beams!=syncedBeams||priorCollapse!=life.collapsing()||life.count()%20==0){syncedBeams=beams;sync();}setChanged();if(!active||life.collapsing())return;
  if(!ateDevices){ateDevices=true;if(!cheat)for(var pos:BlockPos.betweenClosed(worldPosition.offset(-2,-2,-2),worldPosition.offset(2,2,2))){if(pos.equals(worldPosition)||!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);if(!state.isAir()&&state.getDestroySpeed(level,pos)>=0)level.destroyBlock(pos,true);}sync();}
  if(planeId>=0){var plane=PocketPlanes.get(server.getServer()).get(planeId).orElse(null);if(plane!=null&&plane.ready()){if(hasEscrow())commitPearl();PlaneTravel.touch(this,plane,server);}if(hasEscrow())return;if(beams<6&&!cheat)hungry(server,beams);return;}
  if(beams<6&&!cheat)hungry(server,beams);else for(var item:server.getEntitiesOfClass(ItemEntity.class,new AABB(worldPosition).inflate(1)))convert(item);
 }
 public boolean convert(ItemEntity item){if(!(level instanceof ServerLevel server)||!item.isAlive()||life.collapsing()||!life.ready()||!cheat&&beams()<6)return false;var input=item.getItem();if(planeId>=0)return false;
  if(input.is(Horizons.CRYSTAL_WAND.get())){for(int i=0;i<input.getCount();i++)outputs.add(Horizons.DISPOSABLE_WAND.get().full());item.discard();sync();return true;}
  if(input.is(TCItems.PRIMORDIAL_PEARL.get()))return beginPlane(server,item);
  if(input.is(TCItems.material(MaterialType.VOID))){int count=input.getCount();while(count>0){int part=Math.min(16,count);outputs.add(new ItemStack(Horizons.VOID_PUTTY.get(),part));count-=part;}item.discard();sync();return true;}
  if(input.is(TCItems.material(MaterialType.SALIS_MUNDUS))){boolean changed=false;while(!input.isEmpty()){var spawned=new ArrayList<Entity>();boolean accepted=true;int count=server.random.nextInt(4)+1;for(int i=0;i<count;i++){var wisp=TCEntities.WISP.get().create(server);wisp.moveTo(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5);var choices=aspects.aspects().isEmpty()?TCAspects.catalog().primals():aspects.aspects();wisp.setAspectTag(choices.get(server.random.nextInt(choices.size())).tag());if(!server.addFreshEntity(wisp)){accepted=false;break;}spawned.add(wisp);}if(!accepted){spawned.forEach(Entity::discard);break;}input.shrink(1);changed=true;}if(input.isEmpty())item.discard();else item.setItem(input);return changed;}
  if(input.is(Horizons.GOLEM_POWDER.get())){var saved=new CompoundTag();item.addAdditionalSaveData(saved);var owner=saved.hasUUID("Thrower")?server.getServer().getPlayerList().getPlayer(saved.getUUID("Thrower")):null;if(owner==null||owner.level()!=level)return false;boolean changed=false;while(!input.isEmpty()){if(thmodern.golem.AnimatedGolems.summonVoid(owner,worldPosition.getCenter())==null)break;input.shrink(1);changed=true;}if(input.isEmpty())item.discard();else item.setItem(input);return changed;}
  // Unrecognized/deferred inputs remain intact.
  return false;
 }
 private boolean beginPlane(ServerLevel server,ItemEntity item){
  if(!thmodern.LegacyConfig.pocket())return false;
  if(server.dimension().location().equals(PocketPlanes.DIMENSION)||server.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,PocketPlanes.DIMENSION))==null)return false;
  var input=item.getItem();var amounts=new LinkedHashMap<String,Integer>();for(var aspect:aspects.aspects())amounts.put(aspect.tag(),aspects.amount(aspect));
  UUID request=UUID.randomUUID();var params=new PlaneParameters(amounts);var plan=PocketPlanes.get(server.getServer()).reserve(request,input.hasCustomHoverName()?input.getHoverName().getString():"Pocket Plane",amounts,params.color(key->TCAspects.catalog().require(key).color()),server.random.nextLong(),new PocketPlanes.Origin(server.dimension().location(),worldPosition));
  pearlEscrow=input.copyWithCount(1);bindPlane(plan,false);input.shrink(1);if(input.isEmpty())item.discard();else item.setItem(input);sync();return true;
 }
 public boolean extract(ServerPlayer player){if(level!=player.level()||player.distanceToSqr(worldPosition.getCenter())>64)return false;if(hasEscrow()&&level instanceof ServerLevel server){var data=PocketPlanes.get(server.getServer());var plane=data.get(planeId).orElse(null);if(plane!=null&&plane.stage().startsWith("failed/")){var refund=new ItemEntity(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,pearlEscrow.copy());refund.setDefaultPickUpDelay();if(!level.addFreshEntity(refund))return false;data.progress(planeId,"cancelled",plane.cursor(),false);pearlEscrow=ItemStack.EMPTY;planeId=-1;allocation=null;sync();return true;}}if(outputs.isEmpty())return false;var item=new ItemEntity(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,outputs.get(0).copy());item.setDefaultPickUpDelay();if(!level.addFreshEntity(item))return false;outputs.remove(0);sync();return true;}
 private void hungry(ServerLevel server,int beams){var center=worldPosition.getCenter();for(var entity:server.getEntities(null,new AABB(worldPosition).inflate(15))){if(entity instanceof net.minecraft.world.entity.player.Player player&&(player.getAbilities().invulnerable||player.isSpectator()))continue;var delta=center.subtract(entity.position());double square=delta.lengthSqr();if(square<2){if(entity instanceof FallingBlockEntity)entity.discard();else entity.hurt(server.damageSources().fellOutOfWorld(),3-beams/2f);}double distance=Math.sqrt(square);if(distance<1e-6||distance>=15)continue;double falloff=1-distance/15;double force=falloff*falloff*(2-beams/3d);entity.setDeltaMovement(entity.getDeltaMovement().add(delta.x/distance*force*.15,delta.y/distance*force*.25,delta.z/distance*force*.15));entity.hurtMarked=true;}
  for(int attempt=0;attempt<3;attempt++){if(beams>0&&server.random.nextInt(beams)!=0)continue;var target=worldPosition.offset(server.random.nextInt(16)-server.random.nextInt(16),server.random.nextInt(16)-server.random.nextInt(16),server.random.nextInt(16)-server.random.nextInt(16));if(!server.hasChunkAt(target))continue;target=new BlockPos(target.getX(),Math.min(target.getY(),server.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,target.getX(),target.getZ())),target.getZ());var context=new ClipContext(center,target.getCenter(),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,(Entity)null){@Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(BlockState state,BlockGetter world,BlockPos pos){return pos.equals(worldPosition)?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,pos);}};var hit=server.clip(context);if(hit.getType()!=HitResult.Type.BLOCK||hit.getBlockPos().distSqr(worldPosition)>=256)continue;var pos=hit.getBlockPos();var state=server.getBlockState(pos);float hardness=state.getDestroySpeed(server,pos);if(hardness>=0&&hardness<10)server.destroyBlock(pos,true);}
 }
 @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("PlaneId",planeId);if(allocation!=null)tag.putUUID("Allocation",allocation);if(!pearlEscrow.isEmpty())tag.put("PearlEscrow",pearlEscrow.save(new CompoundTag()));tag.putInt("Count",life.count());tag.putBoolean("Collapsing",life.collapsing());tag.putBoolean("Cheat",cheat);tag.putBoolean("AteDevices",ateDevices);AspectNbt.write(tag,aspects);var list=new ListTag();for(var output:outputs)list.add(output.save(new CompoundTag()));tag.put("Outputs",list);}
 @Override public void load(CompoundTag tag){super.load(tag);planeId=tag.contains("PlaneId")?tag.getInt("PlaneId"):-1;allocation=tag.hasUUID("Allocation")?tag.getUUID("Allocation"):null;pearlEscrow=tag.contains("PearlEscrow")?ItemStack.of(tag.getCompound("PearlEscrow")):ItemStack.EMPTY;life=new RiftLifetime(tag.getInt("Count"),tag.getBoolean("Collapsing"));cheat=tag.getBoolean("Cheat");ateDevices=tag.getBoolean("AteDevices");aspects=AspectNbt.read(tag,TCAspects.catalog()).amounts();outputs.clear();var list=tag.getList("Outputs",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var stack=ItemStack.of(list.getCompound(i));if(!stack.isEmpty())outputs.add(stack);}syncedBeams=tag.getInt("Beams");}
 @Override public CompoundTag getUpdateTag(){var tag=saveWithoutMetadata();tag.putInt("Beams",syncedBeams);return tag;}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
