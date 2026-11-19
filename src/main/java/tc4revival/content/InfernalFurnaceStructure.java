package tc4revival.content;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Original 3 by 3 by 3 blueprint; origin is the bottom north-west corner. */
public  class InfernalFurnaceStructure {
    private static final ThreadLocal<Integer> MUTATING=ThreadLocal.withInitial(()->0);
    private InfernalFurnaceStructure() {}
    static boolean mutating() { return MUTATING.get()>0; }
    private static <T> T quietly(java.util.function.Supplier<T> work) {
        int depth=MUTATING.get(); MUTATING.set(depth+1);
        try { return work.get(); } finally { if (depth==0) MUTATING.remove(); else MUTATING.set(depth); }
    }
    public static net.minecraft.world.InteractionResult form(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.context.UseOnContext context) {
        var level=context.getLevel(); var clicked=context.getClickedPos(); var block=level.getBlockState(clicked);
        if (!block.is(Blocks.OBSIDIAN) && !block.is(Blocks.NETHER_BRICKS) && !block.is(Blocks.IRON_BARS)) return net.minecraft.world.InteractionResult.PASS;
        if (!(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)) return net.minecraft.world.InteractionResult.PASS;
        if (!tc4revival.research.KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID())
                .map(knowledge->knowledge.researchMarkers().contains("INFERNALFURNACE")).orElse(false)) return net.minecraft.world.InteractionResult.PASS;
        for (int x=-2;x<=0;x++) for (int y=-2;y<=0;y++) for (int z=-2;z<=0;z++) {
            var origin=clicked.offset(x,y,z); var direction=match(level,origin); if (direction.isEmpty()) continue;
            var states=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for (int yy=0;yy<3;yy++) for (int zz=0;zz<3;zz++) for (int xx=0;xx<3;xx++) {
                var pos=origin.offset(xx,yy,zz);
                if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack)) return net.minecraft.world.InteractionResult.FAIL;
                states.put(pos,level.getBlockState(pos));
            }
            var wand=(CastingWandItem)stack.getItem();
            var cost=java.util.Map.of(tc4revival.aspect.TCAspects.catalog().require("ignis"),50,tc4revival.aspect.TCAspects.catalog().require("terra"),50);
            if (!thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:infernal_furnace",wand,stack,player,cost,true,true,false)) continue;
            boolean formed=quietly(()->{
                boolean committed=false;
                try {
                    for (var pos:states.keySet()) {
                        int dx=pos.getX()-origin.getX(),dy=pos.getY()-origin.getY(),dz=pos.getZ()-origin.getZ();
                        if (dx==1 && dy==2 && dz==1) continue;
                        var state=TCArcane.INFERNAL_FURNACE.get().defaultBlockState().setValue(InfernalFurnaceBlock.PART,part(dx,dy,dz,direction.get()))
                                .setValue(InfernalFurnaceBlock.LAYER,dy).setValue(InfernalFurnaceBlock.FACING,direction.get());
                        if (!level.setBlock(pos,state,18) || !level.getBlockState(pos).equals(state)) return false;
                    }
                    if (!(level.getBlockEntity(origin.offset(1,1,1)) instanceof InfernalFurnaceBlockEntity)
                            || !thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:infernal_furnace",wand,stack,player,cost,true,true,true)) return false;
                    committed=true; return true;
                } finally {
                    if (!committed) for (var entry:states.entrySet()) {
                        if (!level.getBlockState(entry.getKey()).equals(entry.getValue())) level.setBlock(entry.getKey(),entry.getValue(),18);
                        if (!level.getBlockState(entry.getKey()).equals(entry.getValue())) throw new IllegalStateException("Cannot restore infernal furnace at "+entry.getKey());
                    }
                }
            });
            if (!formed) return net.minecraft.world.InteractionResult.FAIL;
            for (var pos:states.keySet()) if (level.getBlockState(pos).is(TCArcane.INFERNAL_FURNACE.get())) {
                level.blockEvent(pos,TCArcane.INFERNAL_FURNACE.get(),1,4);
                level.updateNeighborsAt(pos,TCArcane.INFERNAL_FURNACE.get());
            }
            level.playSound(null,origin,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
    public static Block material(int x,int y,int z) {
        if (x==1 && z==1) return y==2 ? Blocks.AIR : y==1 ? Blocks.LAVA : Blocks.OBSIDIAN;
        return x!=1 && z!=1 ? Blocks.NETHER_BRICKS : Blocks.OBSIDIAN;
    }
    public static Optional<Direction> match(Level level,BlockPos origin) {
        Direction outlet=null;
        for (int y=2;y>=0;y--) for (int x=0;x<3;x++) for (int z=0;z<3;z++) {
            var pos=origin.offset(x,y,z);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return Optional.empty();
            var state=level.getBlockState(pos); var expected=material(x,y,z);
            if (state.is(expected) && (expected!=Blocks.LAVA || state.getFluidState().isSource())
                    || expected==Blocks.AIR && state.isAir()) continue;
            if (y!=1 || outlet!=null || !state.is(Blocks.IRON_BARS) || x==z || x!=1 && z!=1) return Optional.empty();
            outlet=x==0 ? Direction.WEST : x==2 ? Direction.EAST : z==0 ? Direction.NORTH : Direction.SOUTH;
        }
        return Optional.ofNullable(outlet);
    }
    /** Position metadata used by the original connected textures and restoration materials. */
    public static int part(int x,int y,int z,Direction outlet) {
        if (x==1 && y==1 && z==1) return 0;
        if (y==1 && x==1+outlet.getStepX() && z==1+outlet.getStepZ()) return 10;
        return 1+x+z*3;
    }
    public static Block restoredMaterial(int part) {
        if (part==0) return Blocks.AIR;
        if (part==10) return Blocks.IRON_BARS;
        return part%2==0 || part==5 ? Blocks.OBSIDIAN : Blocks.NETHER_BRICKS;
    }
    static boolean intact(Level level,BlockPos core) {
        for (int y=-1;y<=1;y++) for (int x=-1;x<=1;x++)
            for (int z=-1;z<=1;z++) {
                if (x==0 && z==0 && (y==0 || y==1)) continue;
                var pos=core.offset(x,y,z);
                if (!level.hasChunkAt(pos)) return true;
                if (!level.getBlockState(pos).is(TCArcane.INFERNAL_FURNACE.get())) return false;
            }
        return true;
    }
    static void dismantle(Level level,BlockPos center) {
        if (level.isClientSide || mutating()) return;
        quietly(()->{
            for (int y=-1;y<=1;y++) for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
                var pos=center.offset(x,y,z); if (!level.hasChunkAt(pos)) continue;
                var state=level.getBlockState(pos); if (!state.is(TCArcane.INFERNAL_FURNACE.get())) continue;
                level.setBlock(pos,restoredMaterial(state.getValue(InfernalFurnaceBlock.PART)).defaultBlockState(),18);
            }
            return true;
        });
        var blaze=net.minecraft.world.entity.EntityType.BLAZE.create(level);
        if (blaze!=null) {
            blaze.moveTo(center.getX()+.5,center.getY()+1,center.getZ()+.5,0,0);
            blaze.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,6000,2));
            blaze.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,12000)); level.addFreshEntity(blaze);
        }
        for (int y=-1;y<=1;y++) for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            var pos=center.offset(x,y,z); if (level.hasChunkAt(pos)) {
                level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock()); level.getBlockState(pos).updateNeighbourShapes(level,pos,3);
            }
        }
    }
}
