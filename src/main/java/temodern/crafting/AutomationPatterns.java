package temodern.crafting;

import java.util.*;
import appeng.api.crafting.IPatternDetails;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import tc4revival.content.ArcaneRecipe;
import tc4revival.content.TCArcane;
import tc4revival.research.KnowledgeSavedData;
import thaumcraft.api.wands.AutomationCore;

/** Live owner knowledge; cache invalidates for research changes, datapack reloads and selection changes. */
public class AutomationPatterns {
    private ItemStack previousCore=ItemStack.EMPTY, previousSelection=ItemStack.EMPTY;
    private long revision=-1;
    private List<ArcaneRecipe> recipes=List.of();
    private List<IPatternDetails> patterns=List.of();
    private boolean changed;
    public List<IPatternDetails> resolve(ItemStack core, ItemStack selected, ServerLevel level) {
        if (!(core.getItem() instanceof AutomationCore source) || !source.enabled(core)||source.owner(core)==null) {
            if(!patterns.isEmpty())changed=true;
            patterns=List.of();previousCore=ItemStack.EMPTY;revision=-1;return patterns;
        }
        long version=KnowledgeSavedData.get(level.getServer()).revision(source.owner(core));
        var current=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get());
        if(version==revision && current.equals(recipes) && ItemStack.matches(core,previousCore) && ItemStack.matches(selected,previousSelection))return patterns;
        revision=version;recipes=List.copyOf(current);previousCore=core.copy();previousSelection=selected.copy();changed=true;
        var known=source.research(core,level);var result=new ArrayList<IPatternDetails>();
        if(!selected.isEmpty()) {
            var decoded=new ArcanePattern.Decoder().decodePattern(selected,level,false);
            if(decoded instanceof ArcanePattern pattern) {
                var recipe=ArcanePattern.recipe(level,pattern.recipe());var grid=pattern.grid();
                if(recipe!=null && recipe.matches(grid,level) && recipe.unlocked(grid,known::contains))result.add(pattern);
            }
        } else for(var recipe:current) {
            var json=recipe.displayDefinition();String mode=json.has("mode")?json.get("mode").getAsString():"shaped";
            // Variable wand/cap/NBT recipes remain available through an explicitly encoded pattern.
            if(!mode.equals("shaped")&&!mode.equals("shapeless"))continue;
            CraftingRecipe crafting=mode.equals("shaped")?RecipeSerializer.SHAPED_RECIPE.fromJson(recipe.getId(),json):RecipeSerializer.SHAPELESS_RECIPE.fromJson(recipe.getId(),json);
            int width=crafting instanceof ShapedRecipe shaped?shaped.getWidth():3;var grid=new CraftGrid();
            var ingredients=crafting.getIngredients();if(ingredients.size()>9)continue;
            for(int i=0;i<ingredients.size();i++) {
                var choices=ingredients.get(i).getItems();
                if(choices.length>0)grid.setItem(i%width+3*(i/width),choices[0].copyWithCount(1));
            }
            if(!recipe.matches(grid,level)||!recipe.unlocked(grid,known::contains))continue;
            var decoded=new ArcanePattern.Decoder().decodePattern(ArcanePattern.encode(grid,recipe,level),level,false);
            if(decoded instanceof ArcanePattern)result.add(decoded);
        }
        patterns=List.copyOf(result);return patterns;
    }
    public boolean takeChanged() { boolean value=changed;changed=false;return value; }
    public static boolean accepted(ItemStack stack) { return stack.getItem() instanceof temodern.item.KnowledgeCore || stack.getItem() instanceof AutomationCore source && source.owner(stack)!=null; }
}
