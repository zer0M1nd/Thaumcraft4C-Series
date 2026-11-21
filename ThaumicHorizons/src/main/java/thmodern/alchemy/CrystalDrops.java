package thmodern.alchemy;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.TCItems;
public final class CrystalDrops {
 private CrystalDrops(){}
 public static List<ItemEntity> spawn(ServerLevel level,Vec3 pos,AspectAmounts aspects,int multiplier){
  var spawned=new ArrayList<ItemEntity>();for(var entry:aspects.view().entrySet()){long count=(long)entry.getValue()*multiplier;while(count>0){int size=(int)Math.min(64,count);var item=new ItemEntity(level,pos.x,pos.y,pos.z,TCItems.ESSENCE_CRYSTAL.get().of(entry.getKey(),size));item.setInvulnerable(true);item.setDefaultPickUpDelay();if(!level.addFreshEntity(item)){spawned.forEach(Entity::discard);return null;}spawned.add(item);count-=size;}}return spawned;
 }
}
