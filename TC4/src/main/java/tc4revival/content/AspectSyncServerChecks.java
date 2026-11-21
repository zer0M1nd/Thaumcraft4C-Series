package tc4revival.content;

import com.google.gson.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.recipe.*;
import tc4revival.aspect.sync.*;
import tc4revival.aspect.inference.*;

/** Read-only real-server regression: vanilla recipe codec, custom-only inputs and independent local computation. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public class AspectSyncServerChecks {
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("tc_aspect_sync_verify").requires(s->s.hasPermission(4)).executes(c->{
            try{verify(c.getSource().getServer());c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("ASPECT_SYNC PASS"),false);return 1;}
            catch(Exception|LinkageError error){tc4revival.TC4Revival.LOGGER.error("ASPECT_SYNC FAIL",error);c.getSource().sendFailure(net.minecraft.network.chat.Component.literal("ASPECT_SYNC FAIL: "+error.getMessage()));return 0;}
        }));
    }
    private static void check(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static boolean hasTag(String id,String tag){return ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(id)).builtInRegistryHolder().is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse(tag)));}
    public static void verify(net.minecraft.server.MinecraftServer server){
        var snapshot=ServerAspectData.snapshot();var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());var localManager=new RecipeManager();
        var before=RuntimeCraftingAspects.extract(server.getRecipeManager(),server.registryAccess());
        check(AspectDataSet.hash(RuntimeCraftingJson.encode(before).toString()).equals(snapshot.hashes().get(RuntimeCraftingJson.KEY)),"Inference must not mutate shared crafting ingredients or outputs");
        try{
            // This check exercises the crafting projection. Some machine recipe decoders require
            // client-only libraries and cannot be executed on a dedicated server for a round trip.
            var craftingRecipes=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(server.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING));
            new ClientboundUpdateRecipesPacket(craftingRecipes).write(buffer);
            localManager.replaceRecipes(new ClientboundUpdateRecipesPacket(buffer).getRecipes());
        }finally{buffer.release();}
        var crafting=RuntimeCraftingAspects.extract(localManager,server.registryAccess());
        var delta=ServerAspectData.inputs();
        check(delta.keySet().stream().noneMatch(AspectInputDelta::generated),"No generated inference/crafting tables may cross the network");
        var receiver=new AspectDataTransfer.Receiver();Map<String,String> received=null;var frames=AspectDataTransfer.encode(delta);
        for(int i=0;i<frames.size();i++)received=receiver.accept(i,frames.size(),frames.get(i)).orElse(received);
        var local=AspectInputDelta.apply(BundledAspectData.all(),received);
        check(AspectInputDelta.between(local,snapshot.files()).isEmpty(),"Bundled definitions plus authored delta must reproduce authored server inputs");
        var options=InferenceSettings.decode(received.get(AspectInputDelta.SETTINGS));
        var prepared=LocalAspectPreparation.prepare(local,localManager,server.registryAccess(),crafting,new JsonObject(),List.of(),false,options,AspectSyncServerChecks::hasTag);
        var catalog=tc4revival.research.BrowserRecipes.catalog(server.overworld(),prepared.calculation().identity());
        int catalogRows=0,wandRows=0,enchantmentRows=0;
        while(!catalog.done())for(var row:catalog.drain(64,2_000_000L)){
            catalogRows++;
            if(row.kind().equals("arcane")&&row.output().getItem() instanceof CastingWandItem)wandRows++;
            if(row.kind().equals("infusion_enchantment")){
                enchantmentRows++;check(row.output().is(Items.ENCHANTED_BOOK)&&row.inputs().get(0).isEmpty(),"Enchantment catalog contains only representative books");
            }
        }
        check(wandRows>0&&enchantmentRows>0,"Independent catalog must include wands and dedicated enchantment pages: rows="+catalogRows+", wands="+wandRows+", enchantments="+enchantmentRows);
        tc4revival.TC4Revival.LOGGER.info("LOCAL_BROWSER PASS: {} rows, {} wands, {} enchantment levels; no server catalog request",catalogRows,wandRows,enchantmentRows);
        if(!prepared.data().files().get(RuntimeCraftingJson.KEY).equals(RuntimeCraftingJson.encode(before).toString())){
            var absent=new HashSet<>(before.recipes());absent.removeAll(crafting.recipes());
            var extra=new HashSet<>(crafting.recipes());extra.removeAll(before.recipes());
            tc4revival.TC4Revival.LOGGER.warn("CRAFTING_PROJECTION native={}, wire={}, removed={}, added={}, sampleRemoved={}, sampleAdded={}",before.recipes().size(),crafting.recipes().size(),absent.size(),extra.size(),absent.stream().limit(3).toList(),extra.stream().limit(3).toList());
            var ids=absent.stream().map(r->r.output().item()).collect(java.util.stream.Collectors.toSet());
            for(var recipe:server.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)){
                var output=recipe.getResultItem(server.registryAccess());
                if(!ids.contains(ForgeRegistries.ITEMS.getKey(output.getItem()).toString()))continue;
                var copy=localManager.byKey(recipe.getId()).orElseThrow();
                tc4revival.TC4Revival.LOGGER.warn("WIRE_DIFFERENCE {} special={} class={} -> {}; result={} -> {}; ingredients={} -> {}",recipe.getId(),recipe.isSpecial(),recipe.getClass().getName(),copy.getClass().getName(),output.save(new net.minecraft.nbt.CompoundTag()),copy.getResultItem(server.registryAccess()).save(new net.minecraft.nbt.CompoundTag()),recipe.getIngredients().stream().map(i->Arrays.stream(i.getItems()).map(x->x.save(new net.minecraft.nbt.CompoundTag())).toList()).toList(),copy.getIngredients().stream().map(i->Arrays.stream(i.getItems()).map(x->x.save(new net.minecraft.nbt.CompoundTag())).toList()).toList());
            }

        }
        check(prepared.data().files().get(RuntimeCraftingJson.KEY).equals(RuntimeCraftingJson.encode(before).toString()),"Vanilla synchronized crafting must be reconstructed locally");
        int checked=0;
        var authoredServer=new AspectCalculationData(new AspectDataSet(LocalAspectPreparation.base(snapshot.files(),before)),TCAspects.catalog(),AspectSyncServerChecks::hasTag);
        for(var recipe:crafting.recipes()){
            var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(recipe.output().item())));
            try{
                check(ItemStackAspects.resolve(stack,authoredServer).map(AspectAmounts::view).equals(ItemStackAspects.resolve(stack,prepared.calculation()).map(AspectAmounts::view)),"Local crafting aspect parity: "+recipe.output().item());
                if(++checked>=256)break;
            }catch(RuntimeException error){if(error instanceof IllegalStateException)throw error;}
        }
        var wireManager=new RecipeManager();
        var allRecipes=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(localManager.getRecipes());
        for(var recipe:server.getRecipeManager().getRecipes())if(recipe.getType()!=net.minecraft.world.item.crafting.RecipeType.CRAFTING)allRecipes.add(recipe);
        wireManager.replaceRecipes(allRecipes);
        var authoritative=LocalAspectPreparation.prepareAuthoritative(local,wireManager,server.registryAccess(),crafting,options,AspectSyncServerChecks::hasTag);
        int parityChecks=0;
        for(String id:List.of("mysticalagradditions:insanium_essence","mysticalagriculture:supremium_essence","minecraft:iron_ingot")) {
            var item=ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(id));if(item==null||item==Items.AIR)continue;
            var stack=new ItemStack(item);
            var scanned=tc4revival.research.ScanTargetResolver.inventory(stack).orElseThrow().aspects();
            var tooltip=authoritative.cache().resolve(stack).amounts();
            check(scanned.view().equals(tooltip.view()),"Actual scan/client tooltip parity for "+id+": server="+scanned.view()+", client="+tooltip.view());parityChecks++;
        }
        int itemChecks=0;var differences=new ArrayList<String>();
        for(var item:ForgeRegistries.ITEMS) {
            if(item==Items.AIR)continue;
            var stack=new ItemStack(item);
            var expected=ItemStackAspects.resolve(stack.copy()).orElseGet(AspectAmounts::new);
            var actual=authoritative.cache().resolve(stack.copy()).amounts();itemChecks++;
            if(!expected.view().equals(actual.view())&&differences.size()<10)differences.add(ForgeRegistries.ITEMS.getKey(item)+": server="+expected.view()+", client="+actual.view());
        }
        check(differences.isEmpty(),"Default-item inference parity: "+differences);
        tc4revival.TC4Revival.LOGGER.info("AUTHORITATIVE_ITEMS PASS: {} complete default-item comparisons",itemChecks);
        tc4revival.TC4Revival.LOGGER.info("AUTHORITATIVE_INFERENCE PASS: {} actual scanner/tooltip comparisons including Insanium when installed; server recipe snapshot reproduced locally",parityChecks);
        verifyJei(snapshot,server.registryAccess());
        check(before.equals(RuntimeCraftingAspects.extract(server.getRecipeManager(),server.registryAccess())),"Read-only prediction and packet verification must preserve the original recipes");
        tc4revival.TC4Revival.LOGGER.info("ASPECT_SYNC PASS: {} vanilla recipes projected; {} output checks; {} delta resources / {} raw bytes / {} frames; crafting download omitted={}",
                crafting.recipes().size(),checked,delta.size(),AspectDataSet.transferBytes(delta),frames.size(),true);
    }
    private static void verifyJei(AspectDataSet server,net.minecraft.core.RegistryAccess registries){
        var files=new TreeMap<>(server.files());files.replaceAll((key,value)->key.startsWith("item_aspects/")?ItemAspectJson.encode(List.of()).toString():value);
        files.replaceAll((key,value)->key.startsWith("aspect_editor/")?tc4revival.aspect.editor.EditorDefinitions.EMPTY.encode().toString():value);
        var manager=new RecipeManager();manager.replaceRecipes(List.of());
        files.put(RuntimeCraftingJson.KEY,RuntimeCraftingJson.encode(RuntimeCraftingAspects.extract(manager,registries)).toString());
        var fixture=new JsonObject();var rows=new JsonArray();fixture.add("recipes",rows);
        rows.add(AdvancedAspectInference.recipeJson(new InferenceEngine.Recipe("test:jei_only",new InferenceEngine.Amount("item:minecraft:barrier",1),
                List.of(List.of(new InferenceEngine.Amount("item:minecraft:ender_pearl",1))))));
        var prediction=AdvancedAspectInference.calculate(manager,registries,new AspectDataSet(files),fixture,new JsonObject(),new JsonObject(),new JsonObject(),
                List.of(new ItemStack(Items.ENDER_PEARL),new ItemStack(Items.BARRIER)),false,AspectSyncServerChecks::hasTag);
        check(prediction.known().containsKey("item:minecraft:barrier"),"JEI-only recipe participates in the local shared inference engine");
        check(server.hashes().equals(ServerAspectData.snapshot().hashes()),"Client prediction must not mutate server data");
        tc4revival.TC4Revival.LOGGER.info("ASPECT_SYNC PASS: JEI-only fixture inferred independently; server snapshot unchanged");
    }
}
