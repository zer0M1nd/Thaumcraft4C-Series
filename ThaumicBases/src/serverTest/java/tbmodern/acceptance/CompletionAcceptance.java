package tbmodern.acceptance;

import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import tbmodern.*;
import tbmodern.api.*;
import tc4revival.content.*;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.*;
import thaumcraft.api.nodes.NodeApi;

/** Opt-in isolated test payload, excluded from both distributed JARs. */
@Mod.EventBusSubscriber(modid=ThaumicBases.ID)
public class CompletionAcceptance {
    private static final Map<String,String> RESULTS=new LinkedHashMap<>();
    private static void require(boolean value,String message) { if(!value)throw new AssertionError(message); }
    private static void check(String id,Runnable action) {
        try { action.run();RESULTS.put(id,"PASS"); }
        catch(Throwable failure) { RESULTS.put(id,"FAIL: "+failure);failure.printStackTrace(); }
        System.out.println("TB_COMPLETION "+id+" "+RESULTS.get(id));
    }
    private static void save() {
        try {Files.writeString(Path.of("completion-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private static ItemStack wand(String kind,String upgrade) {
        var wand=ThaumcraftApi.createWand("silverwood","gold",false,true);
        var focus=new ItemStack(Content.item("focus_"+kind));
        if(upgrade!=null)((WandFocusItem)focus.getItem()).applyUpgrade(focus,FocusUpgrade.require("thaumicbases:"+upgrade),3);
        WandFocusItem.attach(wand,focus);return wand;
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(Boolean.getBoolean("thaumicbases.migrationSeed")) {
            var level=event.getServer().overworld();var p=new BlockPos(0,100,0);level.getChunkAt(p);
            level.setBlock(p,Blocks.CHEST.defaultBlockState(),3);
            var stack=new ItemStack(Content.item("crying_obelisk"),7);stack.getOrCreateTag().putString("MigrationMarker","preserve");
            ((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(p)).setItem(0,stack);
            level.setBlock(p.east(3),Content.block("crying_obelisk").defaultBlockState(),2|16);
            System.out.println("TB_MIGRATION SEEDED");event.getServer().halt(false);return;
        }
        if(Boolean.getBoolean("thaumicbases.migrationVerify")) {event.getServer().overworld().getChunkAt(new BlockPos(0,100,0));return;}
        if(!Boolean.getBoolean("thaumicbases.completion"))return;
        var level=event.getServer().overworld();var pos=new BlockPos(0,100,0);level.getChunkAt(pos);
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        long time=level.getGameTime();
        check("registration_research_recipe_warp",()->{
            require(!ForgeRegistries.ITEMS.containsKey(new ResourceLocation("thaumicbases:crying_obelisk")),"Duplicate crying obsidian item still exists");
            for(String key:List.of("TB.Enchantments","TB.TaintMinor","TB.HerobrinesScythe"))require(ThaumcraftApi.research().find(key).isPresent(),"Missing "+key);
            var recipe=(InfusionRecipe)event.getServer().getRecipeManager().byKey(new ResourceLocation("thaumicbases:equipment/herobrinesscytherec")).orElseThrow();
            var inputs=List.of(new ItemStack(Items.NETHER_STAR),new ItemStack(TCItems.LIQUID_DEATH_BUCKET.get()),new ItemStack(Content.item("thaumcraft:staff_rod_primal")),new ItemStack(TCItems.LIQUID_DEATH_BUCKET.get()));
            require(recipe.matches(new InfusionRecipe.Input(new ItemStack(Content.item("thaumcraft:void_hoe")),inputs,k->true),level),"Author scythe infusion does not match");
            require(!recipe.matches(new InfusionRecipe.Input(new ItemStack(Items.IRON_HOE),inputs,k->true),level),"Wrong center accepted");
            for(var e:Map.of("tobacco_eldritch",1,"node_focus_destruction",1,"node_focus_taint",2,"bracelet_primal",3).entrySet())
                require(ThaumcraftApi.craftingWarp(new ItemStack(Content.item(e.getKey()))).orElse(-1)==e.getValue(),"Crafting warp "+e.getKey());
            require(tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(Content.item("herobrines_scythe"))).isPresent(),"Scythe has no scan aspects");
            var bricks=(CrucibleRecipe)event.getServer().getRecipeManager().byKey(new ResourceLocation("thaumicbases:alchemy/chiseledbricks")).orElseThrow();
            for(Item item:List.of(Items.STONE_BRICKS,Items.CRACKED_STONE_BRICKS,Items.MOSSY_STONE_BRICKS,Items.CHISELED_STONE_BRICKS))require(bricks.catalystMatches(new ItemStack(item)),"Stone brick variant rejected "+item);
            var air=(CrucibleRecipe)event.getServer().getRecipeManager().byKey(new ResourceLocation("thaumicbases:alchemy/shards_air")).orElseThrow();
            require(air.catalystMatches(new ItemStack(Items.COPPER_ORE)),"Data-pack infused ore tag extension rejected");
        });
        for(String material:List.of("water","lava","gas","goo"))check("focus_removes_"+material,()->{
            var target=pos.above();
            Block block=switch(material){case "water"->Blocks.WATER;case "lava"->Blocks.LAVA;case "gas"->TCArcane.FLUX_GAS.get();default->TCArcane.FLUX_GOO.get();};
            level.setBlock(target,block.defaultBlockState(),2);player.moveTo(.5,100,4.5,180,0);player.tickCount=10;
            var stack=wand(material.equals("water")||material.equals("lava")?"drain":"flux",material.equals("lava")?"netheric":null);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.startUsingItem(InteractionHand.MAIN_HAND);
            ((Foci)WandFocusItem.attached(stack).getItem()).usingTick(stack,player,100);
            require(level.getBlockState(target).isAir(),"Fluid restored or missed: "+level.getBlockState(target));player.stopUsingItem();
        });
        check("flux_oblique_voxel_intersection",()->{
            player.moveTo(.99,100,3.99,160,0);player.tickCount=10;
            var eye=player.getEyePosition();var look=player.getLookAngle();
            Set<BlockPos> samples=new HashSet<>();for(int i=0;i<player.getBlockReach();i++)samples.add(BlockPos.containing(eye.add(look.scale(i))));
            var missed=new ArrayList<BlockPos>();
            net.minecraft.world.level.BlockGetter.traverseBlocks(eye,eye.add(look.scale(player.getBlockReach())),level,(world,cell)->{if(!samples.contains(cell))missed.add(cell.immutable());return null;},w->null);
            require(!missed.isEmpty(),"Oblique fixture must hit a formerly skipped voxel");
            var target=missed.get(0);level.setBlock(target,TCArcane.FLUX_GOO.get().defaultBlockState(),2);
            var stack=wand("flux",null);player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.startUsingItem(InteractionHand.MAIN_HAND);
            ((Foci)Content.item("focus_flux")).usingTick(stack,player,100);require(level.getBlockState(target).isAir(),"Oblique goo not cleared");player.stopUsingItem();
        });
        check("crying_obsidian_assembly_disassembly_drop_respawn",()->{
            var p=pos.offset(6,0,0);level.setBlock(p,Blocks.CRYING_OBSIDIAN.defaultBlockState(),3);level.setBlock(p.above(),Blocks.CRYING_OBSIDIAN.defaultBlockState(),3);
            require(level.getBlockState(p).is(Content.block("crying_obelisk")),"Vanilla pair did not assemble");
            require(level.getBlockState(p).getValue(CryingObelisk.PART)==1 && level.getBlockState(p.above()).getValue(CryingObelisk.PART)==2,"Wrong halves");
            for(BlockPos floor:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,-1,2)))level.setBlock(floor,Blocks.STONE.defaultBlockState(),2);
            var state=level.getBlockState(p);state.use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(p.getCenter(),Direction.NORTH,p,false));
            require(p.equals(player.getRespawnPosition()),"Spawn anchor not recorded");
            require(state.getRespawnPosition(EntityType.PLAYER,level,p,0,player).isPresent(),"Safe respawn missing");
            var drops=Block.getDrops(state,level,p,null,player,new ItemStack(Items.DIAMOND_PICKAXE));
            require(drops.size()==1 && drops.get(0).is(Items.CRYING_OBSIDIAN),"Wrong structure drops "+drops);
            require(new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(state),"Diamond tool tag missing");
            level.removeBlock(p.above(),false);require(level.getBlockState(p).is(Blocks.CRYING_OBSIDIAN),"Remaining half did not revert");
            require(level.getBlockState(p).getRespawnPosition(EntityType.PLAYER,level,p,0,player).isEmpty(),"Broken structure still respawns");
        });
        check("blood_leggings_add_half_knockback",()->{
            var stack=new ItemStack(Content.item("bloody_leggings"));var map=stack.getAttributeModifiers(EquipmentSlot.LEGS);
            var modifier=map.get(Attributes.KNOCKBACK_RESISTANCE).iterator().next();
            require(modifier.getAmount()==.5 && modifier.getOperation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION,"Wrong knockback operation");
        });
        check("destruction_removes_node_preserves_machine",()->{
            var p=pos.offset(10,0,0);level.setBlock(p,TCArcane.AURA_NODE.get().defaultBlockState(),3);
            var node=(AuraNodeBlockEntity)level.getBlockEntity(p);NodeApi.configure(node,new AspectList().add(Aspect.AIR,40),AuraNodeType.NORMAL,AuraNodeModifier.FADING,false);
            level.setBlock(p.above(),Content.block("node_manipulator").defaultBlockState(),3);var machine=(NodeManipulator)level.getBlockEntity(p.above());machine.setMode("destruction");
            for(int i=0;i<360;i++){((ServerLevelData)level.getLevelData()).setGameTime(13+20L*i);NodeManipulator.tick(level,p.above(),machine.getBlockState(),machine);}
            require(level.getBlockState(p).isAir(),"Final node survives");require(level.getBlockEntity(p.above())==machine,"Manipulator was removed");
        });
        for(int crystals:new int[]{0,1,2,3,4,5,6})check("deconstructor_"+crystals+"_crystals",()->{
            var p=pos.offset(14,0,crystals*3);level.getChunkAt(p);level.setBlock(p,Content.block("entity_deconstructor").defaultBlockState(),3);
            var machine=(EntityDeconstructor)level.getBlockEntity(p);var tag=new CompoundTag();tag.putUUID("Owner",player.getUUID());tag.putInt("Crystals",(1<<crystals)-1);machine.load(tag);
            var zombie=new Zombie(level);zombie.setNoAi(true);zombie.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);level.addFreshEntity(zombie);
            float health=zombie.getHealth();for(int i=0;i<40;i++)EntityDeconstructor.tick(level,p,machine.getBlockState(),machine);
            require(zombie.getHealth()<health,"No processing at "+crystals+" crystals");zombie.discard();
        });
        check("scythe_chain_damage_enchants_repair_and_exclusions",()->{
            var base=pos.offset(50,0,0);level.getChunkAt(base);level.getChunkAt(base.offset(16,0,0));
            player.moveTo(base.getX(),base.getY(),base.getZ(),0,0);var weapon=new ItemStack(Content.item("herobrines_scythe"));weapon.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);
            player.setItemInHand(InteractionHand.MAIN_HAND,weapon);player.getAttributes().addTransientAttributeModifiers(weapon.getAttributeModifiers(EquipmentSlot.MAINHAND));
            var mobs=new ArrayList<Zombie>();
            for(int i=0;i<3;i++){var z=new Zombie(level);z.setNoAi(true);z.moveTo(base.getX()+i*6,base.getY(),base.getZ(),0,0);level.addFreshEntity(z);mobs.add(z);}
            var cow=EntityType.COW.create(level);cow.moveTo(base.getX()+6,base.getY(),base.getZ()+2,0,0);level.addFreshEntity(cow);float cowHp=cow.getHealth();
            ((HerobrinesScythe)weapon.getItem()).onLeftClickEntity(weapon,player,mobs.get(0));
            require(mobs.get(1).getHealth()<5 && mobs.get(2).getHealth()<5,"Secondary hits lost full damage or chain reach");require(cow.getHealth()==cowHp,"Passive mob damaged");
            weapon.setDamageValue(20);player.tickCount=20;weapon.getItem().inventoryTick(weapon,level,player,0,false);require(weapon.getDamageValue()==19,"Repair failed");
            require(((HerobrinesScythe)weapon.getItem()).getWarp(weapon,player)==3,"Wrong gear warp");
            player.getAttributes().removeAttributeModifiers(weapon.getAttributeModifiers(EquipmentSlot.MAINHAND));mobs.forEach(Entity::discard);cow.discard();
        });
        check("experience_fluid_conservation_and_real_eio",()->{
            var fluid=ForgeRegistries.FLUIDS.getValue(new ResourceLocation("enderio:xp_juice"));require(fluid!=null,"EIO absent");
            var p=pos.offset(0,0,10);Block block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("enderio:xp_obelisk"));require(block!=null,"EIO obelisk absent");
            level.setBlock(p,block.defaultBlockState(),3);BlockEntity tile=level.getBlockEntity(p);
            var tank=tile.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new AssertionError("No EIO unsided tank"));
            require(tank.fill(new FluidStack(fluid,16519),IFluidHandler.FluidAction.EXECUTE)==16519,"Cannot fill EIO");
            require(ExperienceSources.extract(tile,825)==825,"825 XP not extracted");require(tank.getFluidInTank(0).getAmount()==19,"Fractional remainder lost");
            require(ExperienceSources.extract(tile,825)==0 && tank.getFluidInTank(0).getAmount()==19,"Sub-point fluid duplicated/lost");
            tank.fill(new FluidStack(fluid,1),IFluidHandler.FluidAction.EXECUTE);require(ExperienceSources.extract(tile,1)==1,"Remainder not reusable");
            var generic=new FluidTank(10000);generic.fill(new FluidStack(fluid,47),IFluidHandler.FluidAction.EXECUTE);
            require(ExperienceFluids.extract(generic,1,tile)==1 && generic.getFluidAmount()==27,"Generic tank maximum violated");
            require(ExperienceFluids.extract(generic,99,tile)==1 && generic.getFluidAmount()==7,"Generic fractional handling");
            generic.setFluid(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));require(ExperienceFluids.extract(generic,99,tile)==0 && generic.getFluidAmount()==1000,"Non-XP fluid consumed");
        });
        check("eio_pays_complete_overchant",()->{
            var p=pos.offset(80,0,0);var machine=prepareOverchant(level,p);
            require(machine.start(),"Overchanter refused input");
            for(int i=0;i<800 && machine.active();i++){((ServerLevelData)level.getLevelData()).setGameTime(time+i+1);Overchanter.tick(level,p,machine.getBlockState(),machine);}
            require(!machine.active() && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,machine.getItem(0))==6,"EIO overchant did not finish");
            var tank=level.getBlockEntity(p.east(2)).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new AssertionError("No fluid tank"));
            require(tank.getFluidInTank(0).getAmount()==19,"Full cycle did not use exactly 825 XP");
        });
        check("fm_aspects_no_stale_fire_and_recipe",()->{
            var nether=tc4revival.aspect.TCAspects.catalog().require("forbidden_magic:infernus");
            for(String part:List.of("leaves","sapling","log")) {
                var amounts=tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(Content.item("nether_"+part))).orElseThrow();
                require(amounts.amount(nether)==1,"Missing infernus "+part);require(amounts.amount(Aspect.FIRE.unwrap())==(part.equals("log")?1:0),"Stale ignis "+part);
            }
            var recipe=(InfusionRecipe)event.getServer().getRecipeManager().byKey(new ResourceLocation("thaumicbases:equipment/nethersaplingrec")).orElseThrow();
            var costs=recipe.plan(new ItemStack(Content.item("thaumcraft:greatwood_sapling"))).aspects();require(costs.amount(nether)==32 && costs.amount(Aspect.FIRE.unwrap())==0,"Nether recipe mismatch");
        });
        ((ServerLevelData)level.getLevelData()).setGameTime(time);level.setDayTime(6000);save();
    }
    private static int migrationTicks;
    @SubscribeEvent public static void migrationTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if(!Boolean.getBoolean("thaumicbases.migrationVerify") || event.phase!=net.minecraftforge.event.TickEvent.Phase.END || ++migrationTicks!=40)return;
        var level=event.getServer().overworld();var p=new BlockPos(0,100,0);
        check("old_item_id_count_nbt_migration",()->{
            var stack=((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(p)).getItem(0);
            require(stack.is(Items.CRYING_OBSIDIAN) && stack.getCount()==7 && stack.getOrCreateTag().getString("MigrationMarker").equals("preserve"),"Old item not remapped: "+stack.save(new CompoundTag()));
        });
        check("old_standalone_block_migration",()->require(level.getBlockState(p.east(3)).is(Blocks.CRYING_OBSIDIAN),"Old standalone block was not migrated on chunk load"));
        check("without_fm_fallback",()->{
            require(tc4revival.aspect.TCAspects.catalog().find("forbidden_magic:infernus").isEmpty(),"FM must be absent in this fixture");
            var log=tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(Content.item("nether_log"))).orElseThrow();require(log.amount(Aspect.FIRE.unwrap())==2,"No-FM log ignis mismatch");
            var recipe=(InfusionRecipe)event.getServer().getRecipeManager().byKey(new ResourceLocation("thaumicbases:equipment/nethersaplingrec")).orElseThrow();
            require(recipe.plan(new ItemStack(Content.item("thaumcraft:greatwood_sapling"))).aspects().amount(Aspect.FIRE.unwrap())==32,"No-FM recipe mismatch");
        });save();event.getServer().halt(false);
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        if(!Boolean.getBoolean("thaumicbases.completion"))return;
        event.getDispatcher().register(Commands.literal("tbcompletion").requires(s->s.hasPermission(2))
            .then(Commands.literal("scene").executes(c->{scene(c.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("effects").executes(c->{effects(c.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("combat").executes(c->{combat(c.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("inspect").executes(c->{inspect(c.getSource().getPlayerOrException());return 1;})));
    }
    private static Overchanter prepareOverchant(ServerLevel level,BlockPos p) {
        level.getChunkAt(p);level.setBlock(p,Content.block("overchanter").defaultBlockState(),3);
        level.setBlock(p.east(2),ForgeRegistries.BLOCKS.getValue(new ResourceLocation("enderio:xp_obelisk")).defaultBlockState(),3);
        var tank=level.getBlockEntity(p.east(2)).getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new AssertionError("No fluid tank"));
        tank.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.EXECUTE);
        tank.fill(new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation("enderio:xp_juice")),16519),IFluidHandler.FluidAction.EXECUTE);
        level.setBlock(p.west(2),TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);
        ((EssentiaJarBlockEntity)level.getBlockEntity(p.west(2))).addToContainer(Aspect.MAGIC.unwrap(),64);
        var machine=(Overchanter)level.getBlockEntity(p);var sword=new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,5);machine.setItem(0,sword);return machine;
    }
    private static void effects(ServerPlayer player) {
        var level=player.serverLevel();prepareOverchant(level,new BlockPos(10,57,0));
        player.getInventory().setItem(6,ThaumcraftApi.createWand("silverwood","gold",false,true));
        for(int x:new int[]{0,5,20}) {
            var p=new BlockPos(x,57,-5);level.setBlock(p,TCArcane.AURA_NODE.get().defaultBlockState(),3);
            NodeApi.configure((AuraNodeBlockEntity)level.getBlockEntity(p),new AspectList().add(Aspect.AIR,200),AuraNodeType.NORMAL,null,false);
            level.setBlock(p.above(),Content.block(x==20?"node_manipulator":"node_linker").defaultBlockState(),3);
        }
        ((NodeManipulator)level.getBlockEntity(new BlockPos(20,58,-5))).setMode("purity");
        var link=new CompoundTag();link.putLong("Donor",new BlockPos(5,58,-5).asLong());
        ((NodeLinker)level.getBlockEntity(new BlockPos(0,58,-5))).load(link);
        player.inventoryMenu.broadcastChanges();
    }
    private static void combat(ServerPlayer player) {
        var level=player.serverLevel();
        level.getEntities((Entity)null,new AABB(-2,56,0,24,62,10),e->e.getTags().contains("tb_completion")).forEach(Entity::discard);
        for(int i=0;i<3;i++) {
            var zombie=new Zombie(level);zombie.setNoAi(true);zombie.setPersistenceRequired();zombie.addTag("tb_completion");
            zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);zombie.setHealth(100);
            zombie.moveTo(.5+i*6,57,5.5,0,0);level.addFreshEntity(zombie);
        }
        var cow=EntityType.COW.create(level);cow.addTag("tb_completion");cow.setNoAi(true);cow.moveTo(6.5,57,7.5,0,0);level.addFreshEntity(cow);
        player.teleportTo(.5,57,8.5);player.getInventory().selected=0;
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.inventoryMenu.broadcastChanges();
    }
    private static void inspect(ServerPlayer player) {
        var values=new LinkedHashMap<String,Object>();values.put("name",player.getDisplayName().getString());values.put("position",player.position().toString());
        values.put("respawn",String.valueOf(player.getRespawnPosition()));values.put("held",player.getMainHandItem().save(new CompoundTag()).toString());
        var mobs=new ArrayList<String>();player.serverLevel().getEntities((Entity)null,new AABB(-2,56,0,24,62,10),e->e.getTags().contains("tb_completion")).forEach(e->mobs.add(e.getType()+" "+e.position()+" health="+((LivingEntity)e).getHealth()));values.put("mobs",mobs);
        for(var p:List.of(new BlockPos(10,57,0),new BlockPos(12,57,0))) {var be=player.serverLevel().getBlockEntity(p);if(be!=null)values.put(p.toShortString(),be.saveWithFullMetadata().toString());}
        try{Files.writeString(Path.of("client-observation-"+System.currentTimeMillis()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(values));}catch(Exception e){throw new RuntimeException(e);}
    }
    private static void scene(ServerPlayer player) {
        for(BlockPos floor:BlockPos.betweenClosed(new BlockPos(-16,56,-16),new BlockPos(24,56,24)))player.serverLevel().setBlock(floor,Blocks.SANDSTONE.defaultBlockState(),2);
        var level=player.serverLevel();player.teleportTo(0.5,57,8.5);player.setYRot(180);player.setXRot(0);player.getInventory().clearContent();
        player.getInventory().setItem(0,new ItemStack(Content.item("herobrines_scythe")));
        player.getInventory().setItem(1,wand("drain","netheric"));player.getInventory().setItem(2,wand("flux","crystalization"));
        player.getInventory().setItem(3,new ItemStack(Items.CRYING_OBSIDIAN,16));player.getInventory().setItem(4,new ItemStack(Items.DIAMOND_PICKAXE));
        player.getInventory().setItem(5,new ItemStack(TCItems.THAUMONOMICON.get()));
        var p=new BlockPos(0,58,4);level.setBlock(p,Blocks.WATER.defaultBlockState(),2);level.setBlock(p.offset(2,0,0),Blocks.LAVA.defaultBlockState(),2);
        level.setBlock(p.offset(4,0,0),TCArcane.FLUX_GAS.get().defaultBlockState(),2);level.setBlock(p.offset(6,0,0),TCArcane.FLUX_GOO.get().defaultBlockState(),2);
        level.setDayTime(6000);player.inventoryMenu.broadcastChanges();
    }
    @Mod.EventBusSubscriber(modid=ThaumicBases.ID,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientEvidence {
        private static final Map<String,Integer> SOUNDS=new java.util.concurrent.ConcurrentHashMap<>();
        @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundSourceEvent event) {
            var id=event.getSound().getLocation().toString();if(!id.startsWith("thaumcraft:"))return;
            SOUNDS.merge(id,1,Integer::sum);
            try{Files.writeString(Path.of("completion-audio.json"),new GsonBuilder().setPrettyPrinting().create().toJson(SOUNDS));}catch(Exception e){throw new RuntimeException(e);}
        }
        @SubscribeEvent public static void commands(net.minecraftforge.client.event.RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("tbclient").then(Commands.literal("name").executes(c->{
                try{Files.writeString(Path.of("completion-client-name.txt"),net.minecraft.client.Minecraft.getInstance().player.getDisplayName().getString());}catch(Exception e){throw new RuntimeException(e);}return 1;
            })));
        }
    }
}
