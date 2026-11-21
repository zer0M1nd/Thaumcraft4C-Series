package infusionbrewing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class SpawnerUpgradeItem extends Item {
    private final int code;
    public SpawnerUpgradeItem(int code){super(new Properties().stacksTo(1));this.code=code;}
    public static boolean upgrade(CompoundTag tag,int code){
        boolean power=code<4;int before=power?BrewingRules.powerTier(tag.getShort("MaxNearbyEntities"),tag.getShort("SpawnCount")):BrewingRules.speedTier(tag.getShort("MinSpawnDelay"),tag.getShort("MaxSpawnDelay"));
        if(before!=code%4)return false;
        int after=before+1;
        if(power){tag.putShort("MaxNearbyEntities",(short)BrewingRules.nearby(after));tag.putShort("SpawnCount",(short)BrewingRules.spawnCount(after));}
        else {tag.putShort("MinSpawnDelay",(short)BrewingRules.minDelay(after));tag.putShort("MaxSpawnDelay",(short)BrewingRules.maxDelay(after));}
        return true;
    }
    @Override public InteractionResult useOn(UseOnContext c){
        var level=c.getLevel();var player=c.getPlayer();var pos=c.getClickedPos();
        if(player==null||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,c.getClickedFace(),c.getItemInHand())||!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner))return InteractionResult.PASS;
        var tag=spawner.saveWithoutMetadata();if(!upgrade(tag,code))return InteractionResult.FAIL;
        if(!level.isClientSide){
            spawner.load(tag);spawner.setChanged();level.sendBlockUpdated(pos,spawner.getBlockState(),spawner.getBlockState(),3);
            if(!player.getAbilities().instabuild)c.getItemInHand().shrink(1);
            var lightning=EntityType.LIGHTNING_BOLT.create(level);if(lightning!=null){lightning.moveTo(pos.getX(),pos.getY()+1,pos.getZ());level.addFreshEntity(lightning);}
            player.displayClientMessage(Component.translatable("infusionbrewing.spawner.upgraded",code<4?Component.translatable("infusionbrewing.power"):Component.translatable("infusionbrewing.speed"),code%4+1),true);
        }return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public static void inspect(PlayerInteractEvent.RightClickBlock e){
        if(e.getLevel().isClientSide||!(e.getItemStack().getItem() instanceof tc4revival.content.CastingWandItem)||!(e.getLevel().getBlockEntity(e.getPos()) instanceof SpawnerBlockEntity spawner))return;
        var tag=spawner.saveWithoutMetadata();int power=BrewingRules.powerTier(tag.getShort("MaxNearbyEntities"),tag.getShort("SpawnCount")),speed=BrewingRules.speedTier(tag.getShort("MinSpawnDelay"),tag.getShort("MaxSpawnDelay"));
        e.getEntity().displayClientMessage(Component.translatable("infusionbrewing.spawner.level",power,speed),false);
        if(e.getEntity().isShiftKeyDown()&&BrewingConfig.DETAILS.get())e.getEntity().displayClientMessage(Component.literal(tag.toString()),false);
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
    }
}
