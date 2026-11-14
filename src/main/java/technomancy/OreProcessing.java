package technomancy;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid=Technomancy.ID)
public final class OreProcessing {
    public static ItemStack ingot(ItemStack stack){
        if(!stack.hasTag())return ItemStack.EMPTY;
        var id=ResourceLocation.tryParse(stack.getTag().getString("ingot"));
        if(id==null||!ForgeRegistries.ITEMS.containsKey(id))return ItemStack.EMPTY;
        return new ItemStack(ForgeRegistries.ITEMS.getValue(id));
    }
    private static int index(MachineKind kind){return kind==MachineKind.IGNIS_INCINERATOR?0:kind==MachineKind.BOTANICAL_PURIFIER?1:2;}
    private static ItemStack smeltableOre(ItemStack stack,Level level){
        if(level==null||stack.isEmpty()||stack.getTags().noneMatch(t->t.location().getNamespace().equals("forge")&&(t.location().getPath().startsWith("ores/")||t.location().getPath().startsWith("raw_materials/"))))return ItemStack.EMPTY;
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(stack),level);
        if(recipe.isEmpty())return ItemStack.EMPTY;
        var result=recipe.get().getResultItem(level.registryAccess());
        return result.getTags().anyMatch(t->t.location().getNamespace().equals("forge")&&t.location().getPath().startsWith("ingots/"))?result.copyWithCount(1):ItemStack.EMPTY;
    }
    public static ItemStack output(ItemStack stack,MachineKind kind,Level level){
        int stage=0;int[] passes=new int[3];ItemStack ingot;
        if(stack.getItem() instanceof PurifiedOreItem pure){
            if(pure.stage>=5||!stack.hasTag())return ItemStack.EMPTY;
            stage=pure.stage+1;int[] stored=stack.getTag().getIntArray("passes");System.arraycopy(stored,0,passes,0,Math.min(3,stored.length));
            if(Arrays.stream(passes).anyMatch(n->n<0||n>2)||Arrays.stream(passes).sum()!=stage)return ItemStack.EMPTY;
            ingot=ingot(stack);
        }else ingot=smeltableOre(stack,level);
        if(ingot.isEmpty()||passes[index(kind)]>=2)return ItemStack.EMPTY;
        passes[index(kind)]++;
        var result=new ItemStack(TechRegistry.item("purified_ore_"+(stage+1)));result.getOrCreateTag().putString("ingot",ForgeRegistries.ITEMS.getKey(ingot.getItem()).toString());result.getOrCreateTag().putIntArray("passes",passes);
        return result;
    }
    public static void tick(MachineEntity m){
        if(m.kind==MachineKind.BOTANICAL_PURIFIER)technomancy.compat.MagicIntegration.pullMana(m,5000);
        var input=m.inventory.getStackInSlot(0);var result=output(input,m.kind,m.getLevel());
        if(result.isEmpty()){m.active=false;if(m.progress!=0||m.state.contains("job")){m.progress=0;m.state.remove("job");m.changed();}return;}
        if(!m.inventory.insertItem(1,result,true).isEmpty()){m.active=false;return;}
        var fingerprint=input.copyWithCount(1).save(new CompoundTag());
        if(!fingerprint.equals(m.state.getCompound("job"))){m.progress=0;m.state.put("job",fingerprint);m.changed();}
        int stage=((PurifiedOreItem)result.getItem()).stage,reprocess=result.getTag().getIntArray("passes")[index(m.kind)];
        boolean paid=false;
        if(m.kind==MachineKind.IGNIS_INCINERATOR){int cost=Math.max(1,stage+2*reprocess);if(m.aspects.amount(MachineLogic.aspect("ignis"))>=cost){m.extract(MachineLogic.aspect("ignis"),cost,false);paid=true;}}
        else if(m.kind==MachineKind.BOTANICAL_PURIFIER){int cost=stage*150+1500*reprocess;if(m.mana>=cost){m.mana-=cost;paid=true;}}
        else paid=technomancy.compat.MagicIntegration.payBlood(m,stage*100+1000*reprocess);
        m.active=paid;if(!paid)return;m.progress++;m.changed();
        if(m.progress>=60){m.inventory.extractItem(0,1,false);m.inventory.insertItem(1,result,false);m.progress=0;m.state.remove("job");}
    }
    @SubscribeEvent public static void started(ServerStartedEvent e){installSmelting(e.getServer());}
    @SubscribeEvent public static void reload(OnDatapackSyncEvent e){if(e.getPlayer()==null)installSmelting(e.getPlayerList().getServer());}
    public static void installSmelting(MinecraftServer server){
        var level=server.overworld();var manager=server.getRecipeManager();var metals=new TreeMap<String,Item>();
        for(var item:ForgeRegistries.ITEMS){var output=smeltableOre(new ItemStack(item),level);if(!output.isEmpty())metals.put(ForgeRegistries.ITEMS.getKey(output.getItem()).toString(),output.getItem());}
        var recipes=new ArrayList<Recipe<?>>();for(var recipe:manager.getRecipes())if(!recipe.getId().getNamespace().equals(Technomancy.ID)||!recipe.getId().getPath().startsWith("purified/")&&!recipe.getId().getPath().startsWith("infernal_bonus/"))recipes.add(recipe);
        for(var entry:metals.entrySet())for(int stage=0;stage<6;stage++){
            var tag=new CompoundTag();tag.putString("ingot",entry.getKey());
            var id=Technomancy.id("purified/"+entry.getKey().replace(':','/')+"/"+(stage+1));
            recipes.add(new SmeltingRecipe(id,"technomancy:purified",CookingBookCategory.MISC,PartialNBTIngredient.of(TechRegistry.item("purified_ore_"+(stage+1)),tag),new ItemStack(entry.getValue(),stage+2),1,200));
        }
        if(TechConfig.SMELTING_BONUS.get())for(String metal:new String[]{"gold","iron","cinnabar","copper","tin","silver","lead"}){
            String nugget=switch(metal){case "iron","gold"->"minecraft:"+metal+"_nugget";case "cinnabar"->"thaumcraft:quicksilver_drop";default->"thaumcraft:"+metal+"_nugget";};
            if(!ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(nugget)))continue;
            var json=new com.google.gson.JsonObject();var input=new com.google.gson.JsonObject();input.addProperty("tag","forge:ores/"+metal);json.add("ingredient",input);
            var result=new com.google.gson.JsonObject();result.addProperty("item",nugget);json.add("result",result);json.addProperty("base_count",0);json.addProperty("legacy_order",-100);
            // The 1.7.10 API intentionally reset the supplied nine-nugget stack to zero base yield.
            recipes.add(new tc4revival.content.InfernalBonusRecipe.Serializer().fromJson(Technomancy.id("infernal_bonus/"+metal),json));
        }
        manager.replaceRecipes(recipes);
    }
    private OreProcessing(){}
}
