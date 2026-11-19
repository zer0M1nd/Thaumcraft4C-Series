package tc4revival.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.network.ResearchCompletionPacket;
import tc4revival.network.TCNetwork;
import tc4revival.research.*;

/** Note/discovery/unknown forms replace the old item's metadata, retaining its full note tag. */
public  class ResearchNotesItem extends Item {
    private final boolean unknown;
    private final boolean discovery;
    public ResearchNotesItem(boolean unknown, boolean discovery) {
        super(new Properties().stacksTo(discovery ? 64 : 1).rarity(discovery ? Rarity.EPIC : Rarity.RARE));
        this.unknown = unknown;
        this.discovery = discovery;
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable(discovery ? "item.discovery.name" : "item.researchnotes.name");
    }
    public static ItemStack create(ResearchNote note) {
        var result = new ItemStack(note.complete() ? TCItems.DISCOVERY.get() : TCItems.RESEARCH_NOTES.get());
        ResearchNoteNbt.write(result.getOrCreateTag(), note);
        return result;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.success(stack);
        var data = KnowledgeSavedData.get(serverPlayer.getServer());
        var knowledge = data.snapshot(player.getUUID()).orElseThrow();
        var read = ResearchNoteNbt.read(stack.getTag(), TCAspects.catalog());
        if (!read.problems().isEmpty()) return InteractionResultHolder.fail(stack);
        var note = read.note();
        var rules = ResearchCatalogs.rules();
        if (note.filter(ResearchNote::complete).isPresent() && !rules.completed(knowledge, note.orElseThrow().key())) {
            if(learn(serverPlayer,stack))return InteractionResultHolder.consume(stack);
            return InteractionResultHolder.fail(stack);
        } else if (unknown) {
            var selected = rules.findHidden(knowledge, level.getDayTime());
            if (selected.isEmpty()) {
                stack.shrink(1);
                var fragments = new ItemStack(TCItems.KNOWLEDGE_FRAGMENT.get(), 7 + level.random.nextInt(3));
                level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY() + player.getEyeHeight() / 2, player.getZ(), fragments));
                level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.ERASE.get(), SoundSource.PLAYERS, 0.75f, 1);
                return InteractionResultHolder.consume(stack);
            }
            var generated = new ResearchNotes(TCAspects.catalog()).generate(ResearchCatalogs.catalog().require(selected.orElseThrow()), level.random::nextInt, tc4revival.config.TCConfig.SERVER.randomizedResearchNotes.get());
            if (generated.isEmpty()) return InteractionResultHolder.fail(stack);
            var revealed = new ItemStack(TCItems.RESEARCH_NOTES.get(), stack.getCount());
            if (stack.hasTag()) revealed.setTag(stack.getTag().copy());
            ResearchNoteNbt.write(revealed.getOrCreateTag(), generated.orElseThrow());
            player.setItemInHand(hand, revealed);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.WRITE.get(), SoundSource.PLAYERS, 0.75f, 1);
            return InteractionResultHolder.consume(revealed);
        }
        return InteractionResultHolder.pass(stack);
    }
    public static boolean learn(ServerPlayer player,ItemStack stack) {
        if(!stack.is(TCItems.DISCOVERY.get()))return false;
        var read=ResearchNoteNbt.read(stack.getTag(),TCAspects.catalog());
        if(!read.problems().isEmpty()||read.note().filter(ResearchNote::complete).isEmpty())return false;
        var data=KnowledgeSavedData.get(player.server);var knowledge=data.snapshot(player.getUUID()).orElse(null);if(knowledge==null)return false;
        String key=read.note().orElseThrow().key();var rules=ResearchCatalogs.rules();
        if(rules.completed(knowledge,key)||!rules.prerequisites(knowledge,key))return false;
        var completed=data.update(player.getUUID(),player.getRandom()::nextInt,working->ResearchCatalogs.progression().completeEligible(working,key,TCConfig.SERVER.disableWarp.get()));
        if(completed.isEmpty())return false;
        stack.shrink(1);
        for(var completion:completed)TCNetwork.send(player,ResearchCompletionPacket.from(completion));
        player.level().playSound(null,player.getX(),player.getY(),player.getZ(),TCSounds.LEARN.get(),SoundSource.PLAYERS,.75f,1);
        return true;
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        if (unknown) {
            lines.add(Component.translatable("item.researchnotes.unknown.1").withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("item.researchnotes.unknown.2").withStyle(ChatFormatting.BLUE));
        }
        var read = ResearchNoteNbt.read(stack.getTag(), TCAspects.catalog());
        read.note().flatMap(note -> ResearchCatalogs.bundled().find(note.key())).ifPresent(research -> {
            lines.add(Component.translatable("tc.research_name." + research.key()).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("tc.research_text." + research.key()).withStyle(ChatFormatting.ITALIC));
            if (research.warp() > 0) lines.add(Component.translatable("tc.forbidden",
                    Component.translatable("tc.forbidden.level." + Math.min(5, research.warp()))).withStyle(ChatFormatting.DARK_PURPLE));
        });
    }
}
