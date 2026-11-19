package thaumicfutures.expansion;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.api.crafting.*;
import tc4revival.content.*;

/** TC6 altar families adapted to TC4's direct instability score. */
public final class RestoredInfusion {
    public static final class Family {
        public final String name;
        public final RegistryObject<Block> stone,bricks;
        public final RegistryObject<StairBlock> stairs;
        public final RegistryObject<SlabBlock> slab;
        public final RegistryObject<PedestalBlock> pedestal;
        public final RegistryObject<MatrixBlock> matrix;
        public final RegistryObject<PillarBlock> pillar,top;
        public InfusionAltarMaterials.Material material;
        Family(String name){
            this.name=name;
            stone=RestoredContent.block(name+"_stone",()->new Block(stoneProperties(name)));
            bricks=RestoredContent.block(name+"_stone_bricks",()->new Block(stoneProperties(name)));
            stairs=RestoredContent.block(name+"_stone_stairs",()->new StairBlock(()->stone.get().defaultBlockState(),stoneProperties(name)));
            slab=RestoredContent.block(name+"_stone_slab",()->new SlabBlock(stoneProperties(name)));
            pedestal=RestoredContent.block(name+"_pedestal",PedestalBlock::new);
            matrix=RestoredContent.BLOCKS.register(name+"_matrix",MatrixBlock::new);
            RestoredContent.ITEM_IDS.add(name+"_matrix");
            thaumicfutures.LegacyContent.ITEMS.register(name+"_matrix",()->new MatrixItem(matrix.get()));
            // Formed columns have no item registration or crafting recipe.
            pillar=RestoredContent.BLOCKS.register(name+"_pillar",()->new PillarBlock(false,this));
            top=RestoredContent.BLOCKS.register(name+"_pillar_top",()->new PillarBlock(true,this));
        }
        void register(){
            material=new InfusionAltarMaterials.Material(matrix,pedestal,bricks,stone,pillar,top);
            InfusionAltarMaterials.register(ResourceLocation.parse("thaumicomniverse:"+name),material);
        }
    }
    private static Block.Properties stoneProperties(String family){
        return Block.Properties.copy(Blocks.STONE_BRICKS)
                .strength(family.equals("eldritch")?15:2,family.equals("eldritch")?1000:10);
    }
    public static final Family ANCIENT=new Family("ancient"),ELDRITCH=new Family("eldritch");
    public static final RegistryObject<Block> ANCIENT_GLYPHED=RestoredContent.block("ancient_glyphed_stone",
            ()->new Block(stoneProperties("ancient")));
    public static final RegistryObject<Block> ANCIENT_ROCK=RestoredContent.block("ancient_rock",
            ()->new Block(Block.Properties.copy(Blocks.BEDROCK)));
    public static final RegistryObject<Block> ANCIENT_DOORWAY=RestoredContent.block("ancient_doorway",
            ()->new Block(Block.Properties.copy(Blocks.BEDROCK)));
    public static final RegistryObject<Block> SPEED=RestoredContent.block("infusion_speed_stone",()->new Block(Block.Properties.copy(Blocks.STONE_BRICKS)));
    public static final RegistryObject<Block> EFFICIENCY=RestoredContent.block("infusion_efficiency_stone",()->new Block(Block.Properties.copy(Blocks.STONE_BRICKS)));
    public static final RegistryObject<BlockEntityType<Matrix>> MATRIX_ENTITY=RestoredContent.ENTITIES.register("restored_matrix",
            ()->BlockEntityType.Builder.of(Matrix::new,ANCIENT.matrix.get(),ELDRITCH.matrix.get()).build(null));
    public static final RegistryObject<BlockEntityType<Pedestal>> PEDESTAL_ENTITY=RestoredContent.ENTITIES.register("restored_pedestal",
            ()->BlockEntityType.Builder.of(Pedestal::new,ANCIENT.pedestal.get(),ELDRITCH.pedestal.get()).build(null));
    public static final RegistryObject<BlockEntityType<Pillar>> PILLAR_ENTITY=RestoredContent.ENTITIES.register("restored_pillar",
            ()->BlockEntityType.Builder.of(Pillar::new,ANCIENT.pillar.get(),ELDRITCH.pillar.get()).build(null));
    public static List<Family> families(){return List.of(ANCIENT,ELDRITCH);}
    public static void register(){
        families().forEach(Family::register);
        InfusionDeviceModifiers.register(ResourceLocation.parse("thaumicfutures:restored_altar"),RestoredInfusion::modifiers);
    }
    public static double discount(double nominal){
        if(nominal<=.1)return nominal;
        return Math.min(.4,.1+.3*(-Math.expm1(-(nominal-.1)/.3)));
    }
    public static InfusionDeviceModifiers.Stats modifiers(Level level,BlockPos matrix,InfusionLayout layout){
        if(!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return InfusionDeviceModifiers.Stats.NONE;
        var block=level.getBlockState(matrix).getBlock();
        double nominal=0;int cycle=10,instability=0;
        if(block==ANCIENT.matrix.get()){nominal=.1;cycle=9;instability=1;}
        if(block==ELDRITCH.matrix.get()){nominal=-.05;cycle=7;instability=-2;}
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            var state=level.getBlockState(matrix.offset(x,-3,z));
            if(state.is(SPEED.get())){cycle--;nominal-=.01;}
            if(state.is(EFFICIENCY.get())){cycle++;nominal+=.02;}
        }
        var counted=new HashSet<BlockPos>();
        for(var pos:layout.pedestals()){
            var state=level.getBlockState(pos);
            if(state.is(ANCIENT.pedestal.get()))nominal+=.01;
            if(state.is(ELDRITCH.pedestal.get())){
                nominal-=.0025;
                // Each symmetric pair supplies one direct stability point. Asymmetric
                // placement is already penalised by the ordinary TC4 layout score.
                var mirror=new BlockPos(2*matrix.getX()-pos.getX(),pos.getY(),2*matrix.getZ()-pos.getZ());
                if(!counted.contains(pos)&&layout.pedestals().contains(mirror)&&level.getBlockState(mirror).is(ELDRITCH.pedestal.get())){
                    instability--;counted.add(pos);counted.add(mirror);
                }
            }
        }
        return new InfusionDeviceModifiers.Stats(1-discount(nominal),Math.max(1,cycle)/10d,instability);
    }
    public static Family family(Block block){
        for(var family:families())if(block==family.matrix.get()||block==family.pillar.get()||block==family.top.get())return family;
        return null;
    }
    public static class MatrixBlock extends InfusionMatrixBlock {
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Matrix(pos,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
            return createTickerHelper(type,MATRIX_ENTITY.get(),(world,pos,block,tile)->tile.tick());
        }
        @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
            if(!level.isClientSide&&!next.is(this))dismantle(level,pos,family(this));
            super.onRemove(state,level,pos,next,moving);
        }
    }
    public static class MatrixItem extends net.minecraft.world.item.BlockItem {
        public MatrixItem(Block block){super(block,new net.minecraft.world.item.Item.Properties());}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
                @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return RestoredAltarRenderer.itemRenderer();}
            });
        }
    }
    public static class Matrix extends InfusionMatrixBlockEntity {
        public Matrix(BlockPos pos,BlockState state){super(MATRIX_ENTITY.get(),pos,state);}
        @Override public boolean validLocation(){
            boolean valid=super.validLocation();
            if(!valid&&level!=null&&!level.isClientSide)dismantle(level,worldPosition,family(getBlockState().getBlock()));
            return valid;
        }
    }
    private static void dismantle(Level level,BlockPos matrix,Family family){
        if(family==null)return;
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            var pos=matrix.offset(x,-2,z);if(!level.hasChunkAt(pos))return;
        }
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            var pos=matrix.offset(x,-2,z);
            if(level.getBlockState(pos).is(family.pillar.get()))level.setBlock(pos,family.bricks.get().defaultBlockState(),3);
            if(level.getBlockState(pos.above()).is(family.top.get()))level.setBlock(pos.above(),family.stone.get().defaultBlockState(),3);
        }
    }
    public static class PedestalBlock extends ArcanePedestalBlock {
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Pedestal(pos,state);}
    }
    public static class Pedestal extends ArcanePedestalBlockEntity {
        public Pedestal(BlockPos pos,BlockState state){super(PEDESTAL_ENTITY.get(),pos,state);}
    }
    public static class PillarBlock extends InfusionPillarBlock {
        private final boolean upper;
        private final Family family;
        PillarBlock(boolean upper,Family family){super(upper);this.upper=upper;this.family=family;}
        @Override public RenderShape getRenderShape(BlockState state){return upper?RenderShape.INVISIBLE:RenderShape.ENTITYBLOCK_ANIMATED;}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return upper?null:new Pillar(pos,state);}
        @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block source,BlockPos from,boolean moving){
            if(!level.isClientSide)level.scheduleTick(pos,this,1);
        }
        @Override public void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
            var base=upper?pos.below():pos;
            for(int dx:new int[]{-1,1})for(int dz:new int[]{-1,1}){
                var candidate=base.offset(dx,2,dz);
                if(level.getBlockEntity(candidate) instanceof Matrix matrix&&matrix.validLocation())return;
            }
            if(level.getBlockState(pos).is(this))level.setBlock(pos,(upper?family.stone:family.bricks).get().defaultBlockState(),3);
        }
    }
    public static class Pillar extends InfusionPillarBlockEntity {
        public Pillar(BlockPos pos,BlockState state){super(PILLAR_ENTITY.get(),pos,state);}
    }
    private RestoredInfusion(){}
}
