package gadomancymodern.client;
import com.google.gson.*;
import gadomancymodern.disguise.ArmorDisguise;
import net.minecraft.world.item.*;
import thaumcraft.api.client.ResearchPageRenderers;

/** Rebuild the original changing armor examples using the book's actual infusion artwork. */
public class DisguiseRecipePage implements ResearchPageRenderers.Renderer {
    protected final boolean invisible;
    protected long second=Long.MIN_VALUE;
    protected tc4revival.research.ResearchPage page;
    public DisguiseRecipePage(boolean invisible){this.invisible=invisible;}
    public void render(ResearchPageRenderers.Context context,net.minecraft.client.gui.GuiGraphics graphics,int mx,int my,float partial){
        var level=net.minecraft.client.Minecraft.getInstance().level;if(level==null)return;long now=net.minecraft.Util.getMillis()/1000;
        if(now!=second){second=now;var armor=new java.util.ArrayList<ItemStack>();for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues()){var stack=item.getDefaultInstance();if(ArmorDisguise.armor(stack))armor.add(stack);}if(armor.isEmpty())return;
            var central=DynamicResearchIcons.disguise(now);var options=armor.stream().filter(stack->stack.getItem()!=central.getItem()&&ArmorDisguise.valid(central,stack)).toList();ItemStack disguise;
            if(invisible||options.isEmpty())disguise=net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(level.random.nextBoolean()?Items.POTION:Items.SPLASH_POTION),level.random.nextBoolean()?net.minecraft.world.item.alchemy.Potions.INVISIBILITY:net.minecraft.world.item.alchemy.Potions.LONG_INVISIBILITY);else disguise=options.get(level.random.nextInt(options.size()));
            var recipe=new JsonObject();recipe.addProperty("kind","runic_augmentation_example");recipe.addProperty("research","gadomancy:armor_disguise");recipe.add("input",stack(central));recipe.add("output",stack(ArmorDisguise.apply(central,disguise)));recipe.addProperty("instability",-20);var aspects=new JsonObject();aspects.addProperty("limus",12);aspects.addProperty("tutamen",10);aspects.addProperty("praecantatio",8);recipe.add("aspects",aspects);
            var sequence=new JsonObject();sequence.addProperty("kind","sequence");var elements=new JsonArray();var quicksilver=tc4revival.content.TCItems.material(tc4revival.content.MaterialType.QUICKSILVER);for(var ingredient:java.util.List.of(disguise,new ItemStack(Items.SLIME_BALL),new ItemStack(Items.SLIME_BALL),new ItemStack(quicksilver),new ItemStack(quicksilver))){var entry=new JsonObject();entry.addProperty("kind","stack");entry.add("stack",stack(ingredient));elements.add(entry);}sequence.add("elements",elements);recipe.add("ingredients",sequence);var definition=new JsonObject();definition.addProperty("id","gadomancy:armor_disguise/preview/"+invisible);definition.addProperty("type","INFUSION_CRAFTING");definition.add("recipe",recipe);page=tc4revival.research.ResearchPage.decode(definition);
        }
        if(page!=null)tc4revival.client.ResearchCraftingArtwork.draw(context.screen(),graphics,page,0,context.bounds().x()+11,context.bounds().y()+2,mx,my);
    }
    protected static JsonObject stack(ItemStack stack){var data=new JsonObject();data.addProperty("item",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());data.addProperty("count",1);if(stack.hasTag())data.addProperty("nbt",stack.getTag().toString());return data;}
}
