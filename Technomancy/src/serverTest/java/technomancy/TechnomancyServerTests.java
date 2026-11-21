package technomancy;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

@GameTestHolder("technomancy") @PrefixGameTestTemplate(false)
public class TechnomancyServerTests {
    private static final BlockPos P=new BlockPos(6,3,6);
    private static MachineEntity machine(GameTestHelper h,MachineKind k,BlockPos p){h.setBlock(p,TechRegistry.MACHINES.get(k).get());return (MachineEntity)h.getBlockEntity(p);}
    private static MachineEntity machine(GameTestHelper h,MachineKind k){return machine(h,k,P);}
    private static void ok(GameTestHelper h,boolean value,String message){h.assertTrue(value,message);}
    private static Aspect a(String tag){return MachineLogic.aspect(tag);}
    @GameTest(template="empty") public static void jarSimulationAndPersistence(GameTestHelper h){
        var m=machine(h,MachineKind.QUANTUM_JAR);
        ok(h,m.insert(a("aer"),700,true)==640&&m.amount()==0,"Simulation must not write");
        ok(h,m.insert(a("aer"),700,false)==640&&m.insert(a("ignis"),1,false)==0,"Physical capacity and one aspect");
        ok(h,!m.canInput(Direction.DOWN)&&m.canInput(Direction.UP),"Jar connects from top");
        ok(h,m.extract(a("aer"),25,true)==25&&m.amount()==640,"Extract simulation");
        var saved=m.saveWithoutMetadata();m.extract(a("aer"),640,false);m.load(saved);
        ok(h,m.amount()==640&&m.inventory.getSlots()==2,"Stored contents survive load");
        m.load(new CompoundTag());ok(h,m.inventory.getSlots()==2,"Missing inventory tag preserves slots");h.succeed();
    }
    @GameTest(template="empty") public static void allMachineStateRoundTrips(GameTestHelper h){
        for(var kind:MachineKind.values()){
            var m=machine(h,kind);m.energy=123;m.mana=456;m.progress=17;m.mode=1;m.boost=true;m.owner=java.util.UUID.randomUUID();m.links.add(m.getBlockPos().east(4));m.state.putInt("cursor",912);
            m.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,3));var saved=m.saveWithoutMetadata();
            var restored=MachineEntity.create(m.getBlockPos(),m.getBlockState());restored.load(saved);
            ok(h,restored.energy==123&&restored.mana==456&&restored.progress==17&&restored.boost&&restored.owner.equals(m.owner)&&restored.links.equals(m.links)&&restored.state.getInt("cursor")==912&&restored.inventory.getStackInSlot(0).getCount()==3,"Roundtrip "+kind);
            ok(h,restored.inventory.getSlots()==(kind==MachineKind.EXISTENCE_CROP_HARVESTER?9:2),"Slot count "+kind);
        }h.succeed();
    }
    @GameTest(template="empty") public static void redstoneExchangesAndUpgrade(GameTestHelper h){
        var m=machine(h,MachineKind.ESSENTIA_COIL);var player=FakePlayerFactory.getMinecraft(h.getLevel());player.getInventory().clearContent();player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.REDSTONE,2));m.interact(player,InteractionHand.MAIN_HAND,Direction.UP);
        ok(h,m.redstone==1&&player.getMainHandItem().getCount()==1&&!m.runs(),"Dust installs high mode and consumes one");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GUNPOWDER,2));m.interact(player,InteractionHand.MAIN_HAND,Direction.UP);
        ok(h,m.redstone==0&&m.runs()&&player.getInventory().countItem(Items.REDSTONE)==1,"Old setting refunded");
        MachineLogic.setBoost(m,true,player);ok(h,m.redstone==0&&m.boost&&player.getInventory().countItem(Items.GUNPOWDER)==2,"Boost refunds installed gunpowder");
        MachineLogic.setBoost(m,false,player);ok(h,m.redstone==2,"Removing coil boost restores low mode");h.succeed();
    }
    @GameTest(template="empty") public static void fusorSamplesAndConservation(GameTestHelper h){
        var m=machine(h,MachineKind.ESSENTIA_FUSOR);var player=FakePlayerFactory.getMinecraft(h.getLevel());player.getInventory().clearContent();m.redstone=0;m.energy=1000;
        player.setItemInHand(InteractionHand.MAIN_HAND,EssentiaPhialItem.filled(a("aer")));MachineLogic.fusorInteract(m,player,InteractionHand.MAIN_HAND,Direction.NORTH);
        ok(h,player.getMainHandItem().isEmpty()&&m.amount()==0,"Input sample is stored, not free essentia");
        player.setItemInHand(InteractionHand.MAIN_HAND,EssentiaPhialItem.filled(a("ignis")));MachineLogic.fusorInteract(m,player,InteractionHand.MAIN_HAND,Direction.SOUTH);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.EMPTY_PHIAL.get()));MachineLogic.fusorInteract(m,player,InteractionHand.MAIN_HAND,Direction.EAST);
        ok(h,m.addEssentia(a("aer"),1,Direction.NORTH)==1&&m.addEssentia(a("ignis"),1,Direction.SOUTH)==1,"Configured input sides");
        MachineLogic.tick(m);ok(h,m.energy==0&&m.amount()==1&&m.essentiaType(Direction.EAST).equals(a("lux")),"One fusion uses two inputs and 1000 FE");
        MachineLogic.fusorInteract(m,player,InteractionHand.MAIN_HAND,Direction.NORTH);ok(h,m.state.getInt("side"+Direction.NORTH.ordinal())==1,"Output blocks sample removal");
        ok(h,m.takeEssentia(a("lux"),1,Direction.EAST)==1&&m.takeEssentia(a("lux"),1,Direction.WEST)==0,"Output side only");
        MachineLogic.fusorInteract(m,player,InteractionHand.MAIN_HAND,Direction.NORTH);
        ok(h,player.getInventory().countItem(TCItems.ESSENTIA_PHIAL.get())==1&&m.state.getInt("side"+Direction.NORTH.ordinal())==0,"Exact sample returned");h.succeed();
    }
    @GameTest(template="empty") public static void wirelessEssentiaAndBuffer(GameTestHelper h){
        var coil=machine(h,MachineKind.ESSENTIA_COIL);h.setBlock(P,coil.getBlockState().setValue(MachineBlock.FACING,Direction.DOWN));coil=(MachineEntity)h.getBlockEntity(P);coil.redstone=0;
        var source=machine(h,MachineKind.QUANTUM_JAR,P.east(3));var dest=machine(h,MachineKind.QUANTUM_JAR,P.below());source.insert(a("aer"),3,false);coil.links.add(source.getBlockPos());
        CoilLogic.tick(coil);ok(h,source.amount()==2&&dest.amount()==1,"Wireless transfer conserves essentia");
        h.setBlock(P.below(),TCArcane.ESSENTIA_BUFFER.get());CoilLogic.tick(coil);
        var buffer=(EssentiaTubeBlockEntity)h.getBlockEntity(P.below());ok(h,buffer.revealedAspects().amount(a("aer"))==1&&source.amount()==1,"Buffer accepts wireless output");
        ok(h,coil.takeEssentia(a("aer"),1,Direction.DOWN)==1&&source.amount()==0,"Demand-driven devices can pull from coil");h.succeed();
    }
    @GameTest(template="empty") public static void refusedWirelessTransferIsEscrowed(GameTestHelper h){
        var coil=machine(h,MachineKind.ESSENTIA_COIL);h.setBlock(P,coil.getBlockState().setValue(MachineBlock.FACING,Direction.DOWN));coil=(MachineEntity)h.getBlockEntity(P);coil.redstone=0;
        var source=machine(h,MachineKind.QUANTUM_JAR,P.east(3));source.insert(a("aer"),1,false);coil.links.add(source.getBlockPos());
        var target=machine(h,MachineKind.QUANTUM_JAR,P.below());
        var refusal=new MachineEntity(target.getBlockPos(),target.getBlockState()){@Override public int insert(Aspect a,int n,boolean simulate){return simulate?super.insert(a,n,true):0;}};
        h.getLevel().setBlockEntity(refusal);CoilLogic.tick(coil);
        ok(h,source.amount()==0&&refusal.amount()==0&&coil.state.getInt("essentiaEscrow")==1,"Rejected actual insertion persists in coil");
        var saved=coil.saveWithoutMetadata();coil.load(saved);h.getLevel().setBlockEntity(MachineEntity.create(target.getBlockPos(),target.getBlockState()));CoilLogic.tick(coil);
        ok(h,((MachineEntity)h.getBlockEntity(P.below())).amount()==1&&coil.state.getInt("essentiaEscrow")==0,"Escrow retries once after reload");h.succeed();
    }
    @GameTest(template="empty") public static void wirelessItemsRespectDestinationCapacity(GameTestHelper h){
        var coil=machine(h,MachineKind.ITEM_COIL);coil.redstone=0;h.setBlock(P.north(),Blocks.CHEST);h.setBlock(P.east(3),Blocks.CHEST);
        var source=(ChestBlockEntity)h.getBlockEntity(P.east(3));var dest=(ChestBlockEntity)h.getBlockEntity(P.north());
        for(int i=0;i<dest.getContainerSize();i++)dest.setItem(i,new ItemStack(Items.STONE,64));dest.setItem(0,new ItemStack(Items.STONE,63));source.setItem(0,new ItemStack(Items.STONE,10));coil.links.add(source.getBlockPos());
        CoilLogic.tick(coil);ok(h,source.getItem(0).getCount()==9&&dest.getItem(0).getCount()==64,"Only the one free slot space is transferred");
        CoilLogic.tick(coil);ok(h,source.getItem(0).getCount()==9,"Full destination does not debit source");h.succeed();
    }
    @GameTest(template="empty") public static void purificationSixStagesAndSmelting(GameTestHelper h){
        OreProcessing.installSmelting(h.getLevel().getServer());ItemStack stack=new ItemStack(Items.RAW_IRON);int stage=0;
        for(var kind:new MachineKind[]{MachineKind.IGNIS_INCINERATOR,MachineKind.BOTANICAL_PURIFIER,MachineKind.SANGUINE_INFUSER}){
            for(int i=0;i<2;i++){
                stack=OreProcessing.output(stack,kind,h.getLevel());stage++;
                ok(h,!stack.isEmpty(),"Stage "+stage+" exists");
                var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(stack),h.getLevel()).orElseThrow();
                var result=recipe.getResultItem(h.getLevel().registryAccess());ok(h,result.is(Items.IRON_INGOT)&&result.getCount()==stage+1,"Actual smelting yields "+(stage+1));
            }ok(h,OreProcessing.output(stack,kind,h.getLevel()).isEmpty(),"Each system limited to two passes");
        }ok(h,OreProcessing.output(stack,MachineKind.IGNIS_INCINERATOR,h.getLevel()).isEmpty(),"No seventh pass");h.succeed();
    }
    @GameTest(template="empty") public static void processingConsumesSixtyPayments(GameTestHelper h){
        var m=machine(h,MachineKind.IGNIS_INCINERATOR);m.inventory.setStackInSlot(0,new ItemStack(Items.RAW_IRON));int inserted=0;
        for(int i=0;i<59;i++){inserted+=m.insert(a("ignis"),2,false);OreProcessing.tick(m);}
        ok(h,m.inventory.getStackInSlot(1).isEmpty()&&m.progress==59,"No early result");
        inserted+=m.insert(a("ignis"),2,false);OreProcessing.tick(m);
        ok(h,inserted==120&&m.amount()==0&&m.inventory.getStackInSlot(0).isEmpty()&&m.inventory.getStackInSlot(1).getCount()==1,"First pass costs 120 ignis for one input");h.succeed();
    }
    @GameTest(template="empty") public static void blockedProcessingDoesNotSpend(GameTestHelper h){
        var m=machine(h,MachineKind.IGNIS_INCINERATOR);m.inventory.setStackInSlot(0,new ItemStack(Items.RAW_IRON));m.inventory.setStackInSlot(1,new ItemStack(Items.DIAMOND,64));m.insert(a("ignis"),64,false);
        for(int i=0;i<80;i++)OreProcessing.tick(m);ok(h,m.progress==0&&m.amount()==64&&m.inventory.getStackInSlot(0).getCount()==1,"Blocked output retains all resources");
        m.inventory.setStackInSlot(1,ItemStack.EMPTY);OreProcessing.tick(m);m.inventory.setStackInSlot(0,ItemStack.EMPTY);OreProcessing.tick(m);ok(h,m.progress==0,"Removing input cancels progress");h.succeed();
    }
    @GameTest(template="empty") public static void dynamoFuelAndEnergyFaces(GameTestHelper h){
        var m=machine(h,MachineKind.ESSENTIA_DYNAMO);m.redstone=0;m.insert(a("ignis"),1,false);MachineLogic.tick(m);
        ok(h,m.energy==80&&m.fuel==799&&m.amount()==0,"One ignis yields 800 ordinary generation ticks");
        ok(h,m.getCapability(ForgeCapabilities.ENERGY,Direction.NORTH).isPresent()&&!m.getCapability(ForgeCapabilities.ENERGY,Direction.SOUTH).isPresent(),"Only facing exports FE");
        var cap=m.getCapability(ForgeCapabilities.ENERGY,Direction.NORTH).orElseThrow(IllegalStateException::new);ok(h,cap.extractEnergy(100,true)==80&&m.energy==80,"Simulated FE extraction");h.succeed();
    }
    @GameTest(template="empty") public static void scepterChargingIsSimulationSafe(GameTestHelper h){
        var stack=TechRegistry.item("technoturge_scepter").getDefaultInstance();var wand=(CastingWandItem)stack.getItem();var before=stack.save(new CompoundTag());var cap=stack.getCapability(ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
        ok(h,cap.receiveEnergy(1050,true)==1050&&before.equals(stack.save(new CompoundTag())),"Simulation preserves stack");
        ok(h,cap.receiveEnergy(1050,false)==1050&&cap.getEnergyStored()==1050,"FE accounted as vis plus buffer");
        ok(h,TCAspects.catalog().primals().stream().mapToInt(a->wand.vis(stack,a)).sum()==10,"100 FE per centivis");
        cap.receiveEnergy(Integer.MAX_VALUE,false);ok(h,cap.getEnergyStored()==cap.getMaxEnergyStored()&&cap.receiveEnergy(1,false)==0,"Full capacity rejects further FE");h.succeed();
    }
    @GameTest(template="empty") public static void nodeStructureAndCreation(GameTestHelper h){
        var one=machine(h,MachineKind.NODE_FABRICATOR,new BlockPos(4,2,4));h.setBlock(new BlockPos(4,2,4),one.getBlockState().setValue(MachineBlock.FACING,Direction.SOUTH));one=(MachineEntity)h.getBlockEntity(new BlockPos(4,2,4));
        var two=machine(h,MachineKind.NODE_FABRICATOR,new BlockPos(4,2,10));ok(h,NodeStructure.create(one)&&NodeStructure.create(two),"Two actual 3 by 3 structures");
        one.redstone=0;two.redstone=0;one.insert(a("auram"),61,false);two.insert(a("vitium"),61,false);one.energy=50000000;
        NodeFabrication.begin(one);NodeFabrication.begin(two);ok(h,one.state.getBoolean("running")&&!two.state.getBoolean("running"),"Only one transaction starts");
        for(int i=0;i<199;i++)NodeFabrication.tick(one);ok(h,h.getLevel().isEmptyBlock(NodeFabrication.target(one))&&one.amount()==61,"Charging does not consume early");NodeFabrication.tick(one);
        var node=(AuraNodeBlockEntity)h.getLevel().getBlockEntity(NodeFabrication.target(one));ok(h,node!=null&&node.permanentType()==AuraNodeType.PURE&&one.amount()+two.amount()==0,"Pure node consumes the two stored aspects");
        ok(h,one.energy==50000000-NodeFabrication.parameters(61,61).energy(),"Energy uses pre-drain strength");h.succeed();
    }
    @GameTest(template="empty") public static void nodeAbortRetainsResources(GameTestHelper h){
        var one=machine(h,MachineKind.NODE_FABRICATOR,new BlockPos(4,2,4));h.setBlock(new BlockPos(4,2,4),one.getBlockState().setValue(MachineBlock.FACING,Direction.SOUTH));one=(MachineEntity)h.getBlockEntity(new BlockPos(4,2,4));var two=machine(h,MachineKind.NODE_FABRICATOR,new BlockPos(4,2,10));NodeStructure.create(one);NodeStructure.create(two);
        one.redstone=0;two.redstone=0;one.insert(a("auram"),40,false);two.insert(a("vitium"),40,false);one.energy=50000000;NodeFabrication.begin(one);
        h.getLevel().setBlock(NodeFabrication.target(one),Blocks.STONE.defaultBlockState(),3);NodeFabrication.tick(one);
        ok(h,!one.state.getBoolean("running")&&one.amount()==40&&two.amount()==40&&one.energy==50000000,"Obstructed target aborts without charges");h.succeed();
    }
    @GameTest(template="empty") public static void ritualFrameAndPersistentPurification(GameTestHelper h){
        var m=machine(h,MachineKind.LIGHT_CATALYST);var d=RitualLogic.definitions().stream().filter(r->r.id().equals("purification_1")).findFirst().orElseThrow();
        ok(h,RitualLogic.definitions().size()==16&&!RitualLogic.matches(m,d),"Sixteen rituals and missing frame rejected");
        for(var p:RitualLogic.frame(m.getBlockPos(),0))h.getLevel().setBlock(p,TechRegistry.MACHINES.get(MachineKind.LIGHT_CRYSTAL).get().defaultBlockState(),3);
        RitualLogic.activate(m,null);ok(h,m.state.getString("ritual").equals("purification_1")&&RitualLogic.matches(m,d),"Purification preserves pillars");
        var saved=m.saveWithoutMetadata();m.load(saved);var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.setPos(m.getBlockPos().getX()+.5,m.getBlockPos().getY()+1,m.getBlockPos().getZ()+.5);h.getLevel().addFreshEntity(zombie);RitualLogic.tick(m);
        ok(h,!zombie.isAlive()&&m.state.contains("ritual"),"Saved purification continues to affect hostile mobs");h.succeed();
    }
    @GameTest(template="empty") public static void existenceSacrificeAndTransfer(GameTestHelper h){
        var burner=machine(h,MachineKind.EXISTENCE_BURNER);burner.redstone=0;var cow=EntityType.COW.create(h.getLevel());cow.setPos(burner.getBlockPos().getX()+.5,burner.getBlockPos().getY()+1,burner.getBlockPos().getZ()+.5);h.getLevel().addFreshEntity(cow);ExistenceLogic.tick(burner);
        ok(h,!cow.isAlive()&&ExistenceLogic.power(burner)==2,"Ordinary animal yields two existence");
        var pylon=machine(h,MachineKind.EXISTENCE_PYLON_1,P.east(2));var consumer=machine(h,MachineKind.EXISTENCE_CROP_FERTILISER,P.east(4));pylon.redstone=0;ExistenceLogic.tick(pylon);
        ok(h,ExistenceLogic.power(burner)+ExistenceLogic.power(pylon)+ExistenceLogic.power(consumer)==2&&ExistenceLogic.power(consumer)==2,"Pylon moves exactly the available existence");h.succeed();
    }
    @GameTest(template="empty") public static void harvesterReplantsAndWaitsForOutput(GameTestHelper h){
        var m=machine(h,MachineKind.EXISTENCE_CROP_HARVESTER);m.redstone=0;ExistenceLogic.change(m,100);var pos=P.east();h.setBlock(pos.below(),Blocks.FARMLAND);h.setBlock(pos,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        ExistenceLogic.tick(m);ok(h,h.getBlockState(pos).is(Blocks.WHEAT)&&h.getBlockState(pos).getValue(CropBlock.AGE)==0&&ExistenceLogic.power(m)==70,"Harvest costs thirty and replants once");
        ok(h,java.util.stream.IntStream.range(0,9).anyMatch(i->!m.inventory.getStackInSlot(i).isEmpty()),"Drops stored for automation");h.setBlock(pos,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));ExistenceLogic.tick(m);ok(h,ExistenceLogic.power(m)==70&&h.getBlockState(pos).getValue(CropBlock.AGE)==7,"Pending drops block another harvest");h.succeed();
    }
    @GameTest(template="empty") public static void matureStemsAndHarvestProtection(GameTestHelper h){
        var m=machine(h,MachineKind.EXISTENCE_CROP_HARVESTER);m.redstone=0;ExistenceLogic.change(m,100);
        var pos=P.east();h.setBlock(pos.below(),Blocks.FARMLAND);h.setBlock(pos,Blocks.MELON_STEM.defaultBlockState().setValue(StemBlock.AGE,7));
        var guard=new ProtectionGuard();guard.block=h.absolutePos(pos);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        try {
            ExistenceLogic.tick(m);ok(h,ExistenceLogic.power(m)==100&&h.getBlockState(pos).getValue(StemBlock.AGE)==7&&guard.breaks>0,"Cancelled harvest preserves mature stem and power");
            guard.block=null;guard.place=h.absolutePos(pos);for(int i=0;i<64&&guard.places==0;i++)ExistenceLogic.tick(m);
            ok(h,ExistenceLogic.power(m)==100&&h.getBlockState(pos).getValue(StemBlock.AGE)==7&&guard.places>0,"Cancelled replant preserves mature stem and power");
            guard.place=null;
            // A stem may roll no seeds; no seed means no harvest. Retry its genuine loot roll.
            for(int i=0;i<64&&ExistenceLogic.power(m)==100;i++)ExistenceLogic.tick(m);
            ok(h,ExistenceLogic.power(m)==70&&h.getBlockState(pos).getValue(StemBlock.AGE)==0,"Mature IGrowable-equivalent stem consumes one seed and thirty existence");
        }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(guard);}h.succeed();
    }
    @GameTest(template="empty") public static void consumerProtectionAndInventoryConservation(GameTestHelper h){
        var m=machine(h,MachineKind.ELDRITCH_CONSUMER);m.mode=2;m.redstone=0;m.energy=40000;
        var pos=P.below();h.setBlock(pos,Blocks.CHEST);var chest=(ChestBlockEntity)h.getBlockEntity(pos);chest.setItem(0,new ItemStack(Items.DIAMOND,3));
        var guard=new ProtectionGuard();guard.block=h.absolutePos(pos);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        try {ConsumerLogic.tick(m);ok(h,m.energy==40000&&h.getBlockState(pos).is(Blocks.CHEST)&&chest.getItem(0).getCount()==3,"Cancelled quarry leaves block, inventory and FE intact");}
        finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(guard);}
        ConsumerLogic.tick(m);ok(h,m.energy==20000&&h.getBlockState(pos).isAir(),"One allowed block removal pays once");
        int dropped=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(pos))).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
        ok(h,dropped==3,"Chest inventory follows removal drops without loss or duplication");h.succeed();
    }
    @GameTest(template="empty") public static void ritualCancellationAndTreasureExtraction(GameTestHelper h){
        var m=machine(h,MachineKind.EARTH_CATALYST);var d=RitualLogic.definitions().stream().filter(r->r.id().equals("extraction")).findFirst().orElseThrow();
        for(int ring=0;ring<3;ring++)for(var p:RitualLogic.frame(m.getBlockPos(),ring))h.getLevel().setBlock(p,TechRegistry.MACHINES.get(MachineKind.valueOf(d.frame().get(ring).name()+"_CRYSTAL")).get().defaultBlockState(),3);
        var villager=EntityType.VILLAGER.create(h.getLevel());villager.setPos(m.getBlockPos().getX()+.5,m.getBlockPos().getY()+1,m.getBlockPos().getZ()+.5);villager.getPersistentData().putString("technomancyTreasure","fire_gem");h.getLevel().addFreshEntity(villager);
        var guard=new ProtectionGuard();guard.victim=villager.getUUID();net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        try {RitualLogic.activate(m,null);ok(h,villager.isAlive()&&villager.getPersistentData().getString("technomancyTreasure").equals("fire_gem")&&RitualLogic.matches(m,d),"Cancelled sacrifice preserves villager treasure and ritual pillars");}
        finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(guard);}
        RitualLogic.activate(m,null);ok(h,!villager.isAlive()&&!RitualLogic.matches(m,d),"Successful extraction consumes pillars and villager");
        long count=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(m.getBlockPos()).inflate(3)).stream().filter(e->e.getItem().is(TechRegistry.item("fire_gem"))).mapToInt(e->e.getItem().getCount()).sum();
        ok(h,count==1,"Exactly one extracted treasure");h.succeed();
    }
    @GameTest(template="empty") public static void persistentLargeRitualCursorAndProtectedCell(GameTestHelper h){
        var m=machine(h,MachineKind.DARK_CATALYST);var protectedPos=P.east();h.setBlock(protectedPos,Blocks.DIAMOND_BLOCK);m.state.putString("ritual","black_hole_1");m.active=true;
        var guard=new ProtectionGuard();guard.block=h.absolutePos(protectedPos);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        try {
            RitualLogic.tick(m);int first=m.state.getInt("ritualCursor");ok(h,first==256,"Volume work is capped per tick");
            var saved=m.saveWithoutMetadata();m.load(saved);RitualLogic.tick(m);ok(h,m.state.getInt("ritualCursor")>first&&h.getBlockState(protectedPos).is(Blocks.DIAMOND_BLOCK),"Cursor resumes after NBT load and respects protection");
            for(int i=0;i<65;i++)if(!m.isRemoved())RitualLogic.tick(m);
            ok(h,h.getBlockState(P).isAir()&&h.getBlockState(protectedPos).is(Blocks.DIAMOND_BLOCK)&&guard.breaks>0,"Ritual expires after its effect window, keeping protected cell");
        }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(guard);}h.succeed();
    }
    public static class ProtectionGuard {
        BlockPos block,place;java.util.UUID victim;int breaks,places;
        @net.minecraftforge.eventbus.api.SubscribeEvent public void breaking(net.minecraftforge.event.level.BlockEvent.BreakEvent e){if(e.getPos().equals(block)){breaks++;e.setCanceled(true);}}
        @net.minecraftforge.eventbus.api.SubscribeEvent public void placing(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent e){if(e.getPos().equals(place)){places++;e.setCanceled(true);}}
        @net.minecraftforge.eventbus.api.SubscribeEvent public void hurt(net.minecraftforge.event.entity.living.LivingAttackEvent e){if(e.getEntity().getUUID().equals(victim))e.setCanceled(true);}
    }
    @GameTest(template="empty") public static void nodeRotationHonorsBothFootprints(GameTestHelper h){
        var m=machine(h,MachineKind.NODE_FABRICATOR);ok(h,NodeStructure.create(m),"Build initial structure");
        var old=m.facing();var player=FakePlayerFactory.getMinecraft(h.getLevel());var guard=new ProtectionGuard();
        guard.block=m.getBlockPos().relative(old.getClockWise());net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        try{
            ok(h,!NodeStructure.rotate(m,player)&&m.facing()==old&&NodeStructure.intact(m)&&guard.breaks>0,"Protected old side cannot be removed by rotation");
            guard.block=null;guard.place=m.getBlockPos().relative(old);
            ok(h,!NodeStructure.rotate(m,player)&&m.facing()==old&&NodeStructure.intact(m)&&guard.places>0,"Protected new footprint rejects atomically");
            guard.place=null;ok(h,NodeStructure.rotate(m,player)&&m.facing()==old.getClockWise()&&NodeStructure.intact(m),"Allowed rotation creates all eight reserved cells");
            ok(h,h.getLevel().isEmptyBlock(m.getBlockPos().relative(old.getClockWise())),"Old side is released without leftover cells");
        }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(guard);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=300) public static void electricBellowsRunsRealVanillaFurnace(GameTestHelper h){
        var m=machine(h,MachineKind.GALVANIZED_BELLOWS);m.energy=20000;m.redstone=0;h.setBlock(P.north(),Blocks.FURNACE);
        var furnace=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)h.getBlockEntity(P.north());furnace.setItem(0,new ItemStack(Items.RAW_IRON));
        h.succeedWhen(()->{ok(h,furnace.getItem(2).is(Items.IRON_INGOT)&&furnace.getItem(2).getCount()==1,"Fuel-free furnace finishes from electric bellows");ok(h,m.energy<20000&&m.energy>=0&&furnace.getItem(1).isEmpty(),"Real processing paid FE with no fuel item");});
    }
    @GameTest(template="empty") public static void condenserAndNodeDynamoUseRealStorage(GameTestHelper h){
        var condenser=machine(h,MachineKind.ENERGY_CONDENSER);condenser.redstone=0;condenser.energy=5000000;
        for(int i=0;i<5;i++)condenser.tick();ok(h,condenser.amount()==5&&condenser.energy==0&&condenser.aspect().equals(a("potentia")),"Five million FE converts to five potentia");
        var dynamo=machine(h,MachineKind.NODE_DYNAMO,P.east(4));dynamo.redstone=0;
        h.setBlock(P.east(6),TCArcane.AURA_NODE.get());var node=(AuraNodeBlockEntity)h.getBlockEntity(P.east(6));
        thaumcraft.api.nodes.NodeApi.configure(node,thaumcraft.api.aspects.AspectList.from(new AspectAmounts().add(a("aer"),20)),AuraNodeType.NORMAL,null,false);
        dynamo.ticks=20;dynamo.tick();ok(h,thaumcraft.api.nodes.NodeApi.snapshot(node).charge().unwrap().amount(a("aer"))==19&&dynamo.energy==80&&dynamo.fuel==29,"Node dynamo draws one actual vis for thirty fuel ticks");h.succeed();
    }
    @GameTest(template="empty") public static void killChargesGemAndCloneKeepsPlayerState(GameTestHelper h){
        var player=FakePlayerFactory.getMinecraft(h.getLevel());player.getInventory().clearContent();var gem=TechRegistry.item("existence_gem").getDefaultInstance();player.getInventory().setItem(0,gem);
        var cow=EntityType.COW.create(h.getLevel());cow.setPos(h.absolutePos(P).getX(),h.absolutePos(P).getY(),h.absolutePos(P).getZ());h.getLevel().addFreshEntity(cow);
        cow.hurt(h.getLevel().damageSources().playerAttack(player),Float.MAX_VALUE);ok(h,gem.getDamageValue()==98,"Actual animal death charges two points");
        var original=PlayerPower.data(player);original.putInt("level",27);original.putInt("DARK",11);
        var clone=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TechnoClone"));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,player,true));
        ok(h,PlayerPower.data(clone).getInt("level")==27&&PlayerPower.data(clone).getInt("DARK")==11,"Death clone retains progress and affinities");
        PlayerPower.data(clone).putInt("level",28);ok(h,PlayerPower.data(player).getInt("level")==27,"Cloned state does not alias original");player.getInventory().clearContent();h.succeed();
    }
}
