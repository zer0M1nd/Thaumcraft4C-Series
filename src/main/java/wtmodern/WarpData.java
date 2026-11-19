package wtmodern;
import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;
import net.minecraftforge.data.event.GatherDataEvent;

/** Gameplay JSON is generated; original artwork and prose are loaded from the user's legacy archive. */
public class WarpData implements DataProvider {
    private final PackOutput output;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final List<Object> pageRecipes=new ArrayList<>();
    public WarpData(PackOutput output){this.output=output;}
    public static void gather(GatherDataEvent event){event.getGenerator().addProvider(true,new WarpData(event.getGenerator().getPackOutput()));}
    public String getName(){return "Warp Theory recipes, research and models";}
    private CompletableFuture<?> save(CachedOutput cache,String path,Object data){if(path.startsWith("data/warptheory/recipes/"))displayRecipe(path,data);return DataProvider.saveStable(cache,JSON.toJsonTree(data),output.getOutputFolder().resolve(path));}
    private static Object displayItem(JsonObject ingredient){if(ingredient.has("ingredient"))ingredient=ingredient.getAsJsonObject("ingredient");return obj("kind","stack","stack",obj("item",ingredient.get("item").getAsString(),"count",ingredient.has("count")?ingredient.get("count").getAsInt():1));}
    private void displayRecipe(String path,Object value){
        var recipe=JSON.toJsonTree(value).getAsJsonObject();String type=recipe.get("type").getAsString();var row=obj("research",recipe.get("research").getAsString(),"output",displayItem(recipe.getAsJsonObject("result")));
        if(type.endsWith(":crucible")){row.put("kind","crucible");row.put("catalyst",displayItem(recipe.getAsJsonObject("catalyst")));row.put("aspects",recipe.get("aspects"));}
        else{var parts=new ArrayList<Object>();for(var ingredient:recipe.getAsJsonArray(type.endsWith(":infusion")?"components":"ingredients"))parts.add(displayItem(ingredient.getAsJsonObject()));row.put("ingredients",obj("kind","sequence","elements",parts));if(type.endsWith(":infusion")){row.put("kind","infusion");row.put("input",displayItem(recipe.getAsJsonObject("center")));row.put("instability",recipe.get("instability"));row.put("aspects",recipe.get("aspects"));}else{row.put("kind","arcane_shapeless");row.put("aspects",recipe.get("vis"));}}
        pageRecipes.add(obj("key","warptheory:"+path.substring(path.lastIndexOf('/')+1,path.length()-5),"class","modern","data",row));
    }
    private static Map<String,Object> obj(Object... pairs){var m=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
    private static Map<String,Object> ingredient(String item){return obj("item",item);}
    private static Map<String,Object> infusionIngredient(String item){return obj("ingredient",ingredient(item));}
    @Override public CompletableFuture<?> run(CachedOutput cache){
        var jobs=new ArrayList<CompletableFuture<?>>();
        String[][] models={{"pure_tear","itemcleanser"},{"pure_talisman","itemamulet"},{"something","itemsomething"},{"litmus_paper","itempaper"}};
        for(var m:models)jobs.add(save(cache,"assets/warptheory/models/item/"+m[0]+".json",obj("parent","minecraft:item/generated","textures",obj("layer0","warptheory:items/"+m[1]))));
        jobs.add(save(cache,"assets/minecraft/atlases/blocks.json",obj("sources",java.util.Arrays.stream(models).map(m->obj("type","minecraft:single","resource","warptheory:items/"+m[1])).toList())));
        jobs.add(save(cache,"assets/warptheory/particles/blood.json",obj("textures",List.of("minecraft:drip_hang","minecraft:drip_fall","minecraft:drip_land"))));
        jobs.add(save(cache,"data/curios/tags/items/necklace.json",obj("replace",false,"values",List.of("warptheory:pure_talisman"))));
        for(boolean easy:List.of(false,true)){
            String suffix=easy?"_easy":"";
            var conditions=List.of(obj("type","warptheory:easy_recipes","easy",easy));
            var tearParts=easy?List.of("thaumcraft:salis_mundus","thaumcraft:salis_mundus","minecraft:quartz","minecraft:quartz"):List.of("thaumcraft:salis_mundus","minecraft:ghast_tear","minecraft:diamond","minecraft:quartz","thaumcraft:salis_mundus","minecraft:ghast_tear","minecraft:diamond","minecraft:quartz");
            var amuletParts=easy?List.of("warptheory:pure_tear","minecraft:gold_ingot","warptheory:pure_tear","minecraft:gold_ingot"):List.of("warptheory:pure_tear","minecraft:gold_ingot","minecraft:diamond","thaumcraft:salis_mundus","warptheory:pure_tear","minecraft:gold_ingot","minecraft:diamond","thaumcraft:salis_mundus");
            jobs.add(save(cache,"data/warptheory/recipes/pure_tear"+suffix+".json",obj("type","thaumcraft:infusion","conditions",conditions,"research","warptheory:WARPCLEANSER","instability",5,"aspects",obj("alienis",16,"permutatio",16),"center",infusionIngredient(easy?"minecraft:ghast_tear":"minecraft:nether_star"),"components",tearParts.stream().map(WarpData::infusionIngredient).toList(),"result",obj("item","warptheory:pure_tear"))));
            jobs.add(save(cache,"data/warptheory/recipes/pure_talisman"+suffix+".json",obj("type","thaumcraft:infusion","conditions",conditions,"research","warptheory:WARPAMULET","instability",10,"aspects",obj("alienis",32,"permutatio",32,"praecantatio",64),"center",infusionIngredient("thaumcraft:mundane_amulet"),"components",amuletParts.stream().map(WarpData::infusionIngredient).toList(),"result",obj("item","warptheory:pure_talisman"))));
            for(String meat:List.of("chicken","beef","porkchop","cod"))jobs.add(save(cache,"data/warptheory/recipes/something_"+meat+suffix+".json",obj("type","thaumcraft:crucible","conditions",conditions,"research","warptheory:WARPSOMETHING","catalyst",ingredient("minecraft:"+meat),"aspects",easy?obj("alienis",8):obj("alienis",16,"permutatio",8),"result",obj("item","warptheory:something"))));
            jobs.add(save(cache,"data/warptheory/recipes/litmus_paper"+suffix+".json",obj("type","thaumcraft:arcane","conditions",conditions,"research","warptheory:WARPPAPER","mode","shapeless","vis",obj("aqua",easy?4:8),"ingredients",easy?List.of(ingredient("minecraft:paper"),ingredient("thaumcraft:salis_mundus")):List.of(ingredient("thaumcraft:salis_mundus"),ingredient("minecraft:paper"),ingredient("thaumcraft:salis_mundus")),"result",obj("item","warptheory:litmus_paper"))));
        }
        var cleanser=research("WARPCLEANSER","ELDRITCH","pure_tear",-3,-1,2,obj("alienis",6,"permutatio",3),List.of("ELDRITCHMAJOR"));
        var amulet=research("WARPAMULET","ELDRITCH","pure_talisman",-4,1,3,obj("alienis",6,"permutatio",3,"praecantatio",3),List.of("warptheory:WARPCLEANSER"));
        var chunk=research("WARPSOMETHING","ALCHEMY","something",-2,-5,1,obj("alienis",3,"corpus",3),List.of());chunk.put("isHidden",true);chunk.put("aspectTriggers",List.of("alienis","corpus","permutatio"));chunk.put("warp",2);
        var paper=research("WARPPAPER","ALCHEMY","litmus_paper",0,-5,2,obj("alienis",3,"praecantatio",3),List.of());paper.put("isHidden",true);paper.put("aspectTriggers",List.of("alienis","praecantatio","sensus"));
        jobs.add(save(cache,"data/warptheory/research_catalog/main.json",obj("format",1,"categories",List.of(obj("key","ELDRITCH","icon","thaumcraft:textures/misc/r_eldritch.png","background","thaumcraft:textures/gui/gui_researchbackeldritch.png","research",List.of(cleanser,amulet)),obj("key","ALCHEMY","icon","thaumcraft:textures/misc/r_crucible.png","background","thaumcraft:textures/gui/gui_researchback.png","research",List.of(chunk,paper))))));
        var pages=new ArrayList<Object>();
        pages.add(pages("WARPCLEANSER","warpcleanser","INFUSION_CRAFTING",List.of("pure_tear","pure_tear_easy")));
        pages.add(pages("WARPAMULET","warpamulet","INFUSION_CRAFTING",List.of("pure_talisman","pure_talisman_easy")));
        pages.add(pages("WARPPAPER","warppaper","ARCANE_CRAFTING",List.of("litmus_paper","litmus_paper_easy")));
        pages.add(pages("WARPSOMETHING","warpsomething","CRUCIBLE_CRAFTING",List.of("something_chicken","something_beef","something_porkchop","something_cod","something_chicken_easy","something_beef_easy","something_porkchop_easy","something_cod_easy")));
        jobs.add(save(cache,"data/warptheory/research_pages/main.json",obj("format",1,"entries",pages)));
        jobs.add(save(cache,"data/warptheory/research_page_recipes/main.json",obj("format",1,"entries",pageRecipes)));
        return CompletableFuture.allOf(jobs.toArray(CompletableFuture[]::new));
    }
    private static Map<String,Object> research(String key,String category,String item,int x,int y,int complexity,Map<String,Object> aspects,List<String> parents){return obj("key","warptheory:"+key,"category",category,"displayColumn",x,"displayRow",y,"complexity",complexity,"aspects",aspects,"parents",parents,"pageCount",2,"icon_item",obj("item","warptheory:"+item,"count",1));}
    private static Object pages(String key,String text,String type,List<String> recipes){return obj("key","warptheory:"+key,"pages",List.of(obj("type","TEXT","text","research.warptheory."+text),obj("type","warptheory:recipe","data",obj("recipe_type",type,"recipes",recipes.stream().filter(s->!s.endsWith("_easy")).map(s->"warptheory:"+s).toList()))));}
}
