package tbmodern.acceptance;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tbmodern.*;
import tbmodern.api.RevolverUpgrades;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.content.*;
import tc4revival.research.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.nodes.NodeApi;

/** Included only with -Pacceptance; never exported in the playable or sources JAR. */
@Mod.EventBusSubscriber(modid="thaumicbases")
public final class ServerAcceptance {
    private static final Map<String,String> RESULTS=new LinkedHashMap<>();
    private static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
    private static void check(String name,Runnable work) {
        try {work.run();RESULTS.put(name,"passed");}
        catch(Throwable failure) {RESULTS.put(name,failure.toString());failure.printStackTrace();}
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!Boolean.getBoolean("thaumicbases.acceptance"))return;
        var server=event.getServer();var level=server.overworld();var origin=new BlockPos(0,100,0);
        long originalTime=level.getGameTime();
        try {
            level.getChunkAt(origin);
            check("registered_items_have_scan_values",()->{
                require(Content.ITEM_IDS.size()==157,"Unexpected item count: "+Content.ITEM_IDS.size());
                Content.ITEM_IDS.forEach((id,item)->require(ItemStackAspects.resolve(new ItemStack(item.get())).isPresent(),"No aspect entry: "+id));
            });
            check("bracelets_capacity_cost_and_focus_protocol",()->{
                var air=tc4revival.aspect.TCAspects.catalog().require("aer");
                for(var definition:Bracelet.definitions()) {
                    var stack=new ItemStack(Content.item("bracelet_"+definition.id()));var bracelet=(Bracelet)stack.getItem();
                    require(bracelet.isStaff(stack)&&!bracelet.isSceptre(stack),"Bracelet identity");
                    require(bracelet.capacity(stack)==definition.capacity()*100,"Bracelet capacity");
                    bracelet.addVis(stack,air,99999,false,true);
                    require(bracelet.vis(stack,air)==bracelet.capacity(stack),"Charge exceeds bracelet capacity");
                    require(bracelet.focusCooldownMillis(stack,ItemStack.EMPTY,501)==250 && bracelet.continuousFocusCalls(stack,ItemStack.EMPTY)==2,"Focus timing");
                    require(Math.abs(bracelet.consumptionModifier(stack,null,air,true)-Math.max(.1F,bracelet.cap(stack).cost("aer")/1.5F))<.0001,"Consumption modifier");
                    require(server.getRecipeManager().byKey(new ResourceLocation("thaumicbases","equipment/"+definition.id()+"braceletrec")).isPresent(),"Missing bracelet recipe");
                    require(ResearchCatalogs.catalog().find("TB.Bracelet."+Character.toUpperCase(definition.id().charAt(0))+definition.id().substring(1)).isPresent(),"Missing bracelet research");
                }
                boolean previous=Configuration.BRACELETS.get();
                try {Configuration.BRACELETS.set(false);require(!thaumcraft.api.research.ResearchCategories.enabled("TB.Bracelet.Iron"),"Bracelet module gate");}
                finally {Configuration.BRACELETS.set(previous);}
            });
            check("addon_enchantments_contribute_magic",()->{
                var magic=tc4revival.aspect.TCAspects.catalog().require("praecantatio");
                for(var entry:tbmodern.Enchantments.ENTRIES.values()) {
                    var plain=new ItemStack(Items.IRON_SWORD);var enchanted=plain.copy();enchanted.enchant(entry.get(),3);
                    require(ItemStackAspects.resolve(enchanted).orElseThrow().amount(magic)>ItemStackAspects.resolve(plain).orElseThrow().amount(magic),"Unknown addon enchantment ignored");
                    var book=EnchantedBookItem.createForEnchantment(new net.minecraft.world.item.enchantment.EnchantmentInstance(entry.get(),3));
                    require(ItemStackAspects.resolve(book).orElseThrow().amount(magic)>0,"Enchanted book ignored");
                }
            });
            check("golem_crop_rules_maturity_and_replant",()->{
                var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
                for(var crop:Content.PLANTS)if(!crop.id().equals("ashroom")) {
                    var pos=origin.offset(5,0,0);var block=Content.block(crop.id());
                    level.setBlockAndUpdate(pos.below(),crop.soil().equals("crystal")?Content.block("crystal_air").defaultBlockState():crop.soil().equals("grass")?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.FARMLAND.defaultBlockState());
                    level.setBlockAndUpdate(pos.below(2),Content.block("salis_mundus_block").defaultBlockState());
                    var young=block.defaultBlockState();var ripe=young.setValue(PlantBlock.AGE,crop.stages()-1);
                    var rule=thaumcraft.api.golems.GolemCropRegistry.find(ripe).orElseThrow();
                    level.setBlockAndUpdate(pos,ripe);require(!rule.mature(level,pos,young)&&rule.mature(level,pos,ripe),"Maturity "+crop.id());
                    require(rule.harvest(new thaumcraft.api.golems.GolemCropRegistry.Context(null,level,player,pos,ripe,true)),"Harvest "+crop.id());
                    require(level.getBlockState(pos).equals(young),"Replant "+crop.id());
                    level.setBlockAndUpdate(pos,ripe);require(rule.harvest(new thaumcraft.api.golems.GolemCropRegistry.Context(null,level,player,pos,ripe,false)),"Harvest without upgrade");
                    require(level.isEmptyBlock(pos),"Replanted without order upgrade");
                }
                var pos=origin.offset(6,0,0);var block=Content.block("briar");level.setBlockAndUpdate(pos.below(),Blocks.GRASS_BLOCK.defaultBlockState());
                var bottom=block.defaultBlockState().setValue(SpecialPlants.Briar.AGE,7);
                var top=bottom.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
                level.setBlock(pos,bottom,2);level.setBlock(pos.above(),top,2);
                var rule=thaumcraft.api.golems.GolemCropRegistry.find(bottom).orElseThrow();
                require(rule.mature(level,pos,bottom)&&!rule.mature(level,pos.above(),top),"Briar single harvest target");
                require(rule.harvest(new thaumcraft.api.golems.GolemCropRegistry.Context(null,level,player,pos,bottom,true)),"Briar harvest");
                require(level.getBlockState(pos).equals(block.defaultBlockState())&&level.getBlockState(pos.above()).is(block),"Briar pair replant");
            });
            check("harvest_core_dispatch_and_protection",()->{
                var pos=origin.offset(8,0,0);level.setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());
                var ripe=Content.block("plax").defaultBlockState().setValue(PlantBlock.AGE,7);
                var golem=TCEntities.GOLEM.get().create(level);golem.moveTo(pos.getX(),pos.getY(),pos.getZ(),0,0);
                var config=new GolemConfiguration(GolemMaterial.THAUMIUM,true);config.installCore(GolemCoreType.HARVEST);config.installUpgrade(4);
                golem.state().configure(config);golem.state().home(pos,Direction.UP);golem.refreshConfiguration();
                java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> deny=eventBlock->{if(eventBlock.getPos().equals(pos))eventBlock.setCanceled(true);};
                level.setBlockAndUpdate(pos,ripe);net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(deny);
                try {GolemCropHarvest.harvest(golem,pos,ripe);require(level.getBlockState(pos).equals(ripe),"Harvest bypassed protection");}
                finally {net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny);}
                require(GolemCropHarvest.mature(level,pos),"Original harvest core cannot see addon crop");
                GolemCropHarvest.harvest(golem,pos,ripe);require(level.getBlockState(pos).equals(Content.block("plax").defaultBlockState()),"Original harvest core did not replant");
                golem.discard();
            });
            check("research_catalog",()->{
                long count=ResearchCatalogs.catalog().all().stream().filter(r->r.key().startsWith("TB.") || r.key().equals("ROD_tbthaumium") || r.key().equals("ROD_tbvoid") || r.key().equals("CAP_thauminite")).count();
                require(count>=90,"Addon research missing: "+count);
                require(ResearchCatalogs.catalog().find("TB.NodeFoci.Bright").isPresent(),"Brightness research missing");
            });
            check("recipes_and_custom_ingredients",()->{
                long count=server.getRecipeManager().getRecipes().stream().filter(r->r.getId().getNamespace().equals("thaumicbases")).count();
                require(count>=180,"Addon recipe loading incomplete: "+count);
                var gun=new ItemStack(Content.item("revolver"));var match=new RevolverIngredient("thaumicbases:power",0);
                require(match.test(gun),"Fresh gun rejected");require(RevolverUpgrades.upgrade(gun,"thaumicbases:power"),"Upgrade failed");
                require(!match.test(gun),"Previous level still accepted");require(!RevolverUpgrades.canUpgrade(gun,"thaumicbases:silver"),"Conflict ignored");
                gun.getOrCreateTag().putInt("Shots",7);gun.setDamageValue(23);
                require(RevolverUpgrades.upgrade(gun,"thaumicbases:heavy"),"Compatible upgrade rejected");
                require(gun.getDamageValue()==23 && gun.getTag().getInt("Shots")==7,"Upgrade erased weapon state");
            });
            check("wand_part_registration",()->{
                require(WandParts.Rod.require("thaumicbases:tbthaumium").capacity==80,"Thaumium rod capacity");
                require(WandParts.Rod.require("thaumicbases:tbvoid").capacity==160,"Void rod capacity");
                require(!thaumcraft.api.ThaumcraftApi.createWand("thaumicbases:tbvoid","thaumicbases:thauminite",false,true).isEmpty(),"Wand assembly failed");
            });
            check("placed_block_scan",()->{
                level.setBlock(origin,Content.block("crystal_air").defaultBlockState(),3);
                var value=ScanTargetResolver.block(level,origin).orElseThrow().aspects();
                require(value.total()>0,"Placed crystal lost its aspects");
                level.setBlock(origin,Content.block("pyrofluid").defaultBlockState(),3);
                require(ScanTargetResolver.block(level,origin).orElseThrow().aspects().amount(Aspect.FIRE.unwrap())==16,"Fluid scan callback missing");
            });
            check("node_permanent_brightness_and_reload",()->{
                level.setBlock(origin,TCArcane.AURA_NODE.get().defaultBlockState(),3);
                var node=(AuraNodeBlockEntity)level.getBlockEntity(origin);
                NodeApi.configure(node,new AspectList().add(Aspect.AIR,40),AuraNodeType.NORMAL,null,false);
                var pos=origin.above();level.setBlock(pos,Content.block("node_manipulator").defaultBlockState(),3);
                var machine=(NodeManipulator)level.getBlockEntity(pos);machine.setMode("brightness");
                int seconds=NodeManipulator.transformation("brightness",AuraNodeType.NORMAL,null).ticks()/20;
                for(int i=0;i<seconds;i++) {
                    ((ServerLevelData)level.getLevelData()).setGameTime(13+20L*i);
                    NodeManipulator.tick(level,pos,level.getBlockState(pos),machine);
                    if(i==seconds/2)machine.load(machine.saveWithoutMetadata());
                }
                require(node.permanentModifier()==AuraNodeModifier.BRIGHT,"Node did not become permanently bright");
                var tag=machine.saveWithoutMetadata();var restored=new NodeManipulator(pos,level.getBlockState(pos));restored.load(tag);
                require(restored.mode().equals("brightness"),"Mode not persisted");
                level.removeBlock(pos,false);
            });
            check("advanced_furnace_consumes_and_persists",()->{
                level.setBlock(origin,Content.block("advanced_alchemical_furnace").defaultBlockState(),3);
                var furnace=(AdvancedFurnace)level.getBlockEntity(origin);
                furnace.setItem(0,new ItemStack(Content.item("plax_seeds")));furnace.setItem(1,new ItemStack(Items.COAL));
                for(int i=0;i<80;i++)AdvancedFurnace.tick(level,origin,level.getBlockState(origin),furnace);
                require(furnace.getItem(0).isEmpty() && furnace.vis()>0,"Furnace did not distill addon seed");
                var restored=new AdvancedFurnace(origin,level.getBlockState(origin));restored.load(furnace.saveWithoutMetadata());
                require(restored.vis()==furnace.vis(),"Stored essentia lost on reload");
            });
            check("overchanter_swap_resets_progress",()->{
                level.setBlock(origin,Content.block("overchanter").defaultBlockState(),3);var tile=(Overchanter)level.getBlockEntity(origin);
                var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,5);
                tile.setItem(0,sword);require(tile.start(),"Overchanter refused valid input");
                require(!tile.canTakeItemThroughFace(0,sword,Direction.DOWN),"Automation removed active input");
                tile.setItem(0,new ItemStack(Items.IRON_SWORD));require(!tile.active() && tile.seconds()==0,"Swap retained previous progress");
            });
            if(net.minecraftforge.fml.ModList.get().isLoaded("automagy"))check("automagy_jar_pays_real_overchanter",()->AutomagyAcceptance.run(level,origin,originalTime));
            if(net.minecraftforge.fml.ModList.get().isLoaded("forbidden_magic"))check("forbidden_magic_nether_aspects_loaded",()->{
                var nether=tc4revival.aspect.TCAspects.catalog().require("forbidden_magic:infernus");
                for(String part:List.of("log","leaves","sapling"))require(ItemStackAspects.resolve(new ItemStack(Content.item("nether_"+part))).orElseThrow().amount(nether)==1,"Missing optional Nether aspect on "+part);
            });
            if(net.minecraftforge.fml.ModList.get().isLoaded("thaumic_tinkerer"))check("osmotic_enchanter_receives_five_enchantments_and_live_config",()->{
                boolean enabled=Configuration.TT_COMPAT.get();
                try{
                    var method=Class.forName("ttmodern.EnchanterCatalog").getMethod("entries");Configuration.TT_COMPAT.set(true);
                    var values=(Map<?,?>)method.invoke(null);
                    for(var entry:tbmodern.api.EnchantingCompatibility.entries())require(values.containsKey(ResourceLocation.parse(entry.enchantment())),"Missing osmotic entry "+entry.enchantment());
                    Configuration.TT_COMPAT.set(false);var disabled=(Map<?,?>)method.invoke(null);
                    require(!disabled.containsKey(new ResourceLocation("thaumicbases","elder_knowledge")),"Disabled integration still selectable");
                }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}finally{Configuration.TT_COMPAT.set(enabled);}
            });
            check("crop_harvest_and_replant",()->{
                var fake=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
                level.setBlock(origin.below(),Blocks.FARMLAND.defaultBlockState(),3);
                var plant=(PlantBlock)Content.block("plax");var mature=plant.defaultBlockState().setValue(PlantBlock.AGE,7);level.setBlock(origin,mature,3);
                plant.use(mature,level,origin,fake,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(origin.getCenter(),Direction.UP,origin,false));
                require(level.getBlockState(origin).getValue(PlantBlock.AGE)==0,"Harvest did not replant");
                var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(2));
                require(drops.stream().anyMatch(e->e.getItem().is(Items.STRING)),"Mature crop yielded no string");
            });
            check("infusion_output_preserves_unrelated_nbt",()->{
                var recipe=(InfusionRecipe)server.getRecipeManager().byKey(new ResourceLocation("thaumicbases","revolver/power_1")).orElseThrow();
                var gun=new ItemStack(Content.item("revolver"));gun.setDamageValue(37);gun.getOrCreateTag().putInt("Shots",4);
                require(RevolverUpgrades.upgrade(gun,"thaumicbases:heavy"),"Initial compatible upgrade");
                var upgraded=recipe.plan(gun).output().apply(gun);
                require(upgraded.getItem()==gun.getItem(),"Infusion changed carrier");
                require(RevolverUpgrades.level(upgraded,"power")==1 && RevolverUpgrades.level(upgraded,"heavy")==1,"Infusion patch lost an upgrade");
                require(upgraded.getDamageValue()==37 && upgraded.getTag().getInt("Shots")==4,"Infusion patch lost gun state");
                require(RevolverUpgrades.level(gun,"power")==0,"Preview mutated central stack");
            });
            check("void_anvil_payment_and_repair",()->{
                var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);player.experienceLevel=20;
                var menu=new MagicalAnvil.Menu(41,player.getInventory(),true);
                var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(100);sword.setRepairCost(4);
                menu.getSlot(0).set(sword);menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT,2));menu.createResult();
                require(menu.getCost()==3 && !menu.getSlot(2).getItem().isEmpty(),"Void anvil did not offer fixed-price repair");
                var result=menu.getSlot(2).getItem().copy();require(result.getDamageValue()==0 && result.getBaseRepairCost()==4,"Repair or legacy penalty differs");
                require(menu.getSlot(2).mayPickup(player),"Valid output blocked");menu.getSlot(2).onTake(player,result);
                require(player.experienceLevel==17 && menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(),"Payment/ingredient consumption mismatch");
            });
            check("configured_recipes_loaded_by_server",()->{
                var bright=(InfusionRecipe)server.getRecipeManager().byKey(new ResourceLocation("thaumicbases","equipment/brightfocirec")).orElseThrow();
                var center=new ItemStack(Content.item(Configuration.PEARL.get()?"thaumcraft:primordial_pearl":"thaumcraft:primal_charm"));
                require(bright.plan(center).aspects().amount(Aspect.AURA.unwrap())==(Configuration.PEARL.get()?256:64),"Brightness configuration absent from loaded recipe");
                for(String kind:List.of("air","fire","water","earth","order","entropy")) {
                    var recipe=server.getRecipeManager().byKey(new ResourceLocation("thaumicbases","alchemy/shards_"+kind)).orElseThrow();
                    require(recipe.getResultItem(level.registryAccess()).getCount()==Configuration.SHARDS.get(),"Shard yield differs from server config");
                }
            });
            check("module_switch_controls_real_recipe_matching",()->{
                var recipe=(InfusionRecipe)server.getRecipeManager().byKey(new ResourceLocation("thaumicbases","revolver/power_1")).orElseThrow();
                var input=new InfusionRecipe.Input(new ItemStack(Content.item("revolver")),List.of(new ItemStack(Items.GUNPOWDER),new ItemStack(Content.item("thaumcraft:salis_mundus")),new ItemStack(Items.FIRE_CHARGE),new ItemStack(Content.item("thaumcraft:salis_mundus")),new ItemStack(Items.FIREWORK_ROCKET)),key->true);
                boolean previous=Configuration.REVOLVER.get();
                try {
                    Configuration.REVOLVER.set(true);require(recipe.matches(input,level),"Unlocked valid upgrade does not match");
                    Configuration.REVOLVER.set(false);require(!recipe.matches(input,level),"Disabled module still crafts");
                    require(!thaumcraft.api.research.ResearchCategories.enabled("TB.Revolver.Power"),"Disabled research remains enabled");
                } finally {Configuration.REVOLVER.set(previous);}
            });
        } finally {
            ((ServerLevelData)level.getLevelData()).setGameTime(originalTime);
            try {
                Path target=Path.of(System.getProperty("thaumicbases.acceptanceReport", "../.tooling/server-acceptance.json")).toAbsolutePath();Files.createDirectories(target.getParent());
                Files.writeString(target,new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS)+"\n");
                System.out.println("THAUMIC_BASES_ACCEPTANCE "+RESULTS);
            } catch(Exception failure) {throw new RuntimeException(failure);}
            finally {server.halt(false);}
        }
    }
}
