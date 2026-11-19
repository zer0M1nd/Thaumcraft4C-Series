package thaumicomniverse.compat.ponder;

import net.createmod.ponder.api.PonderPalette;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.*;
import static thaumicomniverse.compat.ponder.PonderStage.*;

final class ThaumcraftScenes {
    static void tables(PonderStage p) {
        p.show(1,1,2); p.text("purpose",70,1,1,2);
        p.show(2,1,2); p.waitFor(15); p.right(1,1,2,"thaumcraft:scribing_tools",false);
        p.waitFor(25); p.block(1,1,2,"thaumcraft:table","part","master","facing","east");
        p.block(2,1,2,"thaumcraft:table","part","extension","facing","west");
        p.key(); p.text("research",90,1,1,2);
        p.show(3,1,3); p.wand(3,1,3); p.waitFor(25);
        p.block(3,1,3,"thaumcraft:arcane_workbench"); p.text("arcane",80,3,1,3);
        p.right(1,1,2,null,false); p.text("use",90,1,1,2);
    }
    static void crucible(PonderStage p) {
        p.show(2,1,2); p.show(2,2,2); p.wand(2,2,2); p.waitFor(25);
        p.block(2,2,2,"thaumcraft:crucible"); p.text("form",80,2,2,2);
        p.outline("heat",p.pos(2,1,2),PonderPalette.RED,80); p.text("heat",80,2,1,2);
        p.key(); p.right(2,2,2,"minecraft:water_bucket",false);
        p.nbt(2,2,2,n->{n.putString("FluidName","water"); n.putInt("Amount",1000);n.putShort("Heat",(short)200);n.remove("Empty");});
        p.text("water",75,2,2,2);
        var wood=item("thaumcraft:greatwood_log");wood.setCount(4);
        var input=p.s.world().createItemEntity(center(2,3,2),new Vec3(0,-.12,0),wood);
        p.waitFor(25);p.s.world().modifyEntity(input,net.minecraft.world.entity.Entity::discard);
        p.nbt(2,2,2,n->tc4revival.aspect.AspectNbt.write(n,new tc4revival.aspect.AspectAmounts()
            .add(tc4revival.aspect.TCAspects.catalog().require("arbor"),12).add(tc4revival.aspect.TCAspects.catalog().require("praecantatio"),4)));
        p.text("recipe",100,2,2,2);
        var catalyst=p.s.world().createItemEntity(center(2,3,2),new Vec3(0,-.12,0),item("minecraft:iron_ingot"));
        p.waitFor(25);p.s.world().modifyEntity(catalyst,net.minecraft.world.entity.Entity::discard);
        p.nbt(2,2,2,n->{n.putInt("Amount",950);tc4revival.aspect.AspectNbt.write(n,new tc4revival.aspect.AspectAmounts().add(tc4revival.aspect.TCAspects.catalog().require("arbor"),12));});
        p.s.world().createItemEntity(center(2,3,2),new Vec3(0,.1,-.04),item("thaumcraft:thaumium_ingot"));
        p.text("result",100,2,2,2);
        p.right(2,2,2,"thaumcraft:casting_wand",true); p.waitFor(20);
        p.nbt(2,2,2,n->{n.putInt("Amount",0);n.putString("Empty","Empty");n.remove("Aspects");});
        p.text("clean",90,2,2,2,PonderPalette.RED);
    }
    static void distillation(PonderStage p) {
        p.show(1,1,3); p.right(1,1,3,"minecraft:coal",false); p.text("furnace",80,1,1,3);
        p.show(1,2,3); p.nbt(1,2,3,n->{n.putString("aspect","ignis");n.putShort("amount",(short)24);});
        p.text("alembic",80,1,2,3);
        p.key(); p.show(2,2,3,5,2,3); p.show(5,1,3); p.text("top",80,5,2,3);
        p.line(1,2,3,5,2,3,PonderPalette.BLUE,100);
        for(int amount=4;amount<=20;amount+=4) {
            p.jar(5,1,3,"ignis",amount,false);
            final int left=24-amount;p.nbt(1,2,3,n->n.putShort("amount",(short)left));p.waitFor(10);
        }
        p.text("suction",100,3,2,3);
        p.right(5,1,3,"thaumcraft:jar_label",false); p.jar(5,1,3,"ignis",20,true);
        p.text("label",100,5,1,3);
        p.jar(5,1,3,"ignis",64,true); p.text("full",85,5,1,3,PonderPalette.RED);
        p.block(5,1,3,"thaumcraft:void_jar");p.jar(5,1,3,"ignis",64,true);
        p.text("void",90,5,1,3,PonderPalette.RED);
    }
    static void suction(PonderStage p) {
        p.show(1,1,3,5,2,3);p.show(3,1,1,3,2,2);
        p.jar(1,1,3,"ignis",24,true);p.jar(5,1,3,"aqua",24,true);
        p.outline("conflict",p.box(2,2,3,4,2,3),PonderPalette.RED,100);
        p.text("conflict",100,3,2,3,PonderPalette.RED);
        p.key();p.wand(3,2,3);
        p.nbt(3,2,3,n->n.putByteArray("open",new byte[]{1,1,1,1,1,0}));
        p.text("disconnect",80,3,2,3);
        p.block(2,2,3,"thaumcraft:essentia_filter");
        p.nbt(2,2,3,n->n.putString("AspectFilter","ignis"));p.right(2,2,3,"thaumcraft:jar_label",false);
        p.text("filter",80,2,2,3);
        p.key();p.block(4,2,3,"thaumcraft:essentia_valve");
        p.nbt(4,2,3,n->{n.putBoolean("flow",true);n.putInt("side",Direction.UP.ordinal());});
        p.right(4,2,3,null,false);p.waitFor(25);p.nbt(4,2,3,n->n.putBoolean("flow",false));
        p.text("valve",85,4,2,3);
        p.block(4,2,3,"thaumcraft:essentia_restrictor");p.text("restrict",85,4,2,3);
        p.block(4,2,3,"thaumcraft:essentia_oneway");p.nbt(4,2,3,n->n.putInt("side",Direction.WEST.ordinal()));
        p.line(3,2,3,5,2,3,PonderPalette.OUTPUT,80);p.text("oneway",85,4,2,3);
        p.block(3,2,3,"thaumcraft:essentia_buffer");p.show(3,2,4);p.text("buffer",100,3,2,3);
    }
    static void infusionBuild(PonderStage p) {
        p.show(2,1,2,4,1,4);p.text("lower",90,2,1,2);
        p.show(2,2,2,4,2,4);p.text("upper",80,2,2,2);
        p.show(3,3,3);p.text("matrix",85,3,3,3);
        p.outline("air",p.box(3,2,2,3,2,4),PonderPalette.GREEN,80);p.text("air",80,3,2,3);
        p.key();p.wand(3,3,3);p.waitFor(25);formAltar(p,3,1,3,"thaumcraft","infusion");
        p.text("activate",110,3,3,3);p.s.rotateCameraY(45);p.waitFor(30);
    }
    static void formAltar(PonderStage p,int x,int y,int z,String namespace,String family) {
        for(int dx:new int[]{-1,1})for(int dz:new int[]{-1,1}) {
            p.block(x+dx,y,z+dz,namespace+":"+family+"_pillar");
            p.block(x+dx,y+1,z+dz,namespace+":"+family+"_pillar_top");
            final int rotation=2+(dx+1)+(dz+1)/2;
            p.nbt(x+dx,y,z+dz,n->n.putByte("orientation",(byte)rotation));
        }
        p.nbt(x,y+2,z,n->n.putBoolean("Active",true));
    }
    static void pedestal(PonderStage p,int x,int y,int z,String item) {
        p.s.world().modifyBlockEntity(at(x,y,z),ArcanePedestalBlockEntity.class,t->t.setItem(0,item==null?ItemStack.EMPTY:item(item)));
    }
    static void infusion(PonderStage p) {
        p.s.rotateCameraY(35); // Look between the front pillars so the central input and result stay visible.
        p.show(3,1,3,5,3,5);formAltar(p,4,1,4,"thaumcraft","infusion");
        pedestal(p,4,1,4,"thaumcraft:thaumium_pickaxe");p.text("center",85,4,1,4);
        p.show(1,1,4);p.show(7,1,4);p.show(4,1,1);p.show(4,1,7);
        pedestal(p,1,1,4,"thaumcraft:fire_shard");pedestal(p,7,1,4,"thaumcraft:fire_shard");
        pedestal(p,4,1,1,"minecraft:diamond");pedestal(p,4,1,7,"thaumcraft:greatwood_log");
        p.text("ingredients",90,1,1,4);
        p.show(1,1,1);p.show(7,1,1);p.show(1,1,7);
        p.jar(1,1,1,"ignis",8,true);p.jar(7,1,1,"perfodio",8,true);p.jar(1,1,7,"sensus",8,true);
        p.key();p.wand(4,3,4);p.line(1,1,1,4,3,4,PonderPalette.BLUE,100);
        p.text("essentia",100,1,1,1);p.jar(1,1,1,"ignis",0,true);p.jar(7,1,1,"perfodio",0,true);p.jar(1,1,7,"sensus",0,true);
        for(int[] a:new int[][]{{1,1,4},{7,1,4},{4,1,1},{4,1,7}}){
            p.line(a[0],a[1],a[2],4,3,4,PonderPalette.INPUT,30);p.waitFor(25);pedestal(p,a[0],a[1],a[2],null);
        }
        pedestal(p,4,1,4,"thaumcraft:elemental_pickaxe");p.text("result",80,4,1,4);
        p.key();p.show(2,1,2);p.show(6,1,6);p.outline("symmetry",p.box(1,1,1,7,1,7),PonderPalette.GREEN,100);
        p.text("stability",100,2,1,2);
        p.hide(7,1,4,7,1,4);p.outline("missing",p.pos(7,1,4),PonderPalette.RED,90);p.text("risk",95,7,1,4,PonderPalette.RED);
    }
    static void thaumatorium(PonderStage p) {
        p.show(2,1,2,2,2,2);p.text("base",80,2,2,2);
        p.show(2,3,2);p.waitFor(15);p.show(2,4,2);p.text("stack",80,2,3,2);
        p.key();p.wand(2,3,2);p.waitFor(25);
        p.block(2,3,2,"thaumcraft:thaumatorium","upper","false","facing","north");
        p.block(2,4,2,"thaumcraft:thaumatorium","upper","true","facing","north");
        p.text("activate",100,2,3,2);
        p.show(0,2,2,1,3,2);p.show(3,3,2,3,4,2);p.text("supply",100,1,3,2);
        p.right(2,3,2,null,false);p.text("recipe",100,2,3,2);
        p.right(3,4,2,"minecraft:iron_ingot",false);p.waitFor(30);
        p.jar(0,2,2,"praecantatio",28,true);
        p.s.world().createItemEntity(center(2,3,1),new Vec3(0,.05,-.04),item("thaumcraft:thaumium_ingot"));
        p.text("result",90,2,3,1);
        p.show(2,3,3);p.text("memory",80,2,3,3);
    }
    static void advancedAlchemy(PonderStage p) {
        p.show(3,1,3);p.text("core",70,3,1,3);
        p.show(2,1,2,4,1,4);p.text("bottom",85,2,1,2);
        p.show(2,2,2);p.show(2,2,4);p.show(4,2,2);p.show(4,2,4);p.text("corners",85,2,2,2);
        p.show(3,2,2);p.show(2,2,3);p.show(4,2,3);p.show(3,2,4);p.text("edges",90,3,2,2);
        p.key();p.wand(3,2,2);p.waitFor(25);
        for(int x=-1;x<=1;x++)for(int y=0;y<=1;y++)for(int z=-1;z<=1;z++){
            int part=AdvancedAlchemyStructure.part(x,y,z);if(part<0)continue;
            p.block(3+x,1+y,3+z,"thaumcraft:advanced_alchemy_furnace","part",Integer.toString(part));
        }
        p.text("activate",110,3,1,3);p.poweredNode(1,1,0);p.text("power",100,1,2,0);
        p.show(5,1,3,6,2,3);p.text("operation",100,3,1,3);
        var input=p.s.world().createItemEntity(center(3,3,3),new Vec3(0,-.1,0),item("minecraft:coal"));
        p.waitFor(30);p.s.world().modifyEntity(input,net.minecraft.world.entity.Entity::discard);
        p.line(3,1,3,6,2,3,PonderPalette.OUTPUT,90);p.jar(6,1,3,"ignis",2,true);p.text("result",90,6,1,3);
    }
    static void infernal(PonderStage p) {
        p.show(2,1,2,4,1,4);p.text("bottom",90,3,1,3);
        p.show(2,2,2,4,2,4);p.text("middle",100,3,2,3);
        p.outline("outlet",p.pos(3,2,2),PonderPalette.OUTPUT,80);p.text("outlet",90,3,2,2);
        p.show(2,3,2,4,3,4);p.text("top",90,3,3,3);
        p.key();p.wand(3,2,2);p.waitFor(25);
        for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++) {
            if(x==1&&y==2&&z==1)continue;
            p.block(x+2,y+1,z+2,"thaumcraft:infernal_furnace","part",Integer.toString(InfernalFurnaceStructure.part(x,y,z,Direction.NORTH)),"layer",Integer.toString(y),"facing","north");
        }
        p.text("activate",110,3,2,2);
        var input=p.s.world().createItemEntity(center(3,4,3),new Vec3(0,-.1,0),item("minecraft:raw_iron"));
        p.waitFor(35);p.s.world().modifyEntity(input,net.minecraft.world.entity.Entity::discard);
        p.s.world().createItemEntity(center(3,2,1),new Vec3(0,.1,-.06),item("minecraft:iron_ingot"));p.text("smelt",95,3,2,2);
        p.s.rotateCameraY(90);p.waitFor(30);p.show(5,2,3);p.text("bellows",95,5,2,3);
    }
    static void nodeJar(PonderStage p) {
        p.show(2,2,2);p.text("node",75,2,2,2);
        p.show(1,1,1,3,1,3);p.show(1,2,1,3,2,3);p.show(1,3,1,3,3,3);
        p.text("glass",95,1,2,2);
        p.show(1,4,1,3,4,3);p.text("cap",85,2,4,2);
        p.key();p.wand(2,4,2);p.text("cost",110,2,4,2);
        p.s.world().setBlocks(p.box(1,1,1,3,4,3),state("minecraft:air"),false);
        p.block(2,2,2,"thaumcraft:node_jar");
        p.s.world().modifyBlockEntity(at(2,2,2),NodeJarBlockEntity.class,t->t.configure(new NodeJarData(
            PonderStage.nodeAspects(),AuraNodeType.NORMAL,null,"ponder")));
        p.text("portable",85,2,2,2);
        p.wand(2,2,2);p.waitFor(25);p.block(2,2,2,"thaumcraft:aura_node");
        p.s.world().modifyBlockEntity(at(2,2,2),AuraNodeBlockEntity.class,t->t.configure(
            PonderStage.nodeAspects(),AuraNodeType.NORMAL,null,"ponder",false));
        p.text("release",95,2,2,2,PonderPalette.RED);
    }
    static void energize(PonderStage p) {
        p.show(2,1,2);p.show(2,2,2);p.text("stabilizer",85,2,1,2);
        p.show(2,3,2);p.text("converter",85,2,3,2);
        p.show(3,3,2);p.key();p.right(3,3,2,null,false);
        p.block(3,3,2,"minecraft:lever","face","wall","facing","east","powered","true");
        p.nbt(2,3,2,n->{n.putInt("status",1);n.putInt("count",400);});p.text("power",100,2,3,2);
        p.block(2,2,2,"thaumcraft:energized_node");p.nbt(2,3,2,n->{n.putInt("status",2);n.putInt("count",1000);});
        p.s.world().modifyBlockEntity(at(2,2,2),tc4revival.content.EnergizedNodeBlockEntity.class,t->t.configure(PonderStage.nodeAspects(),AuraNodeType.NORMAL,null,"ponder"));
        p.text("supply",100,2,2,2);
        p.outline("keep",p.pos(2,1,2),PonderPalette.RED,100);p.text("maintain",110,2,1,2,PonderPalette.RED);
    }
    static void visRelay(PonderStage p) {
        p.poweredNode(1,1,4);p.text("source",80,1,2,4);
        p.show(4,1,4,4,2,4);p.nbt(4,2,4,n->n.putLong("Parent",at(1,2,4).asLong()));
        p.line(1,2,4,4,2,4,PonderPalette.BLUE,90);p.text("range",90,4,2,4);
        p.show(7,1,4,7,3,4);p.nbt(7,2,4,n->n.putLong("Parent",at(4,2,4).asLong()));
        p.key();p.line(4,2,4,7,2,4,PonderPalette.BLUE,100);p.text("consumer",100,7,2,4);
        p.block(3,2,4,"minecraft:stone");p.show(3,2,4);p.nbt(4,2,4,n->n.remove("Parent"));
        p.text("blocked",95,3,2,4,PonderPalette.RED);p.block(3,2,4,"minecraft:air");
        p.nbt(4,2,4,n->n.putLong("Parent",at(1,2,4).asLong()));
        p.wand(4,2,4);p.nbt(4,2,4,n->n.putByte("color",(byte)0));p.text("attune",100,4,2,4);
    }
    static void bore(PonderStage p) {
        p.show(2,1,3);p.show(2,2,3);p.text("mount",90,2,2,3);
        p.right(2,2,3,"thaumcraft:focus_excavation",false);p.waitFor(25);
        p.right(2,2,3,"minecraft:iron_pickaxe",false);p.text("tools",95,2,2,3);
        p.s.world().modifyBlockEntity(at(2,2,3),ArcaneBoreBlockEntity.class,t->{t.setItem(0,item("thaumcraft:focus_excavation"));t.setItem(1,item("minecraft:iron_pickaxe"));});
        p.show(1,1,3);p.show(2,1,2);p.show(4,1,2,5,3,4);p.text("output",85,1,1,3);
        p.key();p.right(2,1,2,null,false);p.block(2,1,2,"minecraft:lever","powered","true");
        p.line(2,2,3,4,2,3,PonderPalette.RED,90);p.waitFor(20);
        p.s.world().destroyBlock(at(4,2,3));p.waitFor(10);
        p.s.world().destroyBlock(at(5,2,3));p.s.world().createItemEntity(center(1,2,3),Vec3.ZERO,item("minecraft:cobblestone"));
        p.text("dig",95,4,2,3);p.text("boost",100,2,2,3);
    }
    static void eldritch(PonderStage p) {
        p.show(2,1,2,4,5,4);p.text("natural",100,3,1,3);
        for(int i=1;i<=4;i++){p.right(3,1,3,"thaumcraft:eldritch_eye",false);final int eyes=i;p.nbt(3,1,3,n->n.putByte("eyes",(byte)eyes));p.waitFor(30);}
        p.text("eyes",90,3,1,3);p.key();p.wand(3,1,3);p.text("research",110,3,1,3);
        p.block(3,2,3,"thaumcraft:eldritch_portal");p.nbt(3,1,3,n->n.putBoolean("open",true));
        for(int i=0;i<31;i++) {
            p.s.world().modifyBlockEntity(at(3,2,3),EldritchPortalBlockEntity.class,EldritchPortalBlockEntity::tick);
            p.waitFor(1);
        }
        p.text("portal",95,3,2,3);p.text("warning",95,3,1,3,PonderPalette.RED);
    }
    private ThaumcraftScenes() {}
}
