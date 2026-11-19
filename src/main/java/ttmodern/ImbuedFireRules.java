package ttmodern;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class ImbuedFireRules extends SimpleJsonResourceReloadListener {
    public record Rule(Block from,Block to,boolean cake) {}
    private static volatile Map<String,List<Rule>> rules=Map.of();
    public ImbuedFireRules() { super(new Gson(),"imbued_fire"); }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void reload(net.minecraftforge.event.AddReloadListenerEvent event) { event.addListener(new ImbuedFireRules()); }
    @Override protected void apply(Map<ResourceLocation,JsonElement> data,ResourceManager manager,ProfilerFiller profiler) {
        var next=new LinkedHashMap<String,List<Rule>>();
        data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file->{
            var root=file.getValue().getAsJsonObject();String aspect=root.get("aspect").getAsString();if(!Tinkerer.FIRES.containsKey(aspect))throw new IllegalArgumentException("Unknown fire "+aspect);
            var rows=next.computeIfAbsent(aspect,k->new ArrayList<>());
            for(var value:root.getAsJsonArray("entries")) {var row=value.getAsJsonObject();var from=block(row.get("from").getAsString());var to=block(row.get("to").getAsString());if(rows.stream().anyMatch(r->r.from()==from))throw new IllegalArgumentException("Duplicate fire input "+from);rows.add(new Rule(from,to,row.has("cake")&&row.get("cake").getAsBoolean()));}
        });
        var frozen=new LinkedHashMap<String,List<Rule>>();next.forEach((k,v)->frozen.put(k,List.copyOf(v)));rules=Map.copyOf(frozen);
    }
    private static Block block(String text) { var id=ResourceLocation.parse(text);if(!ForgeRegistries.BLOCKS.containsKey(id))throw new IllegalArgumentException("Unknown fire block "+id);return ForgeRegistries.BLOCKS.getValue(id); }
    public static BlockState transform(String aspect,ServerLevel world,BlockPos pos) {
        var state=world.getBlockState(pos);
        if(state.getDestroySpeed(world,pos)<0||state.is(Blocks.WATER)&&!state.getFluidState().isSource())return null;
        for(var rule:rules.getOrDefault(aspect,List.of()))if(state.is(rule.from())&&(!rule.cake()||TinkererConfig.FIRE_CAKE.get()))return rule.to().defaultBlockState();
        return aspect.equals("ordo")?ordered(world,pos,state):null;
    }
    public static boolean result(String aspect,BlockState state) { return rules.getOrDefault(aspect,List.of()).stream().anyMatch(r->state.is(r.to())); }
    private static BlockState ordered(ServerLevel world,BlockPos pos,BlockState state) {
        if(state.hasBlockEntity()||state.isAir()||state.is(Blocks.STONE)||state.is(Blocks.GLASS)||state.is(Blocks.SAND)||state.is(Blocks.GRAVEL))return null;
        // Old ore dictionary pairs become standard Forge ore/storage block tags.
        var tags=state.getTags().map(net.minecraft.tags.TagKey::location).filter(id->id.getNamespace().equals("forge")&&id.getPath().startsWith("ores/")).sorted().toList();
        for(var tag:tags) {
            var key=net.minecraft.tags.BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge","storage_blocks/"+tag.getPath().substring(5)));
            var choices=ForgeRegistries.BLOCKS.getValues().stream().filter(b->b.defaultBlockState().is(key)&&b!=state.getBlock()&&!b.defaultBlockState().hasBlockEntity()).sorted(Comparator.comparing(b->ForgeRegistries.BLOCKS.getKey(b))).toList();
            if(!choices.isEmpty())return choices.get(0).defaultBlockState();
        }
        var drops=Block.getDrops(state,world,pos,null,null,new ItemStack(Items.DIAMOND_PICKAXE));
        if(drops.isEmpty()||drops.stream().anyMatch(s->!ItemStack.isSameItemSameTags(s,drops.get(0))))return null;
        var ingredient=drops.get(0).copy();ingredient.setCount(1);var result=compressed(world,ingredient);
        if(result.isEmpty()) {
            var inventory=new net.minecraft.world.SimpleContainer(ingredient);var smelting=world.getRecipeManager().getRecipeFor(RecipeType.SMELTING,inventory,world);
            if(smelting.isPresent())result=compressed(world,smelting.get().assemble(inventory,world.registryAccess()));
        }
        if(result.getItem() instanceof BlockItem item&&item.getBlock()!=state.getBlock()&&!item.getBlock().defaultBlockState().hasBlockEntity())return item.getBlock().defaultBlockState();
        return null;
    }
    private static ItemStack compressed(ServerLevel world,ItemStack input) {
        if(input.isEmpty())return ItemStack.EMPTY;
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,-1){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return false;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);for(int i=0;i<9;i++){var copy=input.copy();copy.setCount(1);grid.setItem(i,copy);}
        return world.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,world).map(r->r.assemble(grid,world.registryAccess())).orElse(ItemStack.EMPTY);
    }
}
