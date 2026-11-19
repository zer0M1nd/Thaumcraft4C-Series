package ttmodern;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Worn-only XP storage. A modern orb may represent several equal denominations. */
public final class XpTalismanItem extends Item implements ICurioItem {
    public static final int CAPACITY=1500, BOTTLE_COST=10;
    public XpTalismanItem(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    public static int xp(ItemStack stack){return stack.hasTag()?Math.max(0,Math.min(CAPACITY,stack.getTag().getInt("xp"))):0;}
    public static boolean active(ItemStack stack){return stack.hasTag()&&stack.getTag().getBoolean("enabled");}
    public static void setXp(ItemStack stack,int value){stack.getOrCreateTag().putInt("xp",Math.max(0,Math.min(CAPACITY,value)));}
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack){return false;}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material){return false;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(level.isClientSide)return InteractionResultHolder.success(stack);
        if(player.isShiftKeyDown()){
            if(xp(stack)<CAPACITY){stack.getOrCreateTag().putBoolean("enabled",!active(stack));sound(player,.3f,.1f);}
        }else if(xp(stack)>=BOTTLE_COST){
            // Search normal player inventory (including offhand), never armor or other containers.
            int slot=-1;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(Items.GLASS_BOTTLE)){slot=i;break;}
            if(slot>=0){player.getInventory().removeItem(slot,1);setXp(stack,xp(stack)-BOTTLE_COST);var bottle=new ItemStack(Items.EXPERIENCE_BOTTLE);if(!player.getInventory().add(bottle))player.drop(bottle,false);player.getInventory().setChanged();sound(player,.1f,.1f+level.random.nextFloat()*.5f);}
        }
        return InteractionResultHolder.consume(stack);
    }
    private static void sound(Player player,float volume,float pitch){player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,volume,pitch);}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        var wearer=context.entity();var level=wearer.level();if(level.isClientSide||!active(stack))return;
        int stored=xp(stack);if(stored>=CAPACITY){stack.getOrCreateTag().putBoolean("enabled",false);return;}
        var position=wearer.position();var bounds=new AABB(position.x-3,position.y-3,position.z-3,position.x+3,position.y+3,position.z+3);
        for(var orb:level.getEntitiesOfClass(ExperienceOrb.class,bounds,entity->entity.isAlive())){
            int value=orb.getValue();if(value<=0||value>CAPACITY-stored)continue;
            var data=new CompoundTag();orb.addAdditionalSaveData(data);int count=Math.max(1,data.getInt("Count"));int taken=Math.min(count,(CAPACITY-stored)/value);if(taken<=0)continue;
            stored+=taken*value;
            if(taken==count)orb.discard();else {data.putInt("Count",count-taken);orb.readAdditionalSaveData(data);}
            level.playSound(null,orb.blockPosition(),tc4revival.content.TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.PLAYERS,Math.min(4,value/10f),1);
            if(level instanceof net.minecraft.server.level.ServerLevel server)thaumcraft.api.fx.LegacyEffects.wisp(server,orb.position(),0x19e619,Math.min(16,orb.getValue()/5f),1,0,.02f);
            if(stored==CAPACITY)break;
        }
        setXp(stack,stored);if(stored==CAPACITY)stack.getOrCreateTag().putBoolean("enabled",false);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){lines.add(Component.literal("XP: "+xp(stack)));lines.add(Component.translatable(xp(stack)>=CAPACITY?"ttmisc.full":active(stack)?"ttmisc.absorbing":"ttmisc.notAbsorbing"));}
}
