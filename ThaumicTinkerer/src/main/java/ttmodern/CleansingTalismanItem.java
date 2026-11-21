package ttmodern;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CleansingTalismanItem extends Item implements ICurioItem {
    private static final String BURNING="burning";
    private static final Set<String> EXPENSIVE=Set.of("thaumcraft:blurred_vision","thaumcraft:death_gaze","thaumcraft:infectious_vis_exhaust","thaumcraft:sun_scorned","thaumcraft:unnatural_hunger");
    private static final Map<Player,State> STATES=new WeakHashMap<>();
    private static final class State {String last;int tick=Integer.MIN_VALUE;final Set<String> permanent=new HashSet<>();}
    public CleansingTalismanItem(){super(new Properties().durability(100).rarity(Rarity.UNCOMMON));}
    public static boolean active(ItemStack stack){return stack.hasTag()&&stack.getTag().getBoolean("enabled");}
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack){return false;}
    @Override public int getEnchantmentValue(){return 10;}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material){return material.is(Tinkerer.DARK_QUARTZ.get());}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(player.isShiftKeyDown()&&!level.isClientSide){stack.getOrCreateTag().putBoolean("enabled",!active(stack));STATES.remove(player);level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.3f,.1f);}return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void onUnequip(SlotContext context,ItemStack newStack,ItemStack stack){if(context.entity() instanceof Player player)STATES.remove(player);}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(!(context.entity() instanceof Player player)||player.level().isClientSide)return;
        if(!active(stack)){STATES.remove(player);return;}if(player.tickCount%20!=0)return;
        var state=STATES.computeIfAbsent(player,p->new State());if(state.tick==player.tickCount)return;
        if(state.tick!=player.tickCount-20){state.last=null;state.permanent.clear();}state.tick=player.tickCount;
        var candidates=new LinkedHashMap<String,net.minecraft.world.effect.MobEffect>();if(player.isOnFire())candidates.put(BURNING,null);
        for(var effect:player.getActiveEffects())if((effect.isInfiniteDuration()||effect.getDuration()>=20)&&effect.getEffect().getCategory()==MobEffectCategory.HARMFUL){var id=net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());if(id!=null)candidates.put(id.toString(),effect.getEffect());}
        String charged=null;var removed=new HashSet<String>();
        for(var entry:candidates.entrySet()){
            String id=entry.getKey();if(state.permanent.contains(id)||id.equals(state.last)){state.permanent.add(id);}else if(charged==null){charged=id;}else continue;
            if(entry.getValue()==null){player.clearFire();removed.add(id);}else if(player.removeEffect(entry.getValue()))removed.add(id);else {state.permanent.remove(id);if(id.equals(charged))charged=null;}
        }
        boolean ended=state.permanent.retainAll(removed);state.last=charged;
        if(charged!=null)stack.hurtAndBreak(EXPENSIVE.contains(charged)?10:1,player,p->{});
        if(stack.isEmpty())STATES.remove(player);
        if(charged!=null||ended)player.level().playSound(null,player.blockPosition(),tc4revival.content.TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.PLAYERS,.3f,.1f);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable(active(stack)?"ttmisc.active":"ttmisc.inactive"));}
}
