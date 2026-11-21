package tc4revival.client;

import java.util.*;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.aspect.editor.*;
import tc4revival.aspect.item.*;

public class EditorViewModel {
    public record Target(String kind,String id,ItemStack stack,BlockState state,Component name,List<String> tags) {
        public String key(){return kind+":"+id;}
        public List<String> tags(){
            if(kind.equals("entity"))return List.of();
            return state==null?stack.getItem().builtInRegistryHolder().tags().map(t->t.location().toString()).sorted().toList()
                    :state.getBlock().builtInRegistryHolder().tags().map(t->t.location().toString()).sorted().toList();
        }
        public boolean hasTag(String id){
            if(kind.equals("entity"))return false;
            var key=new ResourceLocation(id);
            return state==null?stack.is(net.minecraft.tags.ItemTags.create(key)):state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,key));
        }
        public String typeLabel(){return switch(kind){case "block"->"方块";case "entity"->"实体";default->"物品";};}
    }
    public record View(int status,Map<String,Integer> values) {}
    public static final int DIRECT=0,TAG=1,RECIPE=2,UNDEFINED=3,ADVANCED=4;
    public static boolean canSolidify(int status){return status==RECIPE||status==ADVANCED;}
    public final List<Target> targets=new ArrayList<>();
    public final Map<String,Map<String,Integer>> drafts=new LinkedHashMap<>();
    private final Map<String,View> baseline=new HashMap<>();
    public EditorViewModel(){
        for(var item:ForgeRegistries.ITEMS.getValues())if(item!=Items.AIR){
            String id=ForgeRegistries.ITEMS.getKey(item).toString();var stack=new ItemStack(item);
            var tags=item.builtInRegistryHolder().tags().map(t->t.location().toString()).sorted().toList();
            Component name=Component.literal(id);
            try{if(I18n.exists(item.getDescriptionId(stack)))name=stack.getHoverName();}catch(Throwable ignored){}
            targets.add(new Target("item",id,stack,null,name,tags));
        }
        for(var block:ForgeRegistries.BLOCKS.getValues())if(block.asItem()==Items.AIR&&!block.defaultBlockState().isAir()){
            String id=ForgeRegistries.BLOCKS.getKey(block).toString();var tags=block.builtInRegistryHolder().tags().map(t->t.location().toString()).sorted().toList();
            targets.add(new Target("block",id,ItemStack.EMPTY,block.defaultBlockState(),I18n.exists(block.getDescriptionId())?block.getName():Component.literal(id),tags));
        }
        for(var entity:ForgeRegistries.ENTITY_TYPES.getValues()){
            String id=ForgeRegistries.ENTITY_TYPES.getKey(entity).toString();
            targets.add(new Target("entity",id,ItemStack.EMPTY,null,I18n.exists(entity.getDescriptionId())?entity.getDescription():Component.literal(id),List.of()));
        }
        targets.sort(Comparator.comparing(Target::id).thenComparing(Target::kind));
    }
    public void invalidate(){baseline.clear();}
    public View view(Target target){
        var exact=drafts.get(target.key());if(exact!=null)return new View(DIRECT,exact);
        var base=baseline.computeIfAbsent(target.key(),ignored->load(target));
        // Legacy block scan aliases are a fallback; editor block tags supersede them.
        var data=ClientItemAspects.calculation();
        if(base.status()==DIRECT&&(target.kind().equals("item")||data!=null&&data.editor().get(target.key()).isPresent()))return base;
        Map<String,Integer> tag=null;
        for(var e:drafts.entrySet())if(e.getKey().startsWith(target.kind()+"_tag:")&&target.hasTag(e.getKey().substring(target.kind().length()+5)))tag=e.getValue();
        return tag==null?base:new View(TAG,tag);
    }
    private View load(Target target){
        var data=ClientItemAspects.calculation();if(data==null)return new View(UNDEFINED,Map.of());
        var exact=data.editor().get(target.key());if(exact.isPresent())return new View(DIRECT,exact.get());
        if(target.kind().equals("entity")){
            var amounts=data.entities().preview(target.id());
            return new View(amounts.isPresent()?(data.advancedSources().entity(target.id())?ADVANCED:DIRECT):UNDEFINED,amounts.map(EditorDefinitions::values).orElse(Map.of()));
        }
        if(target.kind().equals("item")){
            var rule=data.direct().selectedRule(target.id(),target::hasTag,target.stack().getTag());
            var computed=ClientItemAspects.resolve(target.stack()).map(v->EditorDefinitions.values(v.amounts())).orElse(Map.of());
            boolean advanced=data.advancedSources().item(target.id(),target::hasTag,target.stack().getTag());
            if(!advanced&&rule.isPresent()&&!rule.get().generated()&&rule.get().requiredTag()==null)return new View(DIRECT,computed);
            var tags=data.editor().tag("item",target::hasTag);if(tags.isPresent())return new View(TAG,tags.get());
            if(advanced)return new View(ADVANCED,computed);
            if(rule.isPresent())return new View(rule.get().requiredTag()!=null?TAG:rule.get().generated()?RECIPE:DIRECT,computed);
            return new View(computed.isEmpty()?UNDEFINED:RECIPE,computed);
        }
        var tagged=data.editor().tag("block",target::hasTag);if(tagged.isPresent())return new View(TAG,tagged.get());
        var explicit=data.blockAspects().resolve(target.id(),BlockScanTargets.properties(target.state()));
        if(explicit.isPresent())return new View(data.advancedSources().block(target.id(),BlockScanTargets.properties(target.state()))?ADVANCED:DIRECT,EditorDefinitions.values(explicit.get()));
        var mapped=data.blocks().resolve(target.id(),BlockScanTargets.properties(target.state()));
        if(mapped.isPresent()){
            var m=mapped.get();AspectAmounts values;
            if(m.modernItem()!=null)values=ClientItemAspects.resolve(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(m.modernItem())))).map(ClientItemAspects.Result::amounts).orElseGet(AspectAmounts::new);
            else values=data.recipeObject(data.identity().scanKey(m.legacyKey()));
            return new View(DIRECT,values==null?Map.of():EditorDefinitions.values(values));
        }
        return new View(UNDEFINED,Map.of());
    }
    public Map<String,Integer> values(Target target,String tag){
        if(tag==null)return view(target).values();
        String key=target.kind()+"_tag:"+tag;
        if(drafts.containsKey(key))return drafts.get(key);
        var data=ClientItemAspects.calculation();if(data==null)return Map.of();
        var saved=data.editor().get(key);if(saved.isPresent())return saved.get();
        if(target.kind().equals("item")){
            var rule=data.direct().tagRule(target.id(),tag);
            if(rule.isPresent()&&tag.equals(rule.get().requiredTag()))return EditorDefinitions.values(rule.get().amounts(TCAspects.catalog()));
        }
        return Map.of();
    }
    public void put(String key,Map<String,Integer> values){drafts.remove(key);drafts.put(key,Map.copyOf(values));}
}
