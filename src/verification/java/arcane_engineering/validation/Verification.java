package arcane_engineering.validation;

import arcane_engineering.*;
import com.google.gson.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;
import thaumcraft.api.nodes.NodeApi;
import thaumcraft.api.aspects.AspectList;
import java.nio.file.*;
import java.util.*;

@Mod("arcane_engineering_validation")
public class Verification {
    private final JsonArray results=new JsonArray();
    private int passed,failed;
    public Verification(){MinecraftForge.EVENT_BUS.addListener(this::commands);}
    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("aetest").requires(s->s.hasPermission(2))
            .then(Commands.literal("run").executes(c->run(c.getSource().getPlayerOrException())))
            .then(Commands.literal("setup").executes(c->setup(c.getSource().getPlayerOrException())))
            .then(Commands.literal("craft").executes(c->craft(c.getSource().getPlayerOrException())))
            .then(Commands.literal("drill").executes(c->drill(c.getSource().getPlayerOrException())))
            .then(Commands.literal("device").executes(c->device(c.getSource().getPlayerOrException())))
            .then(Commands.literal("state").executes(c->{var p=c.getSource().getPlayerOrException(); var b=p.getItemBySlot(EquipmentSlot.FEET); c.getSource().sendSuccess(()->Component.literal("AE_STATE y="+p.getY()+" vy="+p.getDeltaMovement().y+" fuel="+ImpulseBootsItem.fuel(b)+" fall="+p.fallDistance+" permit="+BootsNetwork.hasFlightPermit(p)+" flying="+p.getAbilities().flying+" health="+p.getHealth()+" water="+p.isInWater()+" menu="+p.containerMenu.getClass().getSimpleName()+" slots="+p.containerMenu.slots.size()+" hand="+p.getMainHandItem().save(new CompoundTag())),false);return 1;})));
    }
    private static void check(boolean condition,String detail){if(!condition)throw new AssertionError(detail);}
    private void test(String id,Runnable body){
        var row=new JsonObject();row.addProperty("id",id);
        try {body.run();row.addProperty("status","pass");passed++;System.out.println("AE_TEST PASS "+id);}
        catch(Throwable error){row.addProperty("status","fail");row.addProperty("error",error.toString());failed++;error.printStackTrace();System.out.println("AE_TEST FAIL "+id+" "+error);}
        results.add(row);
    }
    private static ItemStack wand(String cap){return ArcaneEngineering.WAND.get().create(WandParts.Rod.require("arcane_engineering:upgradeable"),WandParts.Cap.require(cap),false,false);}
    private static IItemHandler inventory(ItemStack stack){return stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new AssertionError("No item capability"));}
    private static ItemStack stack(String id){return new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id)));}
    private static TransientCraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,-1){public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}},3,3);}
    private int run(ServerPlayer player){
        results.asList().clear();passed=failed=0;var level=player.serverLevel();var wandItem=ArcaneEngineering.WAND.get();
        test("wand.slots.2_3_4",()->{check(wandItem.slots(wand("iron"))==2,"iron");check(wandItem.slots(wand("arcane_engineering:electrum"))==3,"electrum");check(wandItem.slots(wand("arcane_engineering:steel"))==4,"steel");});
        test("wand.capacitors.simulation.persistence.removal",()->{
            var wand=wand("arcane_engineering:steel");var inv=inventory(wand);var before=wand.save(new CompoundTag());
            check(inv.insertItem(0,new ItemStack(ArcaneEngineering.CAPACITOR.get()),true).isEmpty(),"simulation accepts");
            check(before.equals(wand.save(new CompoundTag())),"simulation changed owner");
            int[] capacities={4500,6750,10125,15187};
            for(int i=0;i<4;i++){check(inv.insertItem(i,new ItemStack(ArcaneEngineering.CAPACITOR.get()),false).isEmpty(),"insert "+i);check(wandItem.capacity(wand)==capacities[i],"capacity "+i);}
            var copy=ItemStack.of(wand.save(new CompoundTag()));check(wandItem.capacity(copy)==15187&&inventory(copy).getStackInSlot(3).getItem()==ArcaneEngineering.CAPACITOR.get(),"NBT roundtrip");
            var aer=TCAspects.catalog().require("aer");wandItem.addVis(wand,aer,15187,false,true);
            before=wand.save(new CompoundTag());inv.extractItem(3,1,true);check(before.equals(wand.save(new CompoundTag())),"extract simulation changed owner");
            check(inv.extractItem(3,1,false).getItem()==ArcaneEngineering.CAPACITOR.get(),"remove");check(wand.getTag().getInt("aer")==10125,"removal must clamp stored vis");
            check(wandItem.capacity(copy)==15187,"copy shares mutable inventory");
        });
        test("wand.slot.validation",()->{var wand=wand("iron");var inv=inventory(wand);check(!inv.insertItem(2,new ItemStack(ArcaneEngineering.CAPACITOR.get()),false).isEmpty(),"locked slot");check(!inv.insertItem(0,new ItemStack(Items.DIAMOND),false).isEmpty(),"unrelated upgrade");check(inv.getSlotLimit(0)==1,"slot limit");});
        test("wand.workbench_mutable_slot_flush",()->{var wand=wand("iron");var inv=inventory(wand);inv.insertItem(0,new ItemStack(ArcaneEngineering.CAPACITOR.get()),false);inv.getStackInSlot(0).shrink(1);wandItem.recalculateUpgrades(wand,level,player);check(wandItem.capacity(ItemStack.of(wand.save(new CompoundTag())))==3000,"shift click reintroduced removed upgrade");});
        test("wand.energizer.potency.and_cost",()->{var wand=wand("arcane_engineering:steel");var a=TCAspects.catalog().require("aer");float base=wandItem.consumptionModifier(wand,player,a,true);inventory(wand).insertItem(0,new ItemStack(ArcaneEngineering.ENERGIZER.get()),false);check(Math.abs(wandItem.consumptionModifier(wand,player,a,true)-base-.1f)<.0001,"cost");check(wandItem.focusPotency(wand,player)>=1,"potency");});
        test("wand.charger.80_ticks.one_primal",()->{var wand=wand("iron");inventory(wand).insertItem(0,new ItemStack(ArcaneEngineering.CHARGER.get()),false);int previous=player.tickCount;try{for(int i=1;i<=80;i++){player.tickCount=i;wandItem.inventoryTick(wand,level,player,0,false);}check(TCAspects.catalog().primals().stream().mapToInt(a->wandItem.vis(wand,a)).sum()==100,"one whole vis per 80 ticks");}finally{player.tickCount=previous;}});
        test("wand.arcane_assembly.all_caps_and_research",()->{
            var recipe=(UpgradeableWandRecipe)level.getRecipeManager().byKey(ArcaneEngineering.id("upgradeable_wand")).orElseThrow();
            for(String cap:List.of("iron","gold","arcane_engineering:steel","arcane_engineering:electrum")){
                var grid=grid();var part=WandParts.Cap.require(cap);var capStack=part.item!=null?stack(part.item):new ItemStack(TCItems.wandComponent("wand_cap_"+cap));
                grid.setItem(2,capStack);grid.setItem(6,capStack.copy());grid.setItem(4,stack("immersiveengineering:stick_treated"));
                check(recipe.matches(grid,level),"match "+cap);check(!recipe.unlocked(grid,k->false)&&recipe.unlocked(grid,k->true),"research gate");
                var output=recipe.assemble(grid,level.registryAccess());check(output.is(ArcaneEngineering.WAND.get())&&wandItem.cap(output)==part,"output "+cap);
                check(recipe.visCost(grid).values().stream().allMatch(n->n==part.craftCost*2),"cost "+cap);
                grid.setItem(0,new ItemStack(Items.DIRT));check(!recipe.matches(grid,level),"extra input accepted");
            }
        });
        test("recipes.loaded.and.book_pages.resolve",()->{
            int count=0;for(var recipe:level.getRecipeManager().getRecipes())if(recipe.getId().getNamespace().equals(ArcaneEngineering.ID))count++;
            check(count==10,"Expected 10 executable recipes, got "+count);
            for(var entry:ResearchPages.bundled().entries().entrySet())if(entry.getKey().startsWith("arcane_engineering:"))for(var page:entry.getValue())if(LiveBookRecipes.supported(page.type()))check(!LiveBookRecipes.resolve(level,page).isEmpty(),"Empty live page "+page.id());
            check(BrowserRecipes.recipes(player).stream().anyMatch(r->r.output().is(ArcaneEngineering.WAND.get())),"Missing JEI wand assembly");
        });
        test("research.catalog.mirrors",()->{
            var c=ResearchCatalogs.catalog();
            check(c.all().stream().filter(r->r.key().startsWith("arcane_engineering:")).count()==15,"research count");
            check(c.original("arcane_engineering:AEFAKE_THAUMIUM").key().equals("THAUMIUM"),"mirror");
            var view=new ResearchBrowser(c,r->true,()->true).view(new PlayerKnowledge(TCAspects.catalog()),"ARCANEENGINEERING",0).orElseThrow();
            check(view.nodes().stream().filter(n->n.research().mirror()).count()==4,"hidden core mirrors");
            check(view.links().stream().filter(l->l.target().startsWith("arcane_engineering:AEFAKE_")).count()==4,"missing mirror links");
        });
        test("drills.tiers.wear.repair",()->{
            var t=ArcaneEngineering.THAUMIUM_DRILL.get();var v=ArcaneEngineering.VOID_DRILL.get();var ts=new ItemStack(t);var vs=new ItemStack(v);
            check(t.getMaximumHeadDamage(ts)==3000&&t.getMiningSpeed(ts)==10&&t.getAttackDamage(ts)==6&&t.getMiningLevel(ts)==Tiers.DIAMOND,"thaumium stats");
            t.damageHead(ts,77);check(t.getHeadDamage(ts)==77,"thaumium wear");check(t.isValidRepairItem(ts,stack("thaumcraft:thaumium_ingot")),"thaumium repair");
            check(v.getMaximumHeadDamage(vs)==4000&&v.getMiningSpeed(vs)==12&&v.getAttackDamage(vs)==7,"void stats");v.damageHead(vs,9999);check(v.getHeadDamage(vs)==0,"void wears");
        });
        test("boots.fluid.simulation.capacity.persistence",()->{
            var boots=new ItemStack(ArcaneEngineering.BOOTS.get());var fluid=boots.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(()->new AssertionError("fluid cap"));
            check(fluid.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==0,"water accepted");
            var diesel=new FluidStack(ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse("immersiveengineering:biodiesel")),25000);var before=boots.save(new CompoundTag());
            check(fluid.fill(diesel,IFluidHandler.FluidAction.SIMULATE)==20000&&before.equals(boots.save(new CompoundTag())),"fill simulation");
            check(fluid.fill(diesel,IFluidHandler.FluidAction.EXECUTE)==20000,"tank capacity");check(ImpulseBootsItem.fuel(ItemStack.of(boots.save(new CompoundTag())))==20000,"fuel save");
            check(ImpulseBootsItem.consume(boots)&&ImpulseBootsItem.fuel(boots)==19999,"fuel payment");check(boots.getMaxDamage()==169,"armor durability");
        });
        test("boots.motion.boundaries",()->{check(Math.abs(BootsNetwork.vertical(-1,false)+.6)<1e-9,"braking");check(Math.abs(BootsNetwork.vertical(0,false)-.1)<1e-9,"thrust");check(BootsNetwork.vertical(-.1,true)==0,"hover");check(Math.abs(BootsNetwork.vertical(-1,true)+.7)<1e-9,"hover braking");});
        test("boots.server_permission.lifecycle",()->{
            var previous=player.getItemBySlot(EquipmentSlot.FEET);var velocity=player.getDeltaMovement();int ticks=player.tickCount;
            boolean flying=player.getAbilities().flying;float fall=player.fallDistance;
            try {
                player.getAbilities().flying=false;
                var boots=stack("arcane_engineering:impulse_boots");fill(boots,1000);player.setItemSlot(EquipmentSlot.FEET,boots);
                check(!BootsNetwork.hasFlightPermit(player),"fuel alone granted flight");
                BootsNetwork.acceptControls(player,new BootsNetwork.Controls(true,false));
                check(!BootsNetwork.hasFlightPermit(player),"packet alone granted flight");
                BootsNetwork.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
                check(BootsNetwork.hasFlightPermit(player),"actual thrust not authorized");
                BootsNetwork.acceptControls(player,new BootsNetwork.Controls(false,false));
                player.tickCount=ticks+20;check(BootsNetwork.hasFlightPermit(player),"coast too short");
                player.tickCount=ticks+21;check(!BootsNetwork.hasFlightPermit(player),"coast did not expire");
                BootsNetwork.acceptControls(player,new BootsNetwork.Controls(true,true));
                BootsNetwork.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
                check(BootsNetwork.hasFlightPermit(player),"hover not authorized");
                player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);check(!BootsNetwork.hasFlightPermit(player),"unequipped boots retained permission");
                player.setItemSlot(EquipmentSlot.FEET,boots);BootsNetwork.acceptControls(player,new BootsNetwork.Controls(true,false));
                BootsNetwork.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
                boots.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(h->h.drain(20000,IFluidHandler.FluidAction.EXECUTE));
                check(!BootsNetwork.hasFlightPermit(player),"empty boots retained permission");
                check(!player.getAbilities().flying,"granted vanilla flying");
            } finally {
                BootsNetwork.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
                player.setItemSlot(EquipmentSlot.FEET,previous);player.tickCount=ticks;player.getAbilities().flying=flying;player.setDeltaMovement(velocity);player.fallDistance=fall;
            }
        });
        test("destabilizer.energy.sides.simulation.cycle.save",()->{
            var pos=new BlockPos(80,80,80);level.setBlockAndUpdate(pos,ArcaneEngineering.DESTABILIZER.get().defaultBlockState());level.setBlockAndUpdate(pos.above(),TCArcane.AURA_NODE.get().defaultBlockState());
            var d=(DestabilizerBlockEntity)level.getBlockEntity(pos);var node=(AuraNodeBlockEntity)level.getBlockEntity(pos.above());NodeApi.configure(node,new AspectList().add(thaumcraft.api.aspects.Aspect.AIR,30),AuraNodeType.PURE,AuraNodeModifier.BRIGHT,false);NodeApi.extract(node,thaumcraft.api.aspects.Aspect.AIR,7,false);
            for(var dir:Direction.values())check(d.getCapability(ForgeCapabilities.ENERGY,dir).isPresent()==(dir==Direction.DOWN),"FE side "+dir);
            for(var dir:Direction.values())check(d.getBlockState().isFaceSturdy(level,pos,dir)==(dir==Direction.DOWN),"support side "+dir);
            check(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(d.getBlockState()),"added a nonexistent iron tool gate");
            check(d.getBlockState().getBlock().getExplosionResistance()==5,"legacy blast resistance must be 5");
            var energy=d.getCapability(ForgeCapabilities.ENERGY,Direction.DOWN).orElseThrow(()->new AssertionError("energy cap"));
            check(energy.receiveEnergy(200000,true)==100000&&d.energy()==0,"FE simulation");check(energy.receiveEnergy(200000,false)==100000,"FE limit");
            DestabilizerBlockEntity.tick(level,pos,d.getBlockState(),d);check(d.open&&d.energy()==0&&node.nodeType()==AuraNodeType.HUNGRY,"activation");check(node.aspects().amount(TCAspects.catalog().require("aer"))==23,"activation refilled node");
            energy.receiveEnergy(500,false);DestabilizerBlockEntity.tick(level,pos,d.getBlockState(),d);check(d.energy()==400&&d.open,"run cost");
            var saved=d.saveWithoutMetadata();var loaded=new DestabilizerBlockEntity(pos,d.getBlockState());loaded.load(saved);check(loaded.energy()==400&&loaded.open,"device save");
            d.open=false;level.removeBlock(pos,false);level.removeBlock(pos.above(),false);
        });
        test("destabilizer.outage.both_branches",()->{
            boolean normal=false,tainted=false;var pos=new BlockPos(90,80,90);
            for(int seed=0;seed<200&&!(normal&&tainted);seed++){
                level.setBlockAndUpdate(pos,ArcaneEngineering.DESTABILIZER.get().defaultBlockState());level.setBlockAndUpdate(pos.above(),TCArcane.AURA_NODE.get().defaultBlockState());
                var d=(DestabilizerBlockEntity)level.getBlockEntity(pos);var node=(AuraNodeBlockEntity)level.getBlockEntity(pos.above());NodeApi.configure(node,new AspectList().add(thaumcraft.api.aspects.Aspect.AIR,30),AuraNodeType.HUNGRY,AuraNodeModifier.BRIGHT,false);d.open=true;
                level.random.setSeed(seed*10000L+179);DestabilizerBlockEntity.tick(level,pos,d.getBlockState(),d);check(!d.open,"outage still running");
                if(node.nodeType()==AuraNodeType.NORMAL){normal=true;check(node.base().amount(TCAspects.catalog().require("aer"))==30,"normal lost base");}
                else{tainted=true;check(node.nodeType()==AuraNodeType.TAINTED&&node.nodeModifier()==AuraNodeModifier.FADING&&node.base().amount(TCAspects.catalog().require("vitium"))==1,"failure state");}
                level.removeBlock(pos,false);level.removeBlock(pos.above(),false);
            }check(normal&&tainted,"did not cover both outcomes");
        });
        test("destabilizer.breaking_active_is_dangerous",()->{var pos=new BlockPos(100,80,100);level.setBlockAndUpdate(pos,ArcaneEngineering.DESTABILIZER.get().defaultBlockState());level.setBlockAndUpdate(pos.above(),TCArcane.AURA_NODE.get().defaultBlockState());var d=(DestabilizerBlockEntity)level.getBlockEntity(pos);var node=(AuraNodeBlockEntity)level.getBlockEntity(pos.above());NodeApi.configure(node,new AspectList().add(thaumcraft.api.aspects.Aspect.AIR,20),AuraNodeType.HUNGRY,AuraNodeModifier.BRIGHT,false);d.open=true;level.removeBlock(pos,false);check(node.nodeType()==AuraNodeType.TAINTED&&node.nodeModifier()==AuraNodeModifier.FADING,"removal did not taint");level.removeBlock(pos.above(),false);});
        try{Files.writeString(Path.of("ae-verification-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));}catch(Exception error){throw new RuntimeException(error);}
        player.sendSystemMessage(Component.literal("AE_TEST_DONE passed="+passed+" failed="+failed));return failed==0?1:0;
    }
    private int setup(ServerPlayer player){
        var level=player.serverLevel();var pos=new BlockPos(0,80,0);
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());
        player.teleportTo(level,.5,80,5.5,180,0);player.getInventory().clearContent();
        player.getInventory().setItem(0,wand("arcane_engineering:steel"));player.getInventory().setItem(1,new ItemStack(ArcaneEngineering.CAPACITOR.get()));player.getInventory().setItem(2,new ItemStack(ArcaneEngineering.CHARGER.get()));player.getInventory().setItem(3,new ItemStack(ArcaneEngineering.ENERGIZER.get()));
        player.getInventory().setItem(4,new ItemStack(ArcaneEngineering.THAUMIUM_DRILL.get()));player.getInventory().setItem(5,new ItemStack(ArcaneEngineering.VOID_DRILL.get()));player.getInventory().setItem(6,new ItemStack(ArcaneEngineering.DESTABILIZER_ITEM.get()));player.getInventory().setItem(7,new ItemStack(TCItems.THAUMONOMICON.get()));player.getInventory().setItem(8,stack("immersiveengineering:workbench"));
        var boots=new ItemStack(ArcaneEngineering.BOOTS.get());boots.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(h->h.fill(new FluidStack(ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse("immersiveengineering:biodiesel")),1000),IFluidHandler.FluidAction.EXECUTE));player.setItemSlot(EquipmentSlot.FEET,boots);
        level.setBlockAndUpdate(pos,ArcaneEngineering.DESTABILIZER.get().defaultBlockState());player.containerMenu.broadcastChanges();return 1;
    }
    private static void fill(ItemStack stack,int amount) { stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(h->h.fill(new FluidStack(ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse("immersiveengineering:biodiesel")),amount),IFluidHandler.FluidAction.EXECUTE)); }
    private int craft(ServerPlayer player) {
        var level=player.serverLevel();var pos=new BlockPos(-3,80,2);
        level.setBlockAndUpdate(pos,ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("thaumcraft:arcane_workbench")).defaultBlockState());
        var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);table.clearContent();
        table.setItem(2,stack("arcane_engineering:wandcap_steel"));table.setItem(6,stack("arcane_engineering:wandcap_steel"));table.setItem(4,stack("immersiveengineering:stick_treated"));
        var wand=wand("iron");for(var a:TCAspects.catalog().primals())ArcaneEngineering.WAND.get().addVis(wand,a,3000,false,true);table.setItem(10,wand);return 1;
    }
    private int drill(ServerPlayer player) {
        var drill=stack("immersiveengineering:drill");fill(drill,2000);player.getInventory().setItem(0,drill);
        player.getInventory().setItem(1,stack("arcane_engineering:drillhead_thaumium"));player.getInventory().setItem(2,stack("arcane_engineering:drillhead_void"));
        for(int x=-1;x<=1;x++)for(int y=80;y<=82;y++)for(int z=-5;z<=-4;z++)player.serverLevel().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
        player.containerMenu.broadcastChanges();return 1;
    }
    private int device(ServerPlayer player) {
        var level=player.serverLevel();var pos=new BlockPos(0,80,0);
        if(level.getBlockEntity(pos) instanceof DestabilizerBlockEntity old) old.open=false;
        level.setBlockAndUpdate(pos,ArcaneEngineering.DESTABILIZER.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(),TCArcane.AURA_NODE.get().defaultBlockState());
        NodeApi.configure((AuraNodeBlockEntity)level.getBlockEntity(pos.above()),new AspectList().add(thaumcraft.api.aspects.Aspect.AIR,30),AuraNodeType.NORMAL,AuraNodeModifier.BRIGHT,false);
        level.setBlockAndUpdate(pos.below(),ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("immersiveengineering:capacitor_creative")).defaultBlockState());
        return 1;
    }
}
