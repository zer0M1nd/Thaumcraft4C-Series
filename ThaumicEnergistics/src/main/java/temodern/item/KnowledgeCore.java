package temodern.item;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import temodern.crafting.ArcanePattern;

/** A portable book of at most 21 authorized crafting patterns, as in the original addon. */
public final class KnowledgeCore extends Item {
    public static final int LIMIT=21;
    public KnowledgeCore(){super(new Properties().stacksTo(1));}
    public static List<ItemStack> patterns(ItemStack core){
        if(!(core.getItem() instanceof KnowledgeCore)||!core.hasTag())return List.of();
        var result=new ArrayList<ItemStack>();
        for(var tag:core.getTag().getList("Patterns",Tag.TAG_COMPOUND)) {
            var stack=ItemStack.of((CompoundTag)tag);if(!stack.isEmpty()&&result.size()<LIMIT)result.add(stack);
        }
        return List.copyOf(result);
    }
    public static List<ArcanePattern> decoded(ItemStack core,Level level){
        return patterns(core).stream().map(stack->new ArcanePattern.Decoder().decodePattern(stack,level,false))
                .filter(ArcanePattern.class::isInstance).map(ArcanePattern.class::cast).toList();
    }
    public static boolean toggle(ItemStack core,ItemStack pattern,Level level){
        if(!(core.getItem() instanceof KnowledgeCore))return false;
        var decoded=new ArcanePattern.Decoder().decodePattern(pattern,level,false);if(decoded==null)return false;
        var result=new ArrayList<>(patterns(core));
        boolean removed=result.removeIf(old->{var p=new ArcanePattern.Decoder().decodePattern(old,level,false);
            return p!=null && p.getOutputs()[0].what().equals(decoded.getOutputs()[0].what());});
        if(!removed){if(result.size()>=LIMIT)return false;result.add(pattern.copyWithCount(1));}
        var tags=new ListTag();for(var entry:result)tags.add(entry.save(new CompoundTag()));
        core.getOrCreateTag().put("Patterns",tags);core.removeTagKey("research");return true;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        lines.add(Component.translatable("gui.thaumicenergistics.pattern_count",patterns(stack).size(),LIMIT));
        if(level!=null)for(var pattern:decoded(stack,level))lines.add(pattern.getOutputs()[0].what().getDisplayName());
    }
}
