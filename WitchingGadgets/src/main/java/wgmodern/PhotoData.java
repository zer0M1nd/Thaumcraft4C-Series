package wgmodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.*;
import tc4revival.research.*;

/** A photograph remembers a target, never a client-supplied scan reward. */
public class PhotoData {
 public static CompoundTag of(ItemStack stack){return stack.hasTag()?stack.getTag().getCompound("scanResult"):new CompoundTag();}
 public static ItemStack create(boolean developed,CompoundTag scan){var stack=new ItemStack(developed?Content.PHOTO.get():Content.PLATE.get());stack.getOrCreateTag().put("scanResult",scan.copy());return stack;}
 public static boolean valid(CompoundTag data){return !data.getString("identity").isEmpty()&&Set.of("OBJECT","ENTITY","PHENOMENON").contains(data.getString("kind"));}
 public static CompoundTag capture(ServerPlayer player,ScanTargetSelector.Selection selection){
  var tag=summary(selection.target(),selection.name());var continuity=selection.target().continuity();
  if(continuity instanceof ScanTarget.Entity e){var entity=player.serverLevel().getEntity(e.uuid());if(entity==null)return new CompoundTag();captureEntity(tag,entity);}
  else {var pos=BlockPos.containing(selection.runeOrigin());tag.putLong("position",pos.asLong());tag.putString("dimension",player.level().dimension().location().toString());if(selection.node().isEmpty()){var state=tc4revival.content.WardedBlock.queryOriginal(player.serverLevel(),pos);var picked=state.getCloneItemStack(new net.minecraft.world.phys.BlockHitResult(selection.runeOrigin(),net.minecraft.core.Direction.UP,pos,false),player.level(),pos,player);if(!picked.isEmpty())tag.put("item",picked.copyWithCount(1).save(new CompoundTag()));}}
  // A photo of a container/photo must not produce exponentially growing item packets.
  return tag.sizeInBytes()<=131072?tag:new CompoundTag();
 }
 public static CompoundTag entity(Entity entity){var scan=ScanTargetResolver.entity(entity);if(scan.isEmpty())return new CompoundTag();var tag=summary(scan.get(),entity.getName());captureEntity(tag,entity);return tag;}
 /** Generation reads only the immutable entity-aspect catalogue and an unattached local entity. */
 public static CompoundTag worldgenEntity(Entity entity){var resolved=tc4revival.aspect.entity.EntityAspects.resolve(entity);var target=new ScanTarget(new ScanTarget.Entity(entity.getUUID()),PlayerKnowledge.ScanKind.ENTITY,Optional.of(resolved.map(r->Integer.toString(r.scanHash())).orElse("modern:entity:"+net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))),resolved.map(r->r.aspects()).orElseGet(AspectAmounts::new));var data=summary(target,entity.getName());captureEntity(data,entity);return data;}
 private static void captureEntity(CompoundTag tag,Entity entity){
  if(entity instanceof ItemEntity dropped){tag.put("item",dropped.getItem().copyWithCount(1).save(new CompoundTag()));return;}
  var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());tag.putString("entityType",type.toString());var nbt=new CompoundTag();entity.saveWithoutId(nbt);nbt.remove("Passengers");nbt.remove("Inventory");nbt.remove("EnderItems");tag.put("entity",nbt);
 }
 public static CompoundTag summary(ScanTarget scan,Component name){var tag=new CompoundTag();scan.identity().ifPresent(id->tag.putString("identity",id));tag.putString("kind",scan.kind().name());tag.putString("name",Component.Serializer.toJson(name));var amounts=new CompoundTag();scan.aspects().view().forEach((aspect,count)->amounts.putInt(aspect.tag(),count));tag.put("aspects",amounts);return tag;}
 public static Component name(CompoundTag data){try{var c=Component.Serializer.fromJson(data.getString("name"));return c==null?Component.literal("?"):c;}catch(RuntimeException ignored){return Component.literal("?");}}
 public static AspectAmounts aspects(CompoundTag data){var result=new AspectAmounts();var values=data.getCompound("aspects");for(String key:values.getAllKeys())if(values.getInt(key)>0)TCAspects.catalog().find(key).ifPresent(a->result.add(a,Math.min(32767,values.getInt(key))));return result;}
 public static ItemStack item(CompoundTag data){return data.contains("item",10)?ItemStack.of(data.getCompound("item")):ItemStack.EMPTY;}
 public static Entity entity(CompoundTag data,Level level){if(!data.contains("entityType"))return null;var id=ResourceLocation.tryParse(data.getString("entityType"));if(id==null)return null;var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(id);if(type==null||type==EntityType.PLAYER)return null;var result=type.create(level);if(result!=null)result.load(data.getCompound("entity"));return result;}
 public static Optional<ScanTarget> resolve(CompoundTag data,ServerLevel level){
  if(!valid(data))return Optional.empty();var item=item(data);if(!item.isEmpty())return ScanTargetResolver.inventory(item);
  var entity=entity(data,level);if(entity!=null)return ScanTargetResolver.entity(entity);
  if(data.getString("kind").equals("PHENOMENON")&&data.getString("dimension").equals(level.dimension().location().toString())){var pos=BlockPos.of(data.getLong("position"));if(!level.hasChunkAt(pos))return Optional.empty();return ScanTargetResolver.block(level,pos).filter(s->s.identity().orElse("").equals(data.getString("identity")));}
  // Player entities cannot be instantiated; their original observation remains an entity scan.
  if(data.getString("entityType").equals("minecraft:player"))return Optional.of(new ScanTarget(new ScanTarget.Phenomenon("photo"),PlayerKnowledge.ScanKind.ENTITY,Optional.of(data.getString("identity")),aspects(data)));
  return Optional.empty();
 }
 public static boolean canPhotograph(PlayerKnowledge knowledge,AspectAmounts amounts){if(amounts.isEmpty())return false;for(var a:amounts.aspects())for(String parent:a.components())if(!knowledge.knows(TCAspects.catalog().require(parent)))return false;return true;}
 public static boolean scanned(CompoundTag tag,PlayerKnowledge knowledge){if(!valid(tag))return false;return knowledge.scanned(PlayerKnowledge.ScanKind.valueOf(tag.getString("kind")),tag.getString("identity"));}
}
