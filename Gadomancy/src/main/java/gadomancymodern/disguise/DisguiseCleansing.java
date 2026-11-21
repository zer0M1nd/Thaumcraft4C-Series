package gadomancymodern.disguise;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original 40..99 tick soaking delay and source-only removal, with no retained unloaded entities. */
@Mod.EventBusSubscriber(modid="gadomancy")
public class DisguiseCleansing {
    protected static final java.util.Map<ItemEntity,Long> TRACKED=new java.util.WeakHashMap<>();
    @SubscribeEvent public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event){if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof ItemEntity item&&ArmorDisguise.disguised(item.getItem()))TRACKED.put(item,level.getGameTime()+40+level.random.nextInt(60));}
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){TRACKED.keySet().removeIf(item->item.level()==event.getLevel());}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.LevelTickEvent event){if(event.phase==net.minecraftforge.event.TickEvent.Phase.START&&event.level instanceof ServerLevel level&&level.getGameTime()%10==0){var iterator=TRACKED.entrySet().iterator();while(iterator.hasNext()){var entry=iterator.next();var item=entry.getKey();if(item.isRemoved()||!ArmorDisguise.disguised(item.getItem())){iterator.remove();continue;}if(item.level()==level)advance(item,entry.getValue());}}}
    public static void advance(ItemEntity item,long deadline){
        if(!(item.level() instanceof ServerLevel level)||!ArmorDisguise.disguised(item.getItem()))return;
        if(item.hasPickUpDelay()||level.getGameTime()<=deadline){DisguiseNetwork.bubbles(level,item.position());return;}
        var pos=item.blockPosition();var state=level.getBlockState(pos);if(!state.is(tc4revival.content.TCCleansing.PURE_WATER.get())||state.getValue(LiquidBlock.LEVEL)!=0)return;
        var cleaned=item.getItem().copy();var returned=ArmorDisguise.remove(cleaned);item.setItem(cleaned);
        if(!returned.isEmpty()){var drop=new ItemEntity(level,item.getX(),item.getY(),item.getZ(),returned);drop.setDeltaMovement(level.random.nextFloat()*.7-.35,level.random.nextFloat()*.7-.35,level.random.nextFloat()*.7-.35);level.addFreshEntity(drop);}
        level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
    }
    @SubscribeEvent public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event){if(ArmorDisguise.disguised(event.getItemStack())){var appearance=ArmorDisguise.appearance(event.getItemStack());event.getToolTip().add((appearance.isEmpty()?net.minecraft.network.chat.Component.translatable("gadomancy.lore.disguise.none"):net.minecraft.network.chat.Component.translatable("gadomancy.lore.disguise.item",appearance.getHoverName().getString())).withStyle(net.minecraft.ChatFormatting.GREEN));}}
}
