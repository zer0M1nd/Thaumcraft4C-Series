package conciliummodern;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;

public final class SignalFocus extends WandFocusItem {
    private static final int[] COLORS={11743532,15435844,14602026,4312372,3887386,6719955,2651799,2437522,8073150,12801229,14188952,15790320,11250603,4408131,1973019,5320730};
    static FocusUpgrade upgrade(String key) { return FocusUpgrade.require("thaumicconcilium:"+key); }
    static boolean has(ItemStack focus,String key) { return focus.getItem() instanceof WandFocusItem item && item.upgradeLevel(focus,upgrade(key).id)>0; }
    private static int colorIndex(ItemStack focus) { return Math.floorMod(focus.getOrCreateTag().getInt("Color"),COLORS.length+1); }
    @Override public String sortingPrefix() { return "CONCILIUM_SIGNAL"; }
    @Override public String depthTexture() { return "thaumicconcilium:item/signal_depth"; }
    @Override public int color(ItemStack focus) {
        int index=colorIndex(focus);
        return index==COLORS.length?net.minecraft.util.Mth.hsvToRgb((System.currentTimeMillis()%9000)/9000f,1,1):COLORS[index];
    }
    @Override public int cooldownMillis(ItemStack focus) { return 1000; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) {
        var a=TCAspects.catalog();boolean flash=has(focus,"flashbang");return Map.of(a.require("ignis"),flash?500:100,a.require("aer"),flash?1000:100);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank) {
        return switch(rank) {
            case 1 -> List.of(upgrade("large_ball"),upgrade("star"),upgrade("creeper"),upgrade("amorphic"),upgrade("random_bang"));
            case 2,3,4 -> List.of(FocusUpgrade.FRUGAL);
            case 5 -> List.of(upgrade("flashbang"));default -> List.of();
        };
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!level.isClientSide && stack.getItem() instanceof CastingWandItem wand) {
            var focus=attached(stack);
            if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,false))return InteractionResultHolder.fail(stack);
            if(player.isShiftKeyDown()) {
                focus.getOrCreateTag().putInt("Color",(colorIndex(focus)+1)%(COLORS.length+1));
                stack.getOrCreateTag().put("focus",focus.save(new CompoundTag()));
            } else if(wand.consumeForPlayer(stack,player,costs(focus),false,false,true)) {
                var blast=new CompressedBlast(ConciliumEntities.COMPRESSED_BLAST.get(),level);
                blast.initialize(player,stack,firework(focus,player));level.addFreshEntity(blast);
            }
        }
        player.swing(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    private static ItemStack firework(ItemStack focus,Player player) {
        int index=colorIndex(focus);int color=COLORS[index==COLORS.length?player.getRandom().nextInt(COLORS.length):index];
        int type=has(focus,"large_ball")?1:has(focus,"star")?2:has(focus,"creeper")?3:has(focus,"amorphic")?4:0;
        if(has(focus,"random_bang"))type=player.getRandom().nextInt(5);
        var explosion=new CompoundTag();explosion.putIntArray("Colors",new int[]{color});explosion.putByte("Type",(byte)type);
        if(player.getRandom().nextFloat()<.05f)explosion.putBoolean(player.getRandom().nextBoolean()?"Flicker":"Trail",true);
        var explosions=new ListTag();explosions.add(explosion);
        var fireworks=new CompoundTag();fireworks.putByte("Flight",(byte)2);fireworks.put("Explosions",explosions);
        var result=new ItemStack(Items.FIREWORK_ROCKET);result.getOrCreateTag().put("Fireworks",fireworks);return result;
    }
}
