package tc4revival.content;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

/** Data-pack infusion recipes; matching consumes one pedestal per listed component. */
public  class InfusionRecipe implements Recipe<InfusionRecipe.Input> {
    public static  class Input extends SimpleContainer {
        private final Predicate<String> research;
        private final boolean ordered;
        public Input(ItemStack center,List<ItemStack> components,Predicate<String> research) {
            this(center,components,research,false);
        }
        public Input(ItemStack center,List<ItemStack> components,Predicate<String> research,boolean ordered) {
            super(components.size()+1); setItem(0,center.copy());
            for (int index=0;index<components.size();index++) setItem(index+1,components.get(index).copy());
            this.research=research;
            this.ordered=ordered;
        }
        public Input snapshot(){
            var components=new ArrayList<ItemStack>();for(int i=1;i<getContainerSize();i++)components.add(getItem(i).copy());
            return new Input(getItem(0),components,research,ordered);
        }
        /** The crafting player's actual research predicate, also available to dynamic addon recipes. */
        public boolean researchComplete(String key) { return key.isEmpty() || research.test(key); }
    }
    public record Plan(List<InfusionIngredient> components,AspectAmounts aspects,int instability,int xp,boolean enchanting,InfusionOutput output) {
        public Plan { components=List.copyOf(components); aspects=aspects.copy(); }
        @Override public AspectAmounts aspects() { return aspects.copy(); }
    }
    private final ResourceLocation id;
    private final JsonObject definition;
    private final String research;
    private final int order,instability,baseXp;
    private final InfusionIngredient center;
    private final List<InfusionIngredient> components;
    private final AspectAmounts aspects=new AspectAmounts();
    private final InfusionOutput output;
    private final ItemStack display;
    private final Enchantment enchantment;
    private final boolean runic;
    protected InfusionRecipe(ResourceLocation id,JsonObject json) {
        this.id=id; definition=json.deepCopy(); research=GsonHelper.getAsString(json,"research","");
        runic=GsonHelper.getAsString(json,"mode","").equals("runic_augmentation");
        order=GsonHelper.getAsInt(json,"legacy_order",Integer.MAX_VALUE);
        instability=GsonHelper.getAsInt(json,"instability");
        if (instability<0) throw new IllegalArgumentException("Negative infusion instability: "+id);
        components=new ArrayList<>();
        for (var ingredient:GsonHelper.getAsJsonArray(json,"components")) components.add(new InfusionIngredient(ingredient.getAsJsonObject()));
        if (components.isEmpty()) throw new IllegalArgumentException("Infusion needs pedestal components: "+id);
        GsonHelper.getAsJsonObject(json,"aspects").entrySet().forEach(entry->{
            int quantity=entry.getValue().getAsInt(); if (quantity<0) throw new IllegalArgumentException("Negative infusion cost: "+id);
            aspects.add(TCAspects.catalog().require(entry.getKey()),quantity);
        });
        if (runic) {
            enchantment=null; baseXp=0; center=null; output=InfusionOutput.item(ItemStack.EMPTY); display=ItemStack.EMPTY;
        } else if (json.has("enchantment")) {
            var enchantmentId=ResourceLocation.parse(GsonHelper.getAsString(json,"enchantment"));
            enchantment=ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
            if (enchantment==null) throw new IllegalArgumentException("Unknown infusion enchantment: "+enchantmentId);
            baseXp=GsonHelper.getAsInt(json,"base_xp",Math.max(1,enchantment.getMinCost(1)/3));
            if (baseXp<1) throw new IllegalArgumentException("Invalid infusion XP: "+id);
            center=null; output=InfusionOutput.enchant(enchantmentId); display=ItemStack.EMPTY;
        } else {
            enchantment=null; baseXp=0; center=new InfusionIngredient(GsonHelper.getAsJsonObject(json,"center"));
            var result=GsonHelper.getAsJsonObject(json,"result");
            if (result.has("patch")) {
                try {
                    var patch=TagParser.parseTag(GsonHelper.getAsString(result,"patch"));
                    if (patch.size()!=1) throw new IllegalArgumentException("Infusion tag output must contain one key: "+id);
                    String key=patch.getAllKeys().iterator().next(); output=InfusionOutput.patch(key,patch.get(key)); display=ItemStack.EMPTY;
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) { throw new IllegalArgumentException("Invalid infusion output tag: "+id,failure); }
            } else {
                display=ShapedRecipe.itemStackFromJson(result);
                if (display.isEmpty() || display.getCount()>display.getMaxStackSize()) throw new IllegalArgumentException("Invalid infusion result: "+id);
                if (result.has("nbt")) try { display.setTag(TagParser.parseTag(GsonHelper.getAsString(result,"nbt"))); }
                catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) { throw new IllegalArgumentException("Invalid infusion result NBT: "+id,failure); }
                output=InfusionOutput.item(display);
            }
        }
    }
    @Override public boolean matches(Input input,Level level) {
        if(!enabled())return false;
        if (!research.isEmpty() && !input.research.test(research)) return false;
        var central=input.getItem(0);
        if(!acceptsCenter(central))return false;
        var remaining=new ArrayList<ItemStack>();
        for (int slot=1;slot<input.getContainerSize();slot++) if (!input.getItem(slot).isEmpty()) remaining.add(input.getItem(slot));
        List<InfusionIngredient> requiredComponents;
        try { requiredComponents=enchantment!=null?plan(input).components():runic ? RunicAugmentation.components(central) : components; }
        catch(IllegalArgumentException|ArithmeticException invalid) {return false;}
        if (remaining.size()!=requiredComponents.size()) return false;
        if(input.ordered)return InfusionOrder.cyclicMatches(requiredComponents,remaining,InfusionIngredient::matches);
        return InfusionMatching.assignment(remaining,requiredComponents,false)!=null;
    }
    public boolean acceptsCenter(ItemStack central) {
        if (runic) { if (!RunicAugmentation.eligible(central)) return false; }
        else if (enchantment==null) { if (!center.matches(central)) return false; }
        else {
            if (central.isEmpty() || central.getCount()!=1
                    || !enchantment.canEnchant(central)) return false;
            for (var entry:EnchantmentHelper.getEnchantments(central).entrySet()) {
                if (entry.getKey()==enchantment) { if (entry.getValue()>=(thaumcraft.api.crafting.InfusionEnchantmentLimits.maximum(enchantment,enhancedEnchanting()))) return false; }
                else {
                    if (!enchantment.isCompatibleWith(entry.getKey()) || !entry.getKey().isCompatibleWith(enchantment)) return false;
                }
            }
        }
        return true;
    }
    public Plan plan(ItemStack central) {
        if (runic) return RunicAugmentation.plan(central);
        if (enchantment==null) return new Plan(components,aspects,instability,0,false,output);
        if(enhancedEnchanting())return enhancedPlan(central,1);
        int targetLevel=EnchantmentHelper.getItemEnchantmentLevel(enchantment,central),levels=0;
        float multiplier=targetLevel;
        for (var entry:EnchantmentHelper.getEnchantments(central).entrySet()) {
            levels=Math.addExact(levels,entry.getValue());
            if (entry.getKey()!=enchantment) multiplier+=entry.getValue()*.1f;
        }
        var costs=aspects.copy();
        for (var aspect:costs.aspects()) {
            double extra=costs.amount(aspect)*multiplier;
            if (!Double.isFinite(extra) || extra>Integer.MAX_VALUE-costs.amount(aspect)) throw new IllegalArgumentException("Infusion cost overflow: "+id);
            costs.add(aspect,(int)extra);
        }
        var required=new ArrayList<>(components); if(thaumcraft.api.crafting.InfusionEnchantmentLimits.requiresVoidSeed(enchantment,targetLevel+1,false))required.add(ingredient("void_seed")); return new Plan(required,costs,Math.addExact(instability,levels/2),Math.multiplyExact(baseXp,targetLevel+1),true,output);
    }
    /** Called once when the altar starts a real craft. Addons may realize a random
     * output here; previews must remain deterministic in {@link #plan(ItemStack)}.
     * The returned output is persisted by the infusion process across saves. */
    public Plan planForCraft(ItemStack central, Level level) { return plan(central); }
    /** Input-aware overload retains existing addon overrides for ordinary recipes. */
    public Plan planForCraft(Input input,Level level) {return enhancedEnchanting()?plan(input):planForCraft(input.getItem(0),level);}
    public Plan plan(Input input) {
        if(!enhancedEnchanting())return plan(input.getItem(0));
        int salt=0;
        for(int slot=1;slot<input.getContainerSize();slot++)if(input.getItem(slot).is(item("salis_mundus")))salt++;
        return enhancedPlan(input.getItem(0),salt);
    }
    private boolean enhancedEnchanting() {return enchantment!=null && tc4revival.config.TCConfig.SERVER.enhancedInfusionEnchanting.get();}
    private int enhancedMaximum() {
        int max=enchantment.getMaxLevel();
        return thaumcraft.api.crafting.InfusionEnchantmentLimits.maximum(enchantment,true);
    }
    private static net.minecraft.world.item.Item item(String path) {
        return java.util.Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:"+path)));
    }
    private static InfusionIngredient ingredient(String path) {
        var root=new JsonObject();var item=new JsonObject();item.addProperty("item","thaumcraft:"+path);root.add("ingredient",item);
        return new InfusionIngredient(root);
    }
    private Plan enhancedPlan(ItemStack central,int increments) {
        int before=EnchantmentHelper.getItemEnchantmentLevel(enchantment,central);
        if(increments<1 || increments>256 || (long)before+increments>enhancedMaximum())
            throw new IllegalArgumentException("Infusion enchantment level exceeds limit: "+id);
        var salt=new ItemStack(item("salis_mundus"));
        var required=new ArrayList<InfusionIngredient>();boolean inserted=false;
        for(var component:components) {
            if(component.matches(salt)) {
                if(!inserted)for(int i=0;i<increments;i++)required.add(ingredient("salis_mundus"));
                inserted=true;
            }else required.add(component);
        }
        if(!inserted)for(int i=0;i<increments;i++)required.add(ingredient("salis_mundus"));
        if(thaumcraft.api.crafting.InfusionEnchantmentLimits.requiresVoidSeed(enchantment,before+increments,true))required.add(ingredient("void_seed"));
        var costs=new AspectAmounts();
        for(var aspect:aspects.aspects())costs.add(aspect,Math.multiplyExact(aspects.amount(aspect),increments));
        int levels=EnchantmentHelper.getEnchantments(central).values().stream().mapToInt(Integer::intValue).sum();
        return new Plan(required,costs,Math.addExact(instability,levels/2),0,true,
                InfusionOutput.enchant(ForgeRegistries.ENCHANTMENTS.getKey(enchantment),increments));
    }
    public JsonObject displayDefinition() { return definition.deepCopy(); }
    public boolean enchantmentRecipe(){return enchantment!=null;}
    public record EnchantmentPreview(int before,int after,Plan plan,ItemStack book) {}
    /** Equipment-independent reference rows. Enhanced mode includes direct multilevel application
     * and the last single-level step; at most twice the level limit, regardless of registry size. */
    public List<EnchantmentPreview> enchantmentPreviews(){
        if(enchantment==null)return List.of();
        int maximum=thaumcraft.api.crafting.InfusionEnchantmentLimits.maximum(enchantment,enhancedEnchanting());
        var rows=new ArrayList<EnchantmentPreview>();
        for(int after=1;after<=maximum;after++){
            rows.add(enchantmentPreview(enhancedEnchanting()?0:after-1,after));
            if(enhancedEnchanting()&&after>1)rows.add(enchantmentPreview(after-1,after));
        }
        return List.copyOf(rows);
    }
    private EnchantmentPreview enchantmentPreview(int before,int after){
        // An internal level carrier, never an eligible-item search or a displayed recipe ingredient.
        var carrier=new ItemStack(net.minecraft.world.item.Items.PAPER);
        if(before>0)carrier.enchant(enchantment,before);
        var plan=enhancedEnchanting()?enhancedPlan(carrier,after-before):plan(carrier);
        var book=net.minecraft.world.item.EnchantedBookItem.createForEnchantment(new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment,after));
        return new EnchantmentPreview(before,after,plan,book);
    }
    public boolean dynamicCenter() { return center==null; }
    public List<ItemStack> centerSamples() { return center==null?List.of():center.displayStacks(); }
    public boolean enabled() {
        return thaumcraft.api.research.ResearchCategories.enabled(research)
                && (!java.util.Set.of("MIRROR","MIRRORHAND","MIRRORESSENTIA").contains(research) || tc4revival.config.TCConfig.SERVER.allowMirrors.get());
    }
    public static Comparator<InfusionRecipe> priority() {
        return Comparator.<InfusionRecipe>comparingInt(recipe->recipe.enchantment!=null?2:recipe.runic?1:0)
                .thenComparingInt(recipe->recipe.order).thenComparing(recipe->recipe.id.toString());
    }
    public String research() { return research; }
    public static Optional<InfusionRecipe> find(Level level,Input input) {
        var supplied=thaumcraft.api.crafting.MagicRecipeProviders.infusion(level,input);if(supplied.isPresent())return supplied;
        return level.getRecipeManager().getAllRecipesFor(TCArcane.INFUSION_RECIPE_TYPE.get()).stream().filter(recipe->recipe.matches(input,level))
                .sorted(priority()).findFirst();
    }
    @Override public ItemStack assemble(Input input,RegistryAccess registries) { return (enhancedEnchanting()?plan(input).output():runic ? plan(input.getItem(0)).output() : output).apply(input.getItem(0)); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return display.copy(); }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=components.size()+1; }
    @Override public boolean isSpecial() { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeType<?> getType() { return TCArcane.INFUSION_RECIPE_TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return TCArcane.INFUSION_RECIPE_SERIALIZER.get(); }
    public static  class Serializer implements RecipeSerializer<InfusionRecipe> {
        @Override public InfusionRecipe fromJson(ResourceLocation id,JsonObject json) { return new InfusionRecipe(id,json); }
        @Override public InfusionRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) { return fromJson(id,com.google.gson.JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject()); }
        @Override public void toNetwork(FriendlyByteBuf buffer,InfusionRecipe recipe) { buffer.writeUtf(recipe.definition.toString(),1048576); }
    }
}
