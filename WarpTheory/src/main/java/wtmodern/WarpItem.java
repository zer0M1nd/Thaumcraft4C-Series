package wtmodern;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import tc4revival.research.*;
public class WarpItem extends Item {
    private final String kind;
    public WarpItem(String kind,int count){super(new Properties().stacksTo(count).rarity(Rarity.UNCOMMON));this.kind=kind;}
    @Override public String getDescriptionId(){return "item.warptheory."+kind;}
    @Override public int getUseDuration(ItemStack stack){return kind.equals("cleanser")?24:32;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.EAT;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(kind.equals("amulet"))return InteractionResultHolder.pass(stack);
        if(kind.equals("paper")){
            if(player instanceof ServerPlayer p){int total=WarpState.total(p);var k=WarpState.knowledge(p);
                p.sendSystemMessage(Component.translatable("chat.warptheory."+WarpRules.severityKey(total)).withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC));
                p.sendSystemMessage(Component.literal(" (").append(Component.literal(k.warp(PlayerKnowledge.WarpKind.PERMANENT)+" ")).append(Component.translatable("chat.warptheory.permanentwarp")).append(", "+k.warp(PlayerKnowledge.WarpKind.STICKY)+" ").append(Component.translatable("chat.warptheory.normalwarp")).append(", "+k.warp(PlayerKnowledge.WarpKind.TEMPORARY)+" ").append(Component.translatable("chat.warptheory.tempwarp")).append(")"));
                if(!p.getAbilities().instabuild&&total<=10)stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        return ItemUtils.startUsingInstantly(level,player,hand);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
        if(user instanceof ServerPlayer p){
            if(kind.equals("cleanser")){
                p.sendSystemMessage(Component.translatable("chat.warptheory."+(WarpState.total(p)>0?"purge":"purgefail")));
                WarpState.purge(p);level.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.SPLASH_POTION_BREAK,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
            }else if(kind.equals("something")){
                KnowledgeSavedData.get(p.server).update(p.getUUID(),p.getRandom()::nextInt,k->{k.changeWarp(PlayerKnowledge.WarpKind.PERMANENT,4+p.getRandom().nextInt(4));k.changeWarp(PlayerKnowledge.WarpKind.TEMPORARY,5+p.getRandom().nextInt(5));k.changeWarp(PlayerKnowledge.WarpKind.STICKY,5+p.getRandom().nextInt(5));k.setWarpCounter(k.totalWarp());return null;});
                p.sendSystemMessage(Component.translatable("chat.warptheory.addwarp").withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC));
            }
            if(!p.getAbilities().instabuild)stack.shrink(1);
        }
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("tooltip.warptheory."+kind).withStyle(ChatFormatting.DARK_GRAY,ChatFormatting.ITALIC));}
}
