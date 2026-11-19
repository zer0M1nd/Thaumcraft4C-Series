package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class GrateItemServerTests {
    @GameTest(template="wand_test_empty")
    public static void grateOutputPassesClosedGrateAndRestoresCollision(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,4,3));
        var states=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int y=-2;y<=1;y++){var p=pos.offset(0,y,0);states.put(p,level.getBlockState(p));level.setBlock(p,Blocks.AIR.defaultBlockState(),3);}
        GrateItemEntity item=null;
        try {
            level.setBlock(pos.below(2),Blocks.STONE.defaultBlockState(),3);
            level.setBlock(pos,TCArcane.METAL_GRATE.get().defaultBlockState(),3);
            var tile=(MetalGrateBlockEntity)level.getBlockEntity(pos);var input=new ItemStack(Items.DIAMOND,3);tile.setItem(0,input);
            var outputs=level.getEntitiesOfClass(GrateItemEntity.class,new AABB(pos).inflate(1));
            test.assertTrue(outputs.size()==1 && input.getCount()==3,"Actual tile output creates one dedicated entity without modifying the caller's stack");item=outputs.get(0);
            test.assertTrue(item.getDeltaMovement().y==-.1 && item.getItem().getCount()==3,"Output keeps original downward velocity and count");
            var saved=new CompoundTag();item.save(saved);
            var restored=EntityType.loadEntityRecursive(saved,level,entity->entity);
            test.assertTrue(restored instanceof GrateItemEntity && ((GrateItemEntity)restored).getItem().getCount()==3,"Registered subtype and inventory survive entity NBT round trip");
            level.setBlock(pos,level.getBlockState(pos).setValue(MetalGrateBlock.CLOSED,true),3);
            // Exercise an actual overlap with the closed plate, where vanilla ejection would interfere.
            item.setPos(pos.getX()+.5,pos.getY()+.82,pos.getZ()+.5);
            for(int tick=0;tick<25;tick++)item.tick();
            test.assertTrue(item.getY()<pos.getY() && item.getY()>=pos.getY()-1 && item.onGround(),"Output crosses the closed grate then lands on the solid floor below");
            test.assertTrue(!item.noPhysics && item.getItem().getCount()==3 && item.isAlive(),"Collision returns after leaving the grate; the output is neither duplicated nor lost");
        } finally {if(item!=null)item.discard();states.forEach((p,state)->level.setBlock(p,state,3));}
        tc4revival.TC4Revival.LOGGER.info("Grate item PASS: closed-grate-exit-collision-persistence");test.succeed();
    }
}
