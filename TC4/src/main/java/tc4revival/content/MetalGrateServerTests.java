package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class MetalGrateServerTests {
    @GameTest(template="wand_test_empty")
    public static void grateCollisionControlsAndRecipe(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,4,4));var block=TCArcane.METAL_GRATE.get();var open=block.defaultBlockState();level.setBlockAndUpdate(pos,open);
        var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,new ItemStack(Items.DIAMOND));var sheep=EntityType.SHEEP.create(level);sheep.setPos(item.position());
        test.assertTrue(open.getCollisionShape(level,pos,CollisionContext.of(item)).isEmpty()&&!open.getCollisionShape(level,pos,CollisionContext.of(sheep)).isEmpty()&&open.getCollisionShape(level,pos,CollisionContext.empty()).isEmpty(),"Open grate admits items but blocks living entities, and preserves empty-context behavior");
        item.move(MoverType.SELF,new Vec3(0,-.6,0));sheep.move(MoverType.SELF,new Vec3(0,-.6,0));test.assertTrue(item.getY()<pos.getY()+.8&&sheep.getY()>=pos.getY()+1,"Actual item and mob movement across grate");
        var closed=open.setValue(MetalGrateBlock.CLOSED,true);level.setBlockAndUpdate(pos,closed);item.setPos(pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5);item.move(MoverType.SELF,new Vec3(0,-.6,0));test.assertTrue(item.getY()>=pos.getY()+1,"Closed grate blocks falling items");
        block.use(closed,level,pos,test.makeMockPlayer(),net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));test.assertTrue(!level.getBlockState(pos).getValue(MetalGrateBlock.CLOSED),"Right click opens closed grate");
        level.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());test.assertTrue(level.getBlockState(pos).getValue(MetalGrateBlock.CLOSED),"Power closes grate");level.removeBlock(pos.east(),false);test.assertTrue(!level.getBlockState(pos).getValue(MetalGrateBlock.CLOSED),"Removing source reopens grate");
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);grid.setItem(1,new ItemStack(Items.IRON_BARS));grid.setItem(4,new ItemStack(Items.OAK_TRAPDOOR));var recipe=level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:metal_grate")).orElseThrow();
        test.assertTrue(((net.minecraft.world.item.crafting.CraftingRecipe)recipe).matches(grid,level)&&((net.minecraft.world.item.crafting.CraftingRecipe)recipe).assemble(grid,level.registryAccess()).is(TCItems.METAL_GRATE.get()),"Original iron bars over oak trapdoor recipe");pass(test,"collision-controls-recipe");
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void grateHopperAndCapabilities(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,4,4));level.setBlockAndUpdate(pos,TCArcane.METAL_GRATE.get().defaultBlockState());var tile=(MetalGrateBlockEntity)level.getBlockEntity(pos);
        var cap=tile.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.UP);var top=cap.orElseThrow(IllegalStateException::new);
        for(var side:Direction.values())test.assertTrue(tile.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,side).orElseThrow(IllegalStateException::new).getSlots()==(side==Direction.UP?1:0),"Only upper face exposes insertion");
        test.assertTrue(top.insertItem(0,new ItemStack(Items.DIAMOND,3),true).isEmpty()&&tile.isEmpty()&&top.extractItem(0,64,false).isEmpty(),"Simulated insertion accepted without storage or extraction");
        level.setBlockAndUpdate(pos,level.getBlockState(pos).setValue(MetalGrateBlock.CLOSED,true));test.assertTrue(top.getSlots()==0,"Existing capability immediately follows closed state");
        level.setBlockAndUpdate(pos,level.getBlockState(pos).setValue(MetalGrateBlock.CLOSED,false));
        var floor=pos.below(2);var previousFloor=level.getBlockState(floor);level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.above(),Blocks.HOPPER.defaultBlockState());var hopper=(net.minecraft.world.level.block.entity.HopperBlockEntity)level.getBlockEntity(pos.above());hopper.setItem(0,new ItemStack(Items.EMERALD));
        test.runAfterDelay(20,()->{
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2,4,2));
            test.assertTrue(hopper.isEmpty()&&tile.isEmpty()&&drops.stream().filter(e->e.getItem().is(Items.EMERALD)).mapToInt(e->e.getItem().getCount()).sum()==1,"Real hopper transfers exactly one item through non-storing grate: pos="+pos+",hopper="+hopper.getItem(0)+",ticking="+level.isPositionEntityTicking(pos)+",drops="+drops.stream().map(e->e.getItem()+"@"+e.position()).toList());
            test.assertTrue(drops.stream().noneMatch(e->e.getItem().is(Items.DIAMOND)),"Simulation created no entities");
            for(var e:drops)e.discard();level.removeBlock(pos.above(),false);level.removeBlock(pos,false);level.setBlockAndUpdate(floor,previousFloor);test.assertTrue(!cap.isPresent(),"Removed tile invalidates automation capability");pass(test,"hopper-capability-lifecycle");
        });
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Metal grate test passed: {}",name);test.succeed();}
}
