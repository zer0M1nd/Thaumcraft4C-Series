package ttmodern;

import java.util.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;

/** Exact single-roll previews; unfamiliar data-pack rules are explicitly not assigned a probability. */
public final class CropDropPreview {
    public record Drop(ItemStack stack,double chance){}
    public record Recipe(String aspect,List<Drop> drops,boolean exact){public Recipe{drops=List.copyOf(drops);}}
    public record Snapshot(List<Recipe> recipes){public Snapshot{recipes=List.copyOf(recipes);}}
    public static List<Recipe> collect(net.minecraft.server.MinecraftServer server){var result=new ArrayList<Recipe>();var gson=net.minecraft.world.level.storage.loot.Deserializers.createLootTableSerializer().create();for(var aspect:tc4revival.aspect.TCAspects.catalog().all()){var table=server.getLootData().getLootTable(ResourceLocation.fromNamespaceAndPath(Tinkerer.ID,"crop_loot/"+aspect.tag().replace(':','/')));try{result.add(parse(aspect.tag(),gson.toJsonTree(table).getAsJsonObject()));}catch(RuntimeException unsupported){result.add(new Recipe(aspect.tag(),List.of(),false));}}return result;}
    static Recipe parse(String aspect,JsonObject table){try{
        if(table.has("functions")&&table.getAsJsonArray("functions").size()>0)return new Recipe(aspect,List.of(),false);
        var pools=table.getAsJsonArray("pools");if(pools==null||pools.isEmpty())return new Recipe(aspect,List.of(),true);if(pools.size()!=1)return new Recipe(aspect,List.of(),false);
        var pool=pools.get(0).getAsJsonObject();if(nonempty(pool,"conditions")||nonempty(pool,"functions")||number(pool.get("rolls"))!=1||pool.has("bonus_rolls")&&number(pool.get("bonus_rolls"))!=0)return new Recipe(aspect,List.of(),false);
        var stacks=new ArrayList<ItemStack>();var weights=new ArrayList<Integer>();long total=0;
        for(var raw:pool.getAsJsonArray("entries")){var entry=raw.getAsJsonObject();if(nonempty(entry,"conditions")||entry.has("quality")&&entry.get("quality").getAsInt()!=0)throw new IllegalArgumentException();int weight=entry.has("weight")?entry.get("weight").getAsInt():1;if(weight<0)throw new IllegalArgumentException();String type=entry.get("type").getAsString();var items=new ArrayList<ItemStack>();
            if(type.equals("minecraft:empty")){total+=weight;continue;}
            var id=ResourceLocation.parse(entry.get("name").getAsString());
            if(type.equals("minecraft:item")){var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);if(item==null)throw new IllegalArgumentException();items.add(new ItemStack(item));}
            else if(type.equals("minecraft:tag")&&entry.has("expand")&&entry.get("expand").getAsBoolean()){for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.ItemTags.create(id)))items.add(new ItemStack(item));}
            else throw new IllegalArgumentException();
            for(var stack:items){if(entry.has("functions"))for(var rawFunction:entry.getAsJsonArray("functions")){var function=rawFunction.getAsJsonObject();if(nonempty(function,"conditions"))throw new IllegalArgumentException();switch(function.get("function").getAsString()){
                case "minecraft:set_count" -> {if(function.has("add")&&function.get("add").getAsBoolean())throw new IllegalArgumentException();double count=number(function.get("count"));if(count!=Math.rint(count)||count<1||count>64)throw new IllegalArgumentException();stack.setCount((int)count);}
                case "minecraft:set_nbt" -> stack.getOrCreateTag().merge(net.minecraft.nbt.TagParser.parseTag(function.get("tag").getAsString()));
                default -> throw new IllegalArgumentException();
            }}stacks.add(stack);weights.add(weight);total+=weight;}
        }
        var drops=new ArrayList<Drop>();if(total>0)for(int i=0;i<stacks.size();i++)if(weights.get(i)>0)drops.add(new Drop(stacks.get(i),weights.get(i)/(double)total));if(drops.size()>1024)throw new IllegalArgumentException();return new Recipe(aspect,drops,true);
    }catch(Exception unsupported){return new Recipe(aspect,List.of(),false);}}
    private static boolean nonempty(JsonObject value,String key){return value.has(key)&&!value.getAsJsonArray(key).isEmpty();}
    private static double number(JsonElement value){if(value.isJsonPrimitive())return value.getAsDouble();var object=value.getAsJsonObject();if(object.get("type").getAsString().equals("minecraft:constant"))return object.get("value").getAsDouble();throw new IllegalArgumentException();}
    public static void encode(Snapshot packet,FriendlyByteBuf buffer){buffer.writeVarInt(packet.recipes().size());for(var recipe:packet.recipes()){buffer.writeUtf(recipe.aspect(),128);buffer.writeBoolean(recipe.exact());buffer.writeVarInt(recipe.drops().size());for(var drop:recipe.drops()){buffer.writeItem(drop.stack());buffer.writeDouble(drop.chance());}}}
    public static Snapshot decode(FriendlyByteBuf buffer){int count=bounded(buffer.readVarInt(),4096);var recipes=new ArrayList<Recipe>();for(int i=0;i<count;i++){String aspect=buffer.readUtf(128);boolean exact=buffer.readBoolean();int size=bounded(buffer.readVarInt(),1024);var drops=new ArrayList<Drop>();for(int j=0;j<size;j++){var stack=buffer.readItem();double chance=buffer.readDouble();if(!Double.isFinite(chance)||chance<0||chance>1)throw new IllegalArgumentException("Invalid probability");drops.add(new Drop(stack,chance));}recipes.add(new Recipe(aspect,drops,exact));}return new Snapshot(recipes);}
    private static int bounded(int value,int max){if(value<0||value>max)throw new IllegalArgumentException("Invalid preview length");return value;}
}
