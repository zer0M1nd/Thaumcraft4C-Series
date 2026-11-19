package thaumicomniverse.compat.ponder;

import net.createmod.ponder.api.PonderPalette;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumicomniverse.compat.create.*;
import static thaumicomniverse.compat.ponder.PonderStage.*;

final class OmniverseScenes {
    static void crushingWheel(PonderStage p){
        p.show(2,2,3,2,2,4);p.show(4,2,3,4,2,4);p.show(3,1,3);
        p.outline("pair",p.box(2,2,3,4,2,3),PonderPalette.BLUE,90);p.text("pair",95,3,2,3);
        p.speed(p.box(2,2,3,2,2,4),-32);p.speed(p.box(4,2,3,4,2,4),32);
        p.s.effects().rotationDirectionIndicator(at(2,2,3));p.s.effects().rotationDirectionIndicator(at(4,2,3));
        p.line(3,4,3,3,1,3,PonderPalette.INPUT,95);p.text("rotation",100,3,2,3);
        p.key();
        var input=p.s.world().createItemEntity(center(3,4,3),new Vec3(0,-.1,0),item("minecraft:oak_log"));
        p.waitFor(35);p.s.world().modifyEntity(input,Entity::discard);
        var wood=tc4revival.content.TCItems.ESSENCE_CRYSTAL.get().of(tc4revival.aspect.TCAspects.catalog().require("arbor"),4);
        var output=p.s.world().createItemEntity(center(3,1,3),new Vec3(0,0,-.05),wood);
        p.text("aspects",110,3,1,3);p.text("speed",95,2,2,3);
        p.s.world().modifyEntity(output,Entity::discard);
        p.text("empty",105,3,2,3,PonderPalette.RED);
        p.key();
        var creature=p.s.world().createEntity(level->{
            var cow=new net.minecraft.world.entity.animal.Cow(EntityType.COW,level);
            cow.setPos(3.5,4,3.5);cow.setNoAi(true);cow.setDeltaMovement(0,-.1,0);return cow;
        });
        p.waitFor(40);p.s.world().modifyEntity(creature,Entity::discard);
        for(var aspect:java.util.List.of("bestia","terra")){
            var crystal=tc4revival.content.TCItems.ESSENCE_CRYSTAL.get().of(tc4revival.aspect.TCAspects.catalog().require(aspect),3);
            p.s.world().createItemEntity(center(3,1,3),new Vec3(aspect.equals("bestia")?-.06:.06,.05,-.04),crystal);
        }
        p.text("creatures",120,3,2,3);p.text("recipe",160,2,2,3);
    }
    static void convenient(PonderStage p) {
        p.show(1,1,3);p.show(5,1,3);p.jar(1,1,3,"ignis",32,true);
        p.show(1,2,3,5,2,3);p.text("connect",90,1,2,3);
        p.key();p.right(1,2,3,"thaumicomniverse:essentia_servo",false);
        p.nbt(1,2,3,n->n.putInt("Servos",1<<Direction.DOWN.ordinal()));
        p.outline("source",p.pos(1,1,3),PonderPalette.INPUT,90);p.text("servo",95,1,2,3);
        p.line(1,2,3,5,2,3,PonderPalette.OUTPUT,100);
        for(int i=1;i<=8;i++){p.jar(1,1,3,"ignis",32-i,true);p.jar(5,1,3,"ignis",i,false);p.waitFor(10);}
        p.text("active",100,3,2,3);
        p.jar(5,1,3,"ignis",64,false);p.text("full",85,5,1,3,PonderPalette.RED);
        p.right(1,2,3,"thaumcraft:casting_wand",true);p.nbt(1,2,3,n->n.putInt("Servos",0));
        p.text("remove",90,1,2,3);
    }
    static void routing(PonderStage p) {
        p.show(1,1,3,3,2,3);p.jar(1,1,3,"ignis",48,true);p.nbt(1,2,3,n->n.putInt("Servos",1));
        p.show(3,2,1,5,2,2);p.show(5,1,1);p.show(3,2,4,5,2,5);p.show(5,1,5);
        p.text("branches",85,3,2,3);
        p.right(4,2,1,"thaumcraft:jar_label",false);p.nbt(4,2,1,n->n.putString("AspectFilter","ignis"));
        p.right(4,2,5,"thaumcraft:jar_label",false);p.nbt(4,2,5,n->n.putString("AspectFilter","aqua"));
        p.line(3,2,3,5,2,1,PonderPalette.GREEN,90);p.jar(5,1,1,"ignis",8,true);p.text("filter",95,4,2,1);
        p.key();p.block(4,2,5,"thaumicomniverse:convenient_essentia_restrictor");
        p.text("priority",100,4,2,5);
        p.jar(5,1,1,"ignis",64,true);p.jar(5,1,5,"ignis",8,false);p.line(3,2,3,5,2,5,PonderPalette.OUTPUT,90);
        p.text("overflow",95,5,1,5);
        p.wand(3,2,4);p.nbt(3,2,4,n->n.putByteArray("open",new byte[]{1,1,0,1,1,1}));
        p.text("isolate",90,3,2,4);
    }
    static void motor(PonderStage p) {
        p.show(1,1,3,5,1,3);p.speed(p.box(1,1,3,5,1,3),16);
        p.text("base",85,1,1,3);
        p.poweredNode(1,1,1);p.line(1,2,1,1,1,3,PonderPalette.BLUE,100);
        p.key();p.nbt(1,1,3,n->{n.putInt("FireCV",4);n.putInt("OrderCV",0);n.putInt("AirCV",0);});
        p.text("fire",95,1,1,3);
        p.nbt(1,1,3,n->n.putInt("OrderCV",4));p.text("order",90,1,1,3);
        p.nbt(1,1,3,n->n.putInt("AirCV",4));p.speed(p.box(1,1,3,5,1,3),64);
        p.s.effects().rotationSpeedIndicator(at(3,1,3));p.text("air",100,3,1,3);
        p.text("budget",95,1,2,1,PonderPalette.RED);
    }
    static void burner(PonderStage p) {
        p.show(2,1,2);p.text("empty",75,2,1,2);
        var blaze=p.s.world().createEntity(level->{var entity=new Blaze(EntityType.BLAZE,level);entity.setPos(3.5,1,2.5);entity.setNoAi(true);return entity;});
        p.right(3,2,2,"thaumicomniverse:empty_arcane_blaze_burner",false);p.waitFor(30);p.s.world().modifyEntity(blaze,Entity::discard);
        p.block(2,1,2,"thaumicomniverse:arcane_blaze_burner","blaze","smouldering");p.text("capture",85,2,1,2);
        p.show(0,1,2,1,2,2);p.jar(0,1,2,"ignis",16,true);p.key();
        p.block(2,1,2,"thaumicomniverse:arcane_blaze_burner","blaze","kindled");
        p.nbt(2,1,2,n->{n.putInt("Ignis",4);n.putInt("fuelLevel",1);n.putInt("burnTimeRemaining",400);});
        p.text("fire",90,2,1,2);
        p.show(3,1,2,4,2,2);p.jar(4,1,2,"potentia",16,true);p.block(2,1,2,"thaumicomniverse:arcane_blaze_burner","blaze","seething");
        p.nbt(2,1,2,n->{n.putInt("Ignis",4);n.putInt("Potentia",4);n.putInt("fuelLevel",2);n.putInt("burnTimeRemaining",100);});
        p.text("power",100,2,1,2);p.show(2,2,2);p.text("heat",90,2,2,2);
    }
    static void portableInterface(PonderStage p) {
        p.show(1,1,3,2,2,3);p.jar(1,1,3,"ignis",32,true);
        var moving=p.s.world().showIndependentSection(p.box(4,1,3,6,2,3),Direction.DOWN);
        p.s.world().moveSection(moving,new Vec3(1,0,0),0);p.text("pair",95,2,2,3);
        p.key();p.s.world().moveSection(moving,new Vec3(-1,0,0),30);p.waitFor(35);
        for(int x:new int[]{2,4})p.nbt(x,2,3,n->{n.putFloat("Distance",1);n.putFloat("Timer",12);});
        p.text("dock",100,3,2,3);
        p.line(2,2,3,4,2,3,PonderPalette.OUTPUT,100);p.jar(1,1,3,"ignis",16,true);p.jar(5,1,3,"ignis",16,false);
        p.text("storage",100,5,1,3);
        p.text("back",90,1,2,3);
        for(int x:new int[]{2,4})p.nbt(x,2,3,n->n.putFloat("Timer",2));
        p.s.world().moveSection(moving,new Vec3(1,0,0),30);p.text("release",85,3,2,3);
    }
    static void charger(PonderStage p) {
        p.poweredNode(1,1,3);p.show(4,1,3);p.text("receive",95,4,1,3);
        p.right(4,1,3,"thaumcraft:casting_wand",false);
        p.s.world().modifyBlockEntity(at(4,1,3),WandCharger.Tile.class,t->t.inventory.setStackInSlot(0,item("thaumcraft:casting_wand")));
        p.nbt(4,1,3,n->{var parent=new CompoundTag();parent.putDouble("X",1.5);parent.putDouble("Y",2.5);parent.putDouble("Z",3.5);n.put("VisParent",parent);});
        p.line(1,2,3,4,1,3,PonderPalette.BLUE,100);p.text("wand",95,4,1,3);
        p.key();p.show(4,3,1);p.show(4,1,1);p.show(5,3,1);
        p.nbt(4,3,1,n->n.put("HeldItem",item("thaumcraft:casting_wand").save(new CompoundTag())));
        p.speed(p.pos(4,3,1),16);p.s.world().moveDeployer(at(4,3,1),1,20);
        p.line(1,2,3,5,3,1,PonderPalette.BLUE,100);p.text("deployer",100,5,3,1);
        p.text("leaf",95,4,1,3,PonderPalette.RED);
    }
    static void chargerCrafting(PonderStage p){
        // Keep inputs visible while explaining the grid, then run Create's scripted crafting animation.
        for(int x=4;x<=6;x++)for(int y=1;y<=3;y++){
            var state=state("create:mechanical_crafter","facing","north");
            var target=x==4&&y>1?Direction.DOWN:Direction.WEST;
            for(var pointing:net.createmod.catnip.math.Pointing.values()){
                var option=state.setValue(com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock.POINTING,pointing);
                if(com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock.getTargetDirection(option)==target){state=option;break;}
            }
            p.s.world().setBlock(at(x,y,4),state,false);
        }
        p.show(4,1,4,6,3,4);p.speed(p.box(4,1,4,6,3,4),0);
        p.nbt(4,3,4,n->n.putBoolean("Cover",true));p.nbt(6,3,4,n->n.putBoolean("Cover",true));
        p.text("grid",110,5,3,4);p.key();p.show(3,2,4);
        p.right(3,2,4,"thaumcraft:casting_wand",false);
        p.s.world().modifyBlockEntity(at(3,2,4),WandCharger.Tile.class,t->t.inventory.setStackInSlot(0,item("thaumcraft:casting_wand")));
        p.text("identity",125,3,2,4);
        p.poweredNode(1,1,1);p.show(3,1,1,3,2,1);
        p.nbt(3,2,1,n->n.putLong("Parent",at(1,2,1).asLong()));
        p.nbt(3,2,4,n->{var parent=new CompoundTag();parent.putDouble("X",3.5);parent.putDouble("Y",2.45);parent.putDouble("Z",1.5);n.put("VisParent",parent);});
        p.text("vis",110,3,2,4);p.key();
        String[][] inputs={{"","minecraft:gold_ingot",""},{"minecraft:quartz_block","minecraft:piston","minecraft:quartz_block"},{"thaumcraft:arcane_stone_bricks","thaumcraft:nitor","thaumcraft:arcane_stone_bricks"}};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)if(!inputs[row][col].isEmpty()){
            var held=item(inputs[row][col]);
            p.s.world().modifyBlockEntity(at(4+col,3-row,4),com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity.class,t->t.getInventory().setStackInSlot(0,held.copy()));
        }
        p.text("orientation",120,5,3,4);
        p.block(3,1,4,"create:depot");p.show(3,1,4);
        p.s.world().setCraftingResult(at(4,1,4),item("thaumcraft:node_stabilizer"));
        p.speed(p.box(4,1,4,6,3,4),64);p.waitFor(160);
        // The completed item sits below the charger; clear foreground teaching aids for the result shot.
        p.hide(0,1,1,3,3,1);p.hide(3,2,4,3,2,4);p.waitFor(25);
        p.text("result",110,3,1,4);
    }
    static void stockpile(PonderStage p) {
        p.s.rotateCameraY(90);
        p.show(2,1,2);p.jar(2,1,2,"ignis",8,true);p.show(2,1,1);p.show(1,1,1);
        p.text("observe",85,2,1,1);
        p.right(2,1,1,null,false);p.text("threshold",100,2,1,1);
        p.key();p.jar(2,1,2,"ignis",48,true);p.block(1,1,1,"minecraft:redstone_lamp","lit","true");
        p.nbt(2,1,1,n->{n.putBoolean("Powered",true);n.putBoolean("PoweredAfterDelay",true);n.putInt("CurrentAmount",48);n.putInt("CurrentMaxAmount",64);n.putInt("OnAboveAmount",48);n.putInt("OffBelowAmount",16);});
        p.text("on",85,1,1,1);
        p.jar(2,1,2,"ignis",32,true);p.text("hold",90,2,1,2);
        p.jar(2,1,2,"ignis",16,true);p.block(1,1,1,"minecraft:redstone_lamp","lit","false");
        p.nbt(2,1,1,n->{n.putBoolean("Powered",false);n.putBoolean("PoweredAfterDelay",false);n.putInt("CurrentAmount",16);});p.text("off",85,1,1,1);
        p.right(2,1,1,"thaumcraft:jar_label",false);p.text("filter",95,2,1,1);
    }
    static void coupler(PonderStage p) {
        p.show(2,1,2);p.show(2,2,2);p.text("lower",85,2,2,2);
        var upper=p.s.world().showIndependentSection(p.box(1,5,2,3,6,2),Direction.DOWN);
        p.text("upper",90,2,5,2);p.key();
        p.line(2,2,2,2,5,2,PonderPalette.GREEN,90);p.text("range",100,2,3,2);
        var lower=p.s.world().makeSectionIndependent(p.box(2,1,2,2,2,2));
        p.s.world().moveSection(lower,new Vec3(1,0,0),40);p.s.world().moveSection(upper,new Vec3(1,0,0),40);
        p.text("move",100,2,5,2);
        p.s.world().moveSection(lower,new Vec3(-1,0,0),30);p.s.world().moveSection(upper,new Vec3(-1,0,0),30);p.waitFor(35);
        p.block(2,3,2,"minecraft:stone");p.show(2,3,2);p.text("blocked",90,2,3,2,PonderPalette.RED);
    }
    static void magnet(PonderStage p) {
        p.show(1,1,4,2,1,4);p.speed(p.box(1,1,4,2,1,4),32);
        var cargo=p.s.world().showIndependentSection(p.box(7,1,3,7,2,4),Direction.DOWN);
        p.text("casing",95,7,1,4);
        p.key();p.line(2,1,4,7,1,4,PonderPalette.BLUE,100);p.s.world().moveSection(cargo,new Vec3(-4,0,0),70);
        p.text("pull",100,2,1,4);p.text("range",105,2,1,4);
        p.right(2,1,4,null,false);p.text("options",110,2,1,4);
        p.show(2,1,5);p.right(2,1,5,null,false);p.block(2,1,5,"minecraft:lever","powered","true");
        p.text("pause",90,2,1,5,PonderPalette.RED);
    }
    static void depot(PonderStage p) {
        p.show(1,1,4);p.right(1,1,4,"thaumicomniverse:arcane_floating_depot",true);
        p.text("select",100,1,1,4);p.key();p.show(6,2,4);p.show(6,1,4);
        p.right(6,1,4,"thaumicomniverse:arcane_floating_depot",false);
        p.nbt(1,1,4,n->{n.putLong("PairedDepot",at(6,2,4).asLong());n.putInt("PlatePhase",0);n.putInt("FlightDuration",24);});
        p.nbt(6,2,4,n->{n.putLong("PairedDepot",at(1,1,4).asLong());n.putBoolean("OutputDepot",true);n.putInt("FlightDuration",24);});
        p.line(1,1,4,6,2,4,PonderPalette.GREEN,100);p.text("distance",110,6,2,4);
        p.s.world().createItemOnBeltLike(at(1,1,4),Direction.UP,item("minecraft:iron_ingot"));
        p.text("threshold",95,1,1,4);p.key();
        clearDepot(p,1,1,4);
        p.nbt(1,1,4,n->{n.putInt("PlatePhase",1);n.putInt("FlightTick",0);n.put("PlateCargo",item("minecraft:iron_ingot").save(new CompoundTag()));});
        p.nbt(6,2,4,n->n.putInt("PlatePhase",1));p.waitFor(25);
        p.nbt(1,1,4,n->{n.putInt("PlatePhase",2);n.put("PlateCargo",new CompoundTag());});
        p.nbt(6,2,4,n->n.putInt("PlatePhase",2));p.s.world().createItemOnBeltLike(at(6,2,4),Direction.UP,item("minecraft:iron_ingot"));
        p.text("unload",95,6,2,4);clearDepot(p,6,2,4);
        p.nbt(1,1,4,n->{n.putInt("PlatePhase",3);n.putInt("FlightTick",0);});p.nbt(6,2,4,n->n.putInt("PlatePhase",3));
        p.waitFor(25);p.nbt(1,1,4,n->n.putInt("PlatePhase",0));p.nbt(6,2,4,n->n.putInt("PlatePhase",0));
        p.text("return",95,1,1,4);p.text("boost",90,1,1,4);
    }
    static void clearDepot(PonderStage p,int x,int y,int z) {
        p.s.world().modifyBlockEntity(at(x,y,z),com.simibubi.create.content.logistics.depot.DepotBlockEntity.class,
            t->t.getBehaviour(com.simibubi.create.content.logistics.depot.DepotBehaviour.TYPE).removeHeldItem());
    }
    static void spout(PonderStage p) {
        p.show(3,1,3);p.show(3,3,3);p.text("mount",85,3,3,3);
        p.show(1,1,3,2,3,3);p.jar(1,1,3,"praecantatio",32,true);
        p.nbt(3,3,3,n->{n.putString("Aspect","praecantatio");n.putInt("Amount",32);n.putInt("ProcessingTicks",-1);});
        p.block(3,3,3,"thaumicomniverse:essentia_spout","lit","true");p.text("feed",95,2,3,3);
        p.key();p.s.world().createItemOnBeltLike(at(3,1,3),Direction.UP,item("minecraft:iron_ingot"));
        p.text("recipe",100,3,1,3);
        p.nbt(3,3,3,n->n.putInt("ProcessingTicks",20));p.waitFor(20);
        p.nbt(3,3,3,n->{n.putInt("ProcessingTicks",-1);n.putInt("Amount",28);});
        clearDepot(p,3,1,3);
        p.s.world().createItemOnBeltLike(at(3,1,3),Direction.UP,item("thaumcraft:thaumium_ingot"));
        p.text("result",90,3,1,3);
        p.text("order",110,3,3,3);
        p.right(3,3,3,"thaumcraft:jar_label",false);p.text("filter",100,3,3,3);
    }
    private OmniverseScenes() {}
}
