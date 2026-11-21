package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.content.CastingWandItem;

public final class GatewayRuntime {
 private GatewayRuntime(){}
 public static Map<Aspect,Integer> costs(){var result=new LinkedHashMap<Aspect,Integer>();for(var a:thaumcraft.api.aspects.AspectSelection.select(TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.GATEWAY_COST))result.put(a,250);return result;}
 public static boolean loadedFrame(ServerLevel level,GatewayLinks.Link link){return GatewayLayout.cells().stream().allMatch(c->level.hasChunkAt(c.at(link.slot(),link.xAligned())));}
 public static boolean validOpenFrame(ServerLevel level,GatewayLinks.Link link){
  if(!(level.getBlockEntity(link.slot()) instanceof KeystoneSlotEntity slot)||!link.id().equals(slot.link()))return false;
  for(var cell:GatewayLayout.cells()){var at=cell.at(link.slot(),link.xAligned());if(cell.material().equals("minecraft:air")){if(!(level.getBlockEntity(at) instanceof GatewayPortalEntity portal)||!link.id().equals(portal.link()))return false;}else if(!level.getBlockState(at).is(ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(cell.material()))))return false;}return true;
 }
 public static boolean open(ServerLevel source,KeystoneSlotEntity slot,Player player,ItemStack stack){
  if(player.level()!=source||player.distanceToSqr(slot.getBlockPos().getCenter())>64||!player.mayBuild()||!(stack.getItem() instanceof CastingWandItem wand))return false;
  var plane=KeystoneItem.resolve(slot.key(),source).orElse(null);if(plane==null)return false;boolean xAligned=GatewayLayout.matches(source,slot.getBlockPos(),true);if(!xAligned&&!GatewayLayout.matches(source,slot.getBlockPos(),false))return false;
  for(var cell:GatewayLayout.cells()){var at=cell.at(slot.getBlockPos(),xAligned);if(!source.mayInteract(player,at)||!player.mayUseItemAt(at,Direction.UP,stack)||!source.getWorldBorder().isWithinBounds(at))return false;}
  if(!source.mayInteract(player,slot.getBlockPos())||!wand.consumeForPlayer(stack,player,costs(),true,true,false))return false;
  var target=source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));if(target==null)return false;
  var links=GatewayLinks.get(source.getServer());var link=links.reserve(UUID.randomUUID(),plane,source.dimension().location(),slot.getBlockPos(),xAligned).orElse(null);if(link==null)return false;
  var backups=new ArrayList<BlockSnapshot>();boolean success=false;
  try{
   for(var cell:GatewayLayout.cells())if(cell.material().equals("minecraft:air"))portal(source,cell.at(slot.getBlockPos(),xAligned),xAligned,link.id(),false,backups);
   var outward=GatewayLayout.outward(link.side());for(var at:GatewayLayout.mouth(plane,link.side())){
    target.getChunkAt(at);if(target.getBlockEntity(at)!=null||target.getBlockEntity(at.relative(outward))!=null)throw new IllegalStateException("Gateway destination occupied by block entity");
    portal(target,at,outward.getAxis()==Direction.Axis.Z,link.id(),true,backups);put(target,at.relative(outward),PlanarBlocks.VOID.get().defaultBlockState(),backups);
    for(int depth=1;depth<=3;depth++){var clear=at.relative(outward.getOpposite(),depth);target.getChunkAt(clear);if(target.getBlockEntity(clear)!=null)throw new IllegalStateException("Gateway landing contains block entity");put(target,clear,Blocks.AIR.defaultBlockState(),backups);if(at.getY()==126&&target.getBlockEntity(clear.below())!=null)throw new IllegalStateException("Gateway landing floor contains block entity");if(at.getY()==126)put(target,clear.below(),ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("thaumcraft:arcane_stone")).defaultBlockState(),backups);}
   }
   for(var snapshot:backups)if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,Direction.UP))return false;
   if(!wand.consumeForPlayer(stack,player,costs(),true,true,true))return false;slot.connected(link.id(),xAligned);source.playSound(null,slot.getBlockPos(),tc4revival.content.TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);success=true;return true;
  }catch(RuntimeException ex){com.mojang.logging.LogUtils.getLogger().warn("Gateway creation rolled back at {}",slot.getBlockPos(),ex);return false;}
  finally{if(!success){for(int i=backups.size()-1;i>=0;i--)backups.get(i).restore(true,false);links.release(link.id());}}
 }
 private static void put(ServerLevel level,BlockPos at,BlockState state,List<BlockSnapshot> snapshots){if(level.isOutsideBuildHeight(at)||!level.getWorldBorder().isWithinBounds(at))throw new IllegalStateException("Gateway outside world bounds");snapshots.add(BlockSnapshot.create(level.dimension(),level,at));if(!level.setBlock(at,state,3)&&!level.getBlockState(at).equals(state))throw new IllegalStateException("Gateway block write rejected");}
 private static void portal(ServerLevel level,BlockPos at,boolean xAligned,UUID id,boolean inside,List<BlockSnapshot> snapshots){put(level,at,GatewayBlocks.PORTAL.get().defaultBlockState().setValue(GatewayPortalBlock.AXIS,xAligned?Direction.Axis.X:Direction.Axis.Z),snapshots);if(!(level.getBlockEntity(at) instanceof GatewayPortalEntity portal))throw new IllegalStateException("Missing gateway portal entity");portal.bind(id,inside);}
 public static void close(MinecraftServer server,UUID id){var data=GatewayLinks.get(server);var link=data.get(id).orElse(null);if(link==null)return;data.release(id);var source=server.getLevel(ResourceKey.create(Registries.DIMENSION,link.dimension()));if(source!=null&&source.hasChunkAt(link.slot()))source.playSound(null,link.slot(),tc4revival.content.TCSounds.CRAFT_FAIL.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);if(source!=null)for(var cell:GatewayLayout.cells())remove(source,cell.at(link.slot(),link.xAligned()),id,false);
  var plane=PocketPlanes.get(server).get(link.plane()).filter(p->p.allocation().equals(link.allocation())).orElse(null);var target=server.getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));if(plane!=null&&target!=null)for(var at:GatewayLayout.mouth(plane,link.side()))remove(target,at,id,true);
 }
 private static void remove(ServerLevel level,BlockPos pos,UUID id,boolean inside){if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof GatewayPortalEntity portal&&id.equals(portal.link()))level.setBlockAndUpdate(pos,inside?PlanarBlocks.VOID.get().defaultBlockState():Blocks.AIR.defaultBlockState());}
 public static void travel(ServerLevel level,ServerPlayer player,UUID id,boolean inside){
  if(player.isPassenger()||player.isVehicle()||player.isSpectator())return;long now=player.server.overworld().getGameTime();var tag=player.getPersistentData();String cooldown="thaumichorizons:plane_cooldown";if(tag.getLong(cooldown)>now){tag.putLong(cooldown,now+10);return;}
  var link=GatewayLinks.get(player.server).get(id).orElse(null);if(link==null)return;var plane=PocketPlanes.get(player.server).get(link.plane()).filter(p->p.ready()&&p.allocation().equals(link.allocation())).orElse(null);if(plane==null)return;
  var source=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,link.dimension()));if(source==null)return;for(var cell:GatewayLayout.cells())source.getChunkAt(cell.at(link.slot(),link.xAligned()));if(!validOpenFrame(source,link)){close(player.server,id);return;}
  var destination=inside?source:player.server.getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));if(destination==null)return;var direction=GatewayLayout.outward(link.side());var desired=inside?link.slot().below(3):plane.center().relative(direction,plane.parameters().radius()-1).below(2);var safe=PlaneTravel.safePosition(destination,desired,player);if(safe.isEmpty())return;
  var at=safe.get();player.closeContainer();player.teleportTo(destination,at.x,at.y,at.z,inside?(link.xAligned()?0:270):direction.getOpposite().toYRot(),0);player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;tag.putLong(cooldown,now+10);
 }
}
