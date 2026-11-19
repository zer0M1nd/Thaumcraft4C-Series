package ttmodern;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

/** The saved snapshot travels with the book; same-world owners are resolved by UUID, even offline. */
public final class ShareBookItem extends Item {
    public ShareBookItem() { super(new Properties().stacksTo(1).rarity(Rarity.EPIC)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server) {
            var tag=stack.getOrCreateTag();var data=KnowledgeSavedData.get(server.server);
            data.ensure(server.getUUID(),server.getRandom()::nextInt);
            if(!tag.hasUUID("Owner")) {
                tag.putUUID("Owner",server.getUUID());tag.putString("OwnerName",server.getGameProfile().getName());
                snapshot(tag,data.snapshot(server.getUUID()).orElseThrow());
                server.displayClientMessage(Component.translatable("ttmisc.shareTome.write"),false);
            } else {
                data.snapshot(tag.getUUID("Owner")).ifPresent(knowledge->snapshot(tag,knowledge));
                apply(server,tag);
                server.displayClientMessage(Component.translatable("ttmisc.shareTome.sync"),false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    static void snapshot(CompoundTag tag,PlayerKnowledge knowledge) {
        var research=new ListTag();knowledge.researchMarkers().stream().sorted().forEach(key->research.add(StringTag.valueOf(key)));
        var aspects=new ListTag();knowledge.knownAspects().keySet().stream().map(tc4revival.aspect.Aspect::tag).sorted().forEach(key->aspects.add(StringTag.valueOf(key)));
        tag.put("Research",research);tag.put("Aspects",aspects);
    }
    static void apply(ServerPlayer player,CompoundTag tag) {
        var research=tag.getList("Research",Tag.TAG_STRING);var aspects=tag.getList("Aspects",Tag.TAG_STRING);
        KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,knowledge->{
            for(int i=0;i<Math.min(research.size(),8192);i++) {
                String key=research.getString(i);
                if(!key.isBlank()&&key.length()<=256)ResearchCatalogs.progression().completeTrusted(knowledge,key,tc4revival.config.TCConfig.SERVER.disableWarp.get());
            }
            for(int i=0;i<Math.min(aspects.size(),4096);i++)TCAspects.catalog().find(aspects.getString(i)).ifPresent(knowledge::discover);
            return null;
        });
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flags) {
        var tag=stack.getTag();lines.add(tag==null||!tag.hasUUID("Owner")?Component.translatable("ttmisc.shareTome.noAssign"):Component.translatable("ttmisc.shareTome.playerName",tag.getString("OwnerName")));
    }
}
