package thaumicomniverse.compat.ponder;

import net.createmod.ponder.api.PonderPalette;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import static thaumicomniverse.compat.ponder.PonderStage.*;

public final class RestoredScenes {
    static void restoredAltar(PonderStage p) {
        p.show(2,1,2,4,4,4);p.text("family",100,3,4,3);
        p.wand(3,4,3);p.waitFor(25);formAltar(p,3,2,3,"thaumicfutures","ancient");
        p.text("ancient",95,3,4,3);p.key();
        p.block(3,2,3,"thaumicfutures:eldritch_pedestal");p.block(3,4,3,"thaumicfutures:eldritch_matrix");
        formAltar(p,3,2,3,"thaumicfutures","eldritch");p.text("eldritch",100,3,4,3);
        for(int x:new int[]{2,4})for(int z:new int[]{2,4})p.block(x,1,z,"thaumicfutures:infusion_speed_stone");
        p.outline("feet",p.box(2,1,2,4,1,4),PonderPalette.BLUE,100);p.text("speed",95,2,1,2);
        for(int x:new int[]{2,4})for(int z:new int[]{2,4})p.block(x,1,z,"thaumicfutures:infusion_efficiency_stone");
        p.text("efficiency",95,2,1,2);p.text("balance",95,3,4,3);
    }
    static void wireless(PonderStage p) {
        p.show(1,1,3,2,2,3);p.show(5,1,2);p.show(5,1,4);p.jar(1,1,3,"ignis",32,true);
        p.text("faces",95,2,2,3);p.key();p.line(2,2,3,5,1,2,PonderPalette.OUTPUT,95);
        p.jar(1,1,3,"ignis",24,true);p.jar(5,1,2,"ignis",8,true);p.text("send",100,2,2,3);
        p.block(2,2,3,"thaumicfutures:essentia_injector","facing","east");
        p.jar(1,1,3,"ignis",0,true);p.line(5,1,2,2,2,3,PonderPalette.INPUT,95);
        p.text("receive",105,2,2,3);p.jar(1,1,3,"ignis",8,true);p.jar(5,1,2,"ignis",0,true);
        p.text("range",100,5,1,2);p.text("demand",95,1,2,3,PonderPalette.RED);
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
}
