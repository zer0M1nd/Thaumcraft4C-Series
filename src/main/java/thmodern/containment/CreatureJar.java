package thmodern.containment;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thmodern.Horizons;

/** Capturing moves a real creature and its possessions; unlike cloning, it must not erase its equipment. */
public final class CreatureJar {
    public static boolean eligible(LivingEntity entity){return entity instanceof Mob&&entity.isAlive()&&!entity.isRemoved()&&!entity.isPassenger()&&!entity.isVehicle()
        &&!(entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)&&!(entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
        &&!(entity instanceof tc4revival.content.GolemEntity)&&!entity.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.ResourceLocation.parse("forge:bosses")));}
    public static boolean empty(ItemStack stack){return stack.is(tc4revival.content.TCItems.ESSENTIA_JAR.get())&&((tc4revival.content.StoredAspectItem)stack.getItem()).storedAspects(stack).isEmpty();}
    public static int jarSlot(ServerPlayer player){for(int i=0;i<player.getInventory().items.size();i++)if(empty(player.getInventory().getItem(i)))return i;return -1;}
    public static boolean room(ServerPlayer player){int slot=jarSlot(player);return slot>=0&&(player.getInventory().getItem(slot).getCount()==1||player.getInventory().getFreeSlot()>=0);}
    public static ItemStack bottle(LivingEntity entity){
        if(!eligible(entity))return ItemStack.EMPTY;
        if(entity instanceof SoulEntity)return SoulJar.create(null);
        var tag=new CompoundTag();if(!entity.save(tag)||tag.sizeInBytes()>1048576)return ItemStack.EMPTY;
        tag.remove("Leash");tag.remove("Passengers");
        var stack=new ItemStack(Horizons.CREATURE_JAR_ITEM.get());stack.getOrCreateTag().put("Captured",tag);stack.getOrCreateTag().putString("CreatureName",entity.getName().getString());return stack;
    }
    public static LivingEntity read(ItemStack stack,Level level){return stack.hasTag()&&!stack.getTag().contains("VatRecovery")?readContents(stack.getTag(),level):null;}
    public static LivingEntity readContents(CompoundTag contents,Level level){return level!=null&&SoulJar.contains(contents)?Horizons.SOUL.get().create(level):read(contents.getCompound("Captured"),level);}
    public static LivingEntity read(CompoundTag data,Level level){
        if(level==null||data.isEmpty()||data.sizeInBytes()>1048576||data.contains("Passengers"))return null;
        var entity=EntityType.create(data.copy(),level).orElse(null);return entity instanceof LivingEntity living&&eligible(living)?living:null;
    }
    /** No drop fallback: a failed inventory exchange must leave the original subject untouched. */
    public static boolean exchange(ServerPlayer player,ItemStack input,ItemStack output){
        if(input.isEmpty()||output.isEmpty())return false;
        int free=player.getInventory().getFreeSlot(),source=-1;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i)==input){source=i;break;}
        if(source<0||input.getCount()>1&&free<0)return false;
        if(input.getCount()==1)player.getInventory().setItem(source,output.copy());
        else{input.shrink(1);player.getInventory().setItem(free,output.copy());}
        player.getInventory().setChanged();player.containerMenu.broadcastChanges();return true;
    }
    public static boolean capture(ServerPlayer player,LivingEntity target){
        if(!eligible(target)||target.level()!=player.level()||!player.mayBuild()||!player.level().mayInteract(player,target.blockPosition()))return false;
        var output=bottle(target);int slot=jarSlot(player);if(output.isEmpty()||slot<0||!exchange(player,player.getInventory().getItem(slot),output))return false;
        if(target instanceof Mob mob&&mob.isLeashed())mob.dropLeash(true,true);
        target.discard();return true;
    }
}
