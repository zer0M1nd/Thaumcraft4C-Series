package txverification;

import java.util.*;
import java.nio.file.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumicexploration.*;

@Mod("thaumicexploration_verification")
public final class Verification {
    private static final List<Map<String,Object>> results=new ArrayList<>();
    private static final TreeMap<Long,List<Runnable>> pending=new TreeMap<>();
    private static MinecraftServer server;
    private static final BlockPos BASE=new BlockPos(0,80,0);
    private static final UUID OWNER=UUID.fromString("47152737-0011-4053-8017-102011700053");
    public Verification(){net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);}
    private static void check(String name,boolean result){results.add(Map.of("case",name,"passed",result));System.out.println("TX_VERIFY "+(result?"PASS ":"FAIL ")+name);save();}
    private static void test(String name,Runnable body){try{body.run();}catch(Throwable failure){check(name+": "+failure,false);failure.printStackTrace();}}
    private static void save(){try{Files.writeString(Path.of("tx-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));}catch(Exception ignored){}}
    private static void later(int ticks,Runnable body){pending.computeIfAbsent(server.overworld().getGameTime()+ticks,n->new ArrayList<>()).add(body);}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||server==null)return;while(!pending.isEmpty()&&pending.firstKey()<=server.overworld().getGameTime())for(var task:pending.pollFirstEntry().getValue())test("scheduled",task);}
    @SubscribeEvent public void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("txverify").requires(s->s.hasPermission(2))
        .then(Commands.literal("run").executes(c->{server=c.getSource().getServer();run();return 1;}))
        .then(Commands.literal("scene").executes(c->{server=c.getSource().getServer();scene();return 1;}))
        .then(Commands.literal("reload").executes(c->{server=c.getSource().getServer();reload();return 1;}))
        .then(Commands.literal("player").executes(c->{server=c.getSource().getServer();player();return 1;}))
        .then(Commands.literal("items").executes(c->{server=c.getSource().getServer();items();return 1;}))
        .then(Commands.literal("lifecycle").executes(c->{server=c.getSource().getServer();lifecycle();return 1;}))
        .then(Commands.literal("lifecycleReload").executes(c->{server=c.getSource().getServer();lifecycleReload();return 1;}))
        .then(Commands.literal("config").executes(c->{server=c.getSource().getServer();config();return 1;}))
        .then(Commands.literal("status").executes(c->{c.getSource().sendSuccess(()->Component.literal("checks="+results.size()+" failed="+results.stream().filter(r->r.get("passed").equals(false)).count()+" pending="+pending.size()),false);return 1;}))
    );}
    private static MachineEntity machine(String name,BlockPos pos){var level=server.overworld();level.setBlockAndUpdate(pos,ExplorationMod.block(name).defaultBlockState());return (MachineEntity)level.getBlockEntity(pos);}
    private static BoundEntity bound(ServerLevel level,String name,BlockPos pos,String network,int color){level.setBlockAndUpdate(pos,ExplorationMod.block(name).defaultBlockState());var be=(BoundEntity)level.getBlockEntity(pos);be.initialize(network,color);return be;}
    private static void run(){
        results.clear();pending.clear();var level=server.overworld();
        // Keep both chunk tickets and ServerLevel's no-player tick gate active.
        for(int x=-1;x<=2;x++)for(int z=-1;z<=1;z++)level.setChunkForced(x,z,true);
        for(var entity:level.getEntitiesOfClass(Cow.class,new AABB(BASE.offset(-14,-2,-4),BASE.offset(24,4,8))))entity.discard();
        for(var pos:BlockPos.betweenClosed(BASE.offset(-14,-2,-4),BASE.offset(24,3,8)))level.setBlockAndUpdate(pos,pos.getY()==79?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        var data=BoundNetworks.get(level);data.chest("2:test-chest-rebound").clearContent();data.chest("3:test-chest-rebound").clearContent();
        var jarReset=data.jar("4:test-jar");for(var a:jarReset.contents().aspects())jarReset.extract(a,Integer.MAX_VALUE,false);
        test("registry",()->{
            check("98 registered items",ExplorationMod.ITEM.size()==98);
            long recipes=level.getRecipeManager().getRecipes().stream().filter(r->r.getId().getNamespace().equals(ExplorationMod.ID)).count();check("default recipes loaded: "+recipes,recipes==101);
            var catalog=tc4revival.research.ResearchCatalogs.bundled();
            check("research category TX",catalog.categories().stream().anyMatch(c->c.key().equals("TX")));
            for(var entry:catalog.all())if(entry.key().startsWith(ExplorationMod.ID+":")&&!entry.mirror())check("pages "+entry.key(),!tc4revival.research.ResearchPages.bundled().require(entry.key()).isEmpty());
            check("infusion binding sword eligible",ExplorationMod.BINDING.get().canEnchant(new ItemStack(Items.IRON_SWORD)));
            check("infusion night vision helmet eligible",ExplorationMod.NIGHT_VISION.get().canEnchant(new ItemStack(Items.IRON_HELMET)));
            check("infusion enchant rejects stick",!ExplorationMod.BINDING.get().canEnchant(new ItemStack(Items.STICK)));
        });
        test("bound networks",()->{
            var a=bound(level,"bound_chest",BASE,"test-chest",2);var cap=a.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
            a.initialize("test-chest-rebound",2);var b=bound(level,"bound_chest",BASE.offset(2,0,0),"test-chest-rebound",2);
            var stack=new ItemStack(Items.DIAMOND,12);stack.getOrCreateTag().putString("proof","preserved");cap.insertItem(0,stack,false);
            check("cached capability follows initialization",ItemStack.matches(stack,b.chest().getItem(0)));
            var nether=server.getLevel(Level.NETHER);var remote=bound(nether,"bound_chest",new BlockPos(0,100,0),"test-chest-rebound",2);
            check("chest crosses dimension",remote.chest()==a.chest());
            var removed=remote.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new).extractItem(0,5,false);
            check("cross-dimension extraction conserves items",removed.getCount()==5&&a.chest().getItem(0).getCount()==7);
            var different=bound(level,"bound_chest",BASE.offset(4,0,0),"test-chest-rebound",3);check("colour isolates network",different.chest().isEmpty());
            var first=bound(level,"bound_jar",BASE.offset(0,0,3),"test-jar",4);var second=bound(nether,"bound_jar",new BlockPos(2,100,0),"test-jar",4);
            var air=MachineEntity.aspect("aer");check("jar insert capacity",first.insert(air,80,false)==64&&second.amount()==64);
            check("jar rejects mixed aspects",second.insert(MachineEntity.aspect("aqua"),8,false)==0);
            check("jar simulation has no mutation",second.extract(air,9,true)==9&&first.amount()==64);
            check("jar shared extract",second.extract(air,12,false)==12&&first.amount()==52);
            var saved=BoundNetworks.get(level).save(new CompoundTag());var loaded=BoundNetworks.load(saved);
            check("network NBT round trip",loaded.chest(a.key()).getItem(0).getCount()==7&&loaded.jar(first.key()).contents().amount(air)==52);
            // Exercise an actual top transport on normal server ticks.
            var source=machine("crucible_souls",first.getBlockPos().above());var t=new CompoundTag();var buf=new thaumcraft.api.aspects.EssentiaBuffer(100,n->true,()->{});buf.insert(air,8,false);t.put("buffer",buf.save());source.load(t);
            later(55,()->check("bound jar pulls top transport: "+first.amount()+" / "+source.amount(),first.amount()==60&&source.amount()==0));
        });
        test("urns",()->{
            var water=machine("everfull_urn",BASE.offset(7,0,0));
            check("urn has no lower fluid port",!water.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.DOWN).isPresent());
            var fluid=water.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
            check("everfull bucket drains",fluid.drain(1000,IFluidHandler.FluidAction.EXECUTE).getAmount()==1000);
            check("everfull refuses filling",fluid.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==0);
            var lava=machine("everburn_urn",BASE.offset(9,0,0));lava.vis=4;
            var handler=lava.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
            check("lava simulate",handler.drain(333,IFluidHandler.FluidAction.SIMULATE).getAmount()==333&&handler.getFluidInTank(0).getAmount()==1000);
            handler.drain(333,IFluidHandler.FluidAction.EXECUTE);var nbt=lava.saveWithoutMetadata();lava.load(nbt);
            check("fractional lava survives save/load",handler.getFluidInTank(0).getAmount()==667&&handler.drain(1000,IFluidHandler.FluidAction.EXECUTE).getAmount()==667&&handler.drain(1,IFluidHandler.FluidAction.EXECUTE).isEmpty());
            var crucible=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("thaumcraft:crucible"));var pos=water.getBlockPos().offset(0,0,2);level.setBlockAndUpdate(pos,crucible.defaultBlockState());
            var target=((tc4revival.content.CrucibleBlockEntity)level.getBlockEntity(pos)).fluids();
            later(30,()->check("everfull stream travel delay",target.getFluidInTank(0).getAmount()==0));
            later(90,()->check("everfull refills real crucible",target.getFluidInTank(0).getAmount()>0));
        });
        test("think tank",()->{
            var think=machine("think_tank",BASE.offset(14,0,0));level.setBlockAndUpdate(think.getBlockPos().below(),Blocks.BOOKSHELF.defaultBlockState());
            for(var pos:BlockPos.betweenClosed(think.getBlockPos().offset(-2,-1,-2),think.getBlockPos().offset(2,-1,2)))if(!pos.equals(think.getBlockPos().below()))level.removeBlock(pos,false);
            var input=think.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
            var output=think.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(IllegalStateException::new);
            check("think input rejects stone",input.insertItem(0,new ItemStack(Items.STONE),false).getCount()==1);
            input.insertItem(0,new ItemStack(Items.BOOK,8),false);
            check("think input cannot extract",input.extractItem(0,1,false).isEmpty());
            check("think output cannot insert",output.insertItem(0,new ItemStack(Items.BOOK),false).getCount()==1);
            later(205,()->check("think tank consumes book in 200 ticks",think.items.getStackInSlot(0).getCount()==7));
            later(210,()->level.setBlockAndUpdate(think.getBlockPos().east(),Blocks.STONE.defaultBlockState()));
            later(415,()->{check("blocked think tank stops",think.items.getStackInSlot(0).getCount()==7&&think.progress==0);level.removeBlock(think.getBlockPos().east(),false);});
        });
        test("replication",()->{
            check("replication disallows containers",!MachineEntity.canReplicate(new ItemStack(Items.CHEST)));
            var tagged=new ItemStack(Items.STONE);tagged.getOrCreateTag().putString("test","tag");check("replication disallows NBT",!MachineEntity.canReplicate(tagged));
            check("replication includes vanilla wood",MachineEntity.canReplicate(new ItemStack(Items.OAK_PLANKS)));
            var rep=machine("replicator",BASE.offset(19,0,0));rep.template=new ItemStack(Items.STONE);rep.startReplication();check("replicator starts",rep.crafting);
            var cost=tc4revival.aspect.item.ItemStackAspects.resolve(rep.template).orElseThrow();int i=0;
            for(var entry:cost.view().entrySet()){
                var jar=bound(level,"bound_jar",BASE.offset(18+i++,0,3),"replica-source-"+entry.getKey().tag(),0);jar.insert(entry.getKey(),entry.getValue(),false);
            }
            later(70,()->{var tag=rep.saveWithoutMetadata();rep.load(tag);check("replication resumes saved payment",rep.crafting);});
            int completion=(int)cost.total()*50+200;
            later(completion,()->check("replication pays and creates one output",!rep.crafting&&rep.items.getStackInSlot(1).is(Items.STONE)&&rep.items.getStackInSlot(1).getCount()==1));
        });
        test("brazier refund",()->{
            var fire=machine("soul_brazier",BASE.offset(5,0,6));fire.owner=OWNER;fire.storedWarp=7;fire.active=true;fire.vis=0;fire.sync();
            var knowledge=tc4revival.research.KnowledgeSavedData.get(server);int before=knowledge.update(OWNER,level.random::nextInt,k->k.warp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT));
            later(20,()->{int after=knowledge.update(OWNER,level.random::nextInt,k->k.warp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT));check("offline owner receives permanent warp refund",after==before+7&&!fire.active&&fire.storedWarp==0);fire.deactivate();int twice=knowledge.update(OWNER,level.random::nextInt,k->k.warp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT));check("warp refunds exactly once",twice==after);});
        });
        test("soul crucible",()->{
            var soul=machine("crucible_souls",BASE.offset(-10,0,5));level.setBlockAndUpdate(new BlockPos(-10,79,6),Blocks.STONE.defaultBlockState());var cow=EntityType.COW.create(level);cow.moveTo(-9.5,80,6.5,0,0);cow.setNoAi(true);cow.setHealth(1);level.addFreshEntity(cow);
            later(20,()->check("soul crucible kills nearby mob",!cow.isAlive()));
            later(25,()->check("soul crucible suppresses drops",level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(soul.getBlockPos()).inflate(3)).isEmpty()));
        });
        later(1500,()->{server.saveEverything(false,true,true);check("test suite reached persistence checkpoint",true);});
    }
    private static void reload(){
        var data=BoundNetworks.get(server.overworld());
        check("server restart preserved shared chest NBT",data.chest("2:test-chest-rebound").getItem(0).getCount()==7&&data.chest("2:test-chest-rebound").getItem(0).getTag().getString("proof").equals("preserved"));
        check("server restart preserved shared jar",data.jar("4:test-jar").contents().amount(MachineEntity.aspect("aer"))==60);
    }
    private static final BlockPos FIRE=new BlockPos(64,80,0);
    private static void lifecycle(){
        var level=server.overworld();level.setChunkForced(4,0,true);
        for(var pos:BlockPos.betweenClosed(FIRE.offset(-1,-2,-1),FIRE.offset(8,2,5)))level.setBlockAndUpdate(pos,pos.getY()==78?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        var nodePos=FIRE.east(4);level.setBlockAndUpdate(nodePos,tc4revival.content.TCArcane.ENERGIZED_NODE.get().defaultBlockState());
        ((tc4revival.content.EnergizedNodeBlockEntity)level.getBlockEntity(nodePos)).configure(new tc4revival.aspect.AspectAmounts().add(MachineEntity.aspect("ignis"),64),tc4revival.content.AuraNodeType.NORMAL,null,"tx-real-node");
        var fire=machine("soul_brazier",FIRE);fire.vis=16;fire.insert(MachineEntity.aspect("mortuus"),16,false);
        // A jar exposes its upper port; the soul crucible deliberately does not.
        var source=bound(level,"bound_jar",FIRE.below(),"brazier-fuel",0);
        for(var aspect:source.contents().aspects())source.extract(aspect,Integer.MAX_VALUE,false);
        source.insert(MachineEntity.aspect("mortuus"),64,false);
        var knowledge=tc4revival.research.KnowledgeSavedData.get(server);knowledge.update(OWNER,level.random::nextInt,k->{k.setWarp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT,11);return null;});
        var owner=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(OWNER,"TxBrazierOwner"));fire.activate(owner);
        check("brazier activates and stores permanent warp",fire.active&&fire.storedWarp==11&&knowledge.update(OWNER,level.random::nextInt,k->k.warp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT))==0);
        level.setChunkForced(4,0,false);
        var urn=machine("everburn_urn",FIRE.offset(6,0,2));var trash=machine("trash_jar",FIRE.offset(6,0,4));trash.insert(MachineEntity.aspect("aer"),10,false);
        var first=bound(level,"bound_chest",FIRE.offset(1,0,4),"break-conservation",5);var second=bound(level,"bound_chest",FIRE.offset(3,0,4),"break-conservation",5);first.chest().clearContent();first.chest().setItem(0,new ItemStack(Items.DIAMOND,13));
        for(var e:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(FIRE).inflate(10)))e.discard();
        level.destroyBlock(first.getBlockPos(),true);level.destroyBlock(second.getBlockPos(),true);
        int diamonds=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(FIRE).inflate(10)).stream().mapToInt(e->e.getItem().is(Items.DIAMOND)?e.getItem().getCount():0).sum();
        check("breaking two bound endpoints drops shared items once",diamonds==13);
        later(250,()->{
            check("energized node supplies everburn urn",urn.vis>0&&urn.vis<=16);
            check("trash jar actually disposes all ten essentia",trash.amount()==0);
            check("active brazier stays fuelled and consumes death essentia",fire.active&&fire.vis>0&&source.amount()<64&&fire.storedWarp==11);
            check("brazier works outside vanilla forced chunks",!level.getForcedChunks().contains(new net.minecraft.world.level.ChunkPos(FIRE).toLong())&&net.minecraftforge.common.world.ForgeChunkManager.hasForcedChunks(level));
            boolean tainted=false;for(var pos:BlockPos.betweenClosed(FIRE.offset(-15,0,-15),FIRE.offset(15,0,15)))if(tc4revival.content.TCBiomes.isTainted(level,pos)){tainted=true;break;}check("active brazier converts real biome columns",tainted);
            server.saveEverything(false,true,true);check("active brazier saved for restart",true);
        });
    }
    private static void lifecycleReload(){
        var level=server.overworld();var fire=(MachineEntity)level.getBlockEntity(FIRE);
        check("restart restores active brazier owner and stored warp",fire!=null&&fire.active&&OWNER.equals(fire.owner)&&fire.storedWarp==11);
        var knowledge=tc4revival.research.KnowledgeSavedData.get(server);
        check("restart does not refund active stored warp",knowledge.update(OWNER,level.random::nextInt,k->k.warp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT))==0);
        var source=(BoundEntity)level.getBlockEntity(FIRE.below());int before=source.amount();
        later(70,()->{check("restored brazier continues consuming fuel",fire.active&&source.amount()<before);level.destroyBlock(FIRE,true);
            check("breaking active brazier returns permanent warp",knowledge.update(OWNER,level.random::nextInt,k->k.warp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT))==11);
            fire.deactivate();check("broken brazier cannot refund twice",knowledge.update(OWNER,level.random::nextInt,k->k.warp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT))==11);
        });
    }
    private static void config(){
        var recipes=server.overworld().getRecipeManager();
        check("bread recipe follows common config",recipes.byKey(ExplorationMod.id("rod_bread")).isPresent()==ExplorationConfig.BREAD.get());
        for(String id:List.of("meteorboots","cometboots"))check(id+" recipe follows common config",recipes.byKey(ExplorationMod.id(id)).isPresent()==ExplorationConfig.FEATURES.get("boots").get());
        for(var entry:tc4revival.research.ResearchCatalogs.bundled().all())if(entry.key().startsWith("thaumicexploration:")){
            String option=ExplorationConfig.RESEARCH.get(entry.key().substring("thaumicexploration:".length()));
            if(option!=null)check("research availability "+entry.key(),tc4revival.research.ResearchCatalogs.enabled(entry)==ExplorationConfig.FEATURES.get(option).get());
        }
    }
    private static void scene(){
        var level=server.overworld();int z=20;
        for(var pos:BlockPos.betweenClosed(new BlockPos(-4,79,16),new BlockPos(24,83,26)))level.setBlockAndUpdate(pos,pos.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        int x=0;for(String name:List.of("think_tank","everfull_urn","everburn_urn","soul_brazier","crucible_souls","replicator","trash_jar")){
            var m=machine(name,new BlockPos(x,80,z));if(name.equals("think_tank")){for(var q:BlockPos.betweenClosed(m.getBlockPos().offset(-2,-1,-2),m.getBlockPos().offset(2,-1,2)))level.removeBlock(q,false);level.setBlockAndUpdate(m.getBlockPos().below(),Blocks.BOOKSHELF.defaultBlockState());m.items.setStackInSlot(0,new ItemStack(Items.ENCHANTED_BOOK,64));}if(name.equals("replicator")){m.template=new ItemStack(Items.STONE);m.sync();}x+=3;
        }
        var jar=bound(level,"bound_jar",new BlockPos(0,80,24),"visual-jar",11);jar.insert(MachineEntity.aspect("aer"),48,false);
        bound(level,"bound_chest",new BlockPos(3,80,24),"visual-chest",14);
        for(int i=0;i<16;i++)level.setBlockAndUpdate(new BlockPos(6+i,81,24),ExplorationMod.block("floating_candle_"+DyeColor.byId(i).getName()).defaultBlockState());
        for(var p:server.getPlayerList().getPlayers())p.teleportTo(level,5,82,17,0,20);
        level.setDayTime(6000);
    }
    private static void player(){
        var p=server.getPlayerList().getPlayers().stream().findFirst().orElseThrow();p.getInventory().clearContent();
        p.addItem(new ItemStack(ExplorationMod.item("chest_seal_red"),8));p.addItem(new ItemStack(ExplorationMod.item("jar_seal_blue"),8));p.addItem(new ItemStack(ExplorationMod.item("boots_meteor")));p.addItem(new ItemStack(ExplorationMod.item("boots_comet")));p.addItem(new ItemStack(ExplorationMod.item("talisman_food")));p.addItem(new ItemStack(Items.BREAD,32));p.addItem(new ItemStack(ExplorationMod.core("thaumonomicon")));
        p.containerMenu.broadcastChanges();
    }
    private static void items(){
        var level=server.overworld();var p=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.fromString("47152737-0011-4053-8017-102011700099"),"TXFunctional"));
        p.moveTo(40,80,0,0,0);p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        test("food reservoir",()->{
            var charm=new ItemStack(ExplorationMod.item("talisman_food"));p.getInventory().setItem(0,charm);p.getInventory().setItem(1,new ItemStack(Items.BREAD,3));p.getInventory().setItem(2,new ItemStack(Items.MUSHROOM_STEW));p.getInventory().setItem(3,new ItemStack(Items.SUSPICIOUS_STEW));p.getInventory().setItem(4,new ItemStack(Items.ROTTEN_FLESH));
            p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);p.tickCount=20;charm.inventoryTick(level,p,0,false);
            check("talisman consumes plain food and preserves bowls",p.getInventory().getItem(1).getCount()==2&&p.getInventory().getItem(2).is(Items.BOWL));
            check("talisman excludes effect foods",p.getInventory().getItem(3).is(Items.SUSPICIOUS_STEW)&&p.getInventory().getItem(4).is(Items.ROTTEN_FLESH));
            check("talisman food and saturation bounded",p.getFoodData().getFoodLevel()==20&&p.getFoodData().getSaturationLevel()<=20&&charm.getTag().getFloat("food")==1);
        });
        p.getInventory().clearContent();
        test("wands",()->{
            var wand=(tc4revival.content.CastingWandItem)ExplorationMod.core("casting_wand");var air=MachineEntity.aspect("aer");
            var amber=wand.create(tc4revival.content.WandParts.Rod.require("thaumicexploration:amber"),tc4revival.content.WandParts.Cap.GOLD,false,false);p.tickCount=100;amber.inventoryTick(level,p,0,true);
            check("amber rod regenerates all six aspects",tc4revival.aspect.TCAspects.catalog().primals().stream().allMatch(a->wand.vis(amber,a)==100));
            check("amber capacity is 10 vis",wand.capacity(amber)==1000);
            var transmute=wand.create(tc4revival.content.WandParts.Rod.require("thaumicexploration:transmutative"),tc4revival.content.WandParts.Cap.GOLD,false,false);transmute.getOrCreateTag().putInt("aer",7500);transmute.inventoryTick(level,p,0,true);
            check("transmutative rod exchanges at 4 to 1",wand.vis(transmute,air)==6750&&wand.vis(transmute,MachineEntity.aspect("terra"))==37);
            var necro=wand.create(tc4revival.content.WandParts.Rod.require("thaumicexploration:necromancer_staff"),tc4revival.content.WandParts.Cap.GOLD,false,false);necro.inventoryTick(level,p,0,true);wand.receiveVis(necro,air,100,false);necro.inventoryTick(level,p,0,true);
            check("necromancer ordinary charging retains 75 percent",wand.vis(necro,air)==75&&wand.isStaff(necro)&&wand.capacity(necro)==20000);
            var orb=new tc4revival.content.AspectOrbEntity(level,p.getX()+1,p.getY()+1,p.getZ(),air,2);level.addFreshEntity(orb);orb.tick();p.takeXpDelay=0;necro.inventoryTick(level,p,0,true);
            check("necromancer absorbs actual vis orb at 4x",!orb.isAlive()&&wand.vis(necro,air)==875);
            var sojourn=wand.create(tc4revival.content.WandParts.Rod.GREATWOOD,tc4revival.content.WandParts.Cap.require("thaumicexploration:sojourner"),false,false);p.setItemInHand(InteractionHand.MAIN_HAND,sojourn);p.tickCount=120;
            var pos=p.blockPosition().east(2);level.setBlockAndUpdate(pos,tc4revival.content.TCArcane.AURA_NODE.get().defaultBlockState());var node=(tc4revival.content.AuraNodeBlockEntity)level.getBlockEntity(pos);
            node.configure(new tc4revival.aspect.AspectAmounts().add(air,2),tc4revival.content.AuraNodeType.NORMAL,null,"tx-node",false);WandBehaviors.tickCap(p);
            check("sojourner drains loaded node",node.aspects().amount(air)==1&&wand.vis(sojourn,air)==100);
            p.tickCount=140;WandBehaviors.tickCap(p);check("sojourner retains last node aspect",node.aspects().amount(air)==1&&wand.vis(sojourn,air)==100);
            level.removeBlock(pos,false);
        });
        test("seal interactions",()->{
            p.getInventory().clearContent();var pos=new BlockPos(40,80,4);level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos);chest.setItem(0,new ItemStack(Items.EMERALD));var seal=new ItemStack(ExplorationMod.item("chest_seal_red"),3);seal.setHoverName(Component.literal("functional"));p.setItemInHand(InteractionHand.MAIN_HAND,seal);
            var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));seal.getItem().useOn(context);
            check("nonempty chest sealing rejected",level.getBlockState(pos).is(Blocks.CHEST)&&seal.getCount()==3);chest.clearContent();seal.getItem().useOn(context);
            check("empty chest sealing consumes one",level.getBlockEntity(pos) instanceof BoundEntity&&seal.getCount()==2);
            var bound=(BoundEntity)level.getBlockEntity(pos);check("named seal network",bound.network.equals("named:functional")&&bound.color==DyeColor.RED.getId());
            var alt=new ItemStack(ExplorationMod.item("chest_seal_blue"));p.setItemInHand(InteractionHand.MAIN_HAND,alt);alt.getItem().useOn(context);
            check("copy seal adopts endpoint colour and network",p.getInventory().items.stream().anyMatch(s->s.is(ExplorationMod.item("chest_seal_red"))&&s.hasTag()&&s.getTag().getString("network").equals(bound.network)));
        });
        test("enchantment events",()->{
            p.getInventory().clearContent();var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(ExplorationMod.BINDING.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,sword);
            var ender=EntityType.ENDERMAN.create(level);ender.moveTo(40,80,0,0,0);level.addFreshEntity(ender);ender.hurt(level.damageSources().playerAttack(p),1);
            check("binding applies slowness and teleport block",ender.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)&&ender.hasEffect(ExplorationMod.BOUND_EFFECT.get()));
            var teleport=new net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity(ender,42,80,0);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(teleport);check("binding cancels enderman teleport",teleport.isCanceled());ender.discard();
            var helm=new ItemStack(Items.IRON_HELMET);helm.enchant(ExplorationMod.NIGHT_VISION.get(),1);p.setItemSlot(EquipmentSlot.HEAD,helm);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(p));check("night vision equipment effect",p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION));p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);p.removeAllEffects();
            var zombie=EntityType.ZOMBIE.create(level);zombie.moveTo(40,80,0,0,0);level.addFreshEntity(zombie);var weapon=new ItemStack(Items.IRON_AXE);weapon.setHoverName(Component.literal("Preserved Axe"));zombie.setItemSlot(EquipmentSlot.MAINHAND,weapon);sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(ExplorationMod.DISARM.get(),3);p.setItemInHand(InteractionHand.MAIN_HAND,sword);
            for(int i=0;i<100&&!zombie.getMainHandItem().isEmpty();i++){zombie.invulnerableTime=0;zombie.setHealth(20);zombie.hurt(level.damageSources().playerAttack(p),.1F);}
            check("disarm removes held item",zombie.getMainHandItem().isEmpty());check("disarm drop retains custom name",level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,zombie.getBoundingBox().inflate(3)).stream().anyMatch(e->e.getItem().is(Items.IRON_AXE)&&e.getItem().getHoverName().getString().equals("Preserved Axe")));zombie.discard();
        });
        p.getInventory().clearContent();p.removeAllEffects();
        if(net.minecraftforge.fml.ModList.get().isLoaded("thaumic_tinkerer"))test("Thaumic Tinkerer integration",()->{
            var enchants=net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS;
            var vamp=enchants.getValue(ResourceLocation.parse("thaumic_tinkerer:vampirism"));
            check("binding rejects actual Tinkerer vampirism",vamp!=null&&!ExplorationMod.BINDING.get().isCompatibleWith(vamp));
            var ascent=enchants.getValue(ResourceLocation.parse("thaumic_tinkerer:ascent_boost"));var legs=new ItemStack(Items.IRON_LEGGINGS);legs.enchant(ascent,2);
            p.setItemSlot(EquipmentSlot.LEGS,legs);p.setItemSlot(EquipmentSlot.FEET,new ItemStack(ExplorationMod.item("boots_meteor")));p.setShiftKeyDown(true);p.setYRot(0);p.setXRot(0);p.setDeltaMovement(Vec3.ZERO);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent(p));
            check("meteor launch respects Ascent II",Math.abs(p.getDeltaMovement().z-8)<.001&&Math.abs(p.getDeltaMovement().y-4D/3)<.001);
            p.setShiftKeyDown(false);p.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);
            try{
                var catalog=(Map<?,?>)Class.forName("ttmodern.EnchanterCatalog").getMethod("entries").invoke(null);
                for(String name:List.of("binding","night_vision","disarm"))check("osmotic enchanter loads "+name,catalog.containsKey(ExplorationMod.id(name)));
            }catch(Exception e){throw new IllegalStateException(e);}
        });
    }
}
