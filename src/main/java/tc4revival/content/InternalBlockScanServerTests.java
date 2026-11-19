package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class InternalBlockScanServerTests {
    @GameTest(template="wand_test_empty")
    public static void hiddenOutlineBlocksRetainLegacyScanRaysAndEntityOcclusion(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"InternalScanAudit"));
        player.setPos(pos.getX()+.5,pos.getY()+.5-player.getEyeHeight(),pos.getZ()-2);player.setYRot(0);player.setXRot(0);
        var blocks=List.of(TCArcane.PORTABLE_HOLE.get(),TCEldritch.VOID.get(),TCEldritch.PORTAL.get());
        String[] old={"blockHole","blockEldritchNothing","blockPortalEldritch"};int states=0;
        level.setBlock(pos.south(2),Blocks.DIAMOND_BLOCK.defaultBlockState(),18);
        for(int kind=0;kind<blocks.size();kind++)for(var state:blocks.get(kind).getStateDefinition().getPossibleStates()) {
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);level.setBlock(pos,state,18);
            test.assertTrue(level.getBlockState(pos).getShape(level,pos).isEmpty(),"Restoring scan rays does not create a visible selection outline");
            var expected=Integer.toString(LegacyItemMappings.identity().scanHash(new Key("Thaumcraft:"+old[kind],0)));
            var direct=ScanTargetResolver.block(level,pos).orElseThrow();var selected=ScanTargetSelector.select(player).orElseThrow();
            test.assertTrue(direct.identity().orElseThrow().equals(expected),"Direct scan keeps saved scan identity compatibility");
            test.assertTrue(selected.target().identity().equals(ScanTargetResolver.block(level,pos.south(2)).orElseThrow().identity()),"Native outline ray reaches diamond behind invisible blocks");
            var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,player.getEyeY(),pos.getZ()+1.5,new ItemStack(Items.DIAMOND));
            item.setPos(item.getX(),player.getEyeY()-item.getEyeHeight(),item.getZ());item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);item.setNoGravity(true);level.addFreshEntity(item);
            try {
                test.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(3)).contains(item),"Fixture entity is queryable in loaded chunks");
                var pointed=ScanTargetSelector.pointedEntity(player);
                boolean blocked=level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),item.getEyePosition(),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS;
                test.assertTrue(blocked?pointed.isEmpty():pointed.orElse(null)==item,"Entity LOS follows actual native collision shapes");
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
                test.assertTrue(ScanTargetSelector.pointedEntity(player).orElse(null)==item,"Removing the obstruction exposes the same entity");
            } finally {item.discard();}
            states++;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS hiddenOutlineBlocksRetainLegacyScanRaysAndEntityOcclusion: {} states",states);test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void finiteFluidsScanOnlyAtExactCapacityAndNeverOccludeEntities(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var behind=pos.south(2);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FluidScanAudit"));
        player.setPos(pos.getX()+.5,pos.getY()+.5-player.getEyeHeight(),pos.getZ()-2);player.setYRot(0);player.setXRot(0);
        level.setBlock(behind,Blocks.DIAMOND_BLOCK.defaultBlockState(),18);
        var background=ScanTargetResolver.block(level,behind).orElseThrow();int states=0;
        var blocks=List.of(TCArcane.FLUX_GAS.get(),TCArcane.FLUX_GOO.get(),TCArcane.LIQUID_DEATH.get());
        String[] ids={"blockFluxGas","blockFluxGoo","blockFluidDeath"};int[] full={8,8,4};
        for(int kind=0;kind<blocks.size();kind++)for(int quanta=1;quanta<=9;quanta++) {
            var state=blocks.get(kind).defaultBlockState().setValue(FiniteFluidBlock.QUANTA,quanta);
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);level.setBlock(pos,state,18);
            var selected=ScanTargetSelector.select(player).orElseThrow();
            var identity=quanta==full[kind]?Integer.toString(LegacyItemMappings.identity().scanHash(new Key("Thaumcraft:"+ids[kind],0))):background.identity().orElseThrow();
            test.assertTrue(selected.target().identity().orElseThrow().equals(identity),"Only exact old capacity stops the block ray, including overflow: "+state);
            var expectedOrigin=net.minecraft.world.phys.Vec3.atLowerCornerOf(quanta==full[kind]?pos:behind).add(0,.25,0);
            test.assertTrue(selected.runeOrigin().equals(expectedOrigin),"Fluid/source versus background rune position");
            var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,player.getEyeY(),pos.getZ()+1.5,new ItemStack(Items.DIAMOND));
            item.setPos(item.getX(),player.getEyeY()-item.getEyeHeight(),item.getZ());item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);item.setNoGravity(true);level.addFreshEntity(item);
            try {test.assertTrue(ScanTargetSelector.pointedEntity(player).orElse(null)==item,"Finite fluid never blocks legacy entity LOS: "+state);}finally{item.discard();}
            states++;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS finiteFluidsScanOnlyAtExactCapacityAndNeverOccludeEntities: {} states",states);test.succeed();
    }

}
