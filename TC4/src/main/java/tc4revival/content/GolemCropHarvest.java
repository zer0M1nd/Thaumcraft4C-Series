package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

/** TC4's built-in crop set and order-upgrade replanting; registered crop rules take precedence. */
public  class GolemCropHarvest {
    private static final GameProfile PROFILE=new GameProfile(UUID.nameUUIDFromBytes("FakeThaumcraftGolem".getBytes(StandardCharsets.UTF_8)),"FakeThaumcraftGolem");
    private static final int[] X={0,0,1,1,-1,0,-1,-1,1},Z={0,1,0,1,0,-1,-1,1,-1};
    private GolemCropHarvest() {}
    public static boolean mature(ServerLevel level,BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
        var state=level.getBlockState(pos);
        var registered=thaumcraft.api.golems.GolemCropRegistry.find(state);
        if(registered.isPresent())return registered.get().mature(level,pos,state);
        var agricraft=AgriCraftCropHarvest.mature(level,pos);
        if(agricraft.isPresent())return agricraft.get();
        if (column(state)) return level.getBlockState(pos.below()).is(state.getBlock())
                && !level.getBlockState(pos.below(2)).is(state.getBlock());
        if (ModernCropHarvest.mature(state)) return true;
        if (state.is(Blocks.NETHER_WART)) return state.getValue(NetherWartBlock.AGE)>=3;
        if (state.is(Blocks.COCOA)) return state.getValue(CocoaBlock.AGE)>=2;
        if (state.is(TCArcane.MANA_POD.get())) return state.getValue(ManaPodBlock.AGE)==7;
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN)) return true;
        return false;
    }
    static boolean column(BlockState state) {
        return state.getBlock() instanceof SugarCaneBlock || state.getBlock() instanceof CactusBlock;
    }
    public static void harvest(GolemWorkHost host,BlockPos pos,BlockState original) {
        if (!(host.mob().level() instanceof ServerLevel level)) return;
        var player=FakePlayerFactory.get(level,PROFILE);
        player.setPos(host.mob().position());
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).equals(original)||!mature(level,pos))return;
        var context=new thaumcraft.api.golems.GolemCropRegistry.Context(host,level,player,pos,original,host.configuration().upgrades(4)>0);
        var registered=thaumcraft.api.golems.GolemCropRegistry.find(original);
        if(registered.isPresent()) {
            if(context.allowed(pos)&&registered.get().harvest(context))host.action();
            return;
        }
        var agricraft=AgriCraftCropHarvest.harvest(context);
        if(agricraft.isPresent()) { if(agricraft.get())host.action();return; }
        if(ModernCropHarvest.pickedByUse(original)) {
            if(ModernCropHarvest.pick(context))host.action();
            return;
        }
        if(!context.breakBlock(pos,true))return;
        if (!column(original) && host.configuration().upgrades(4)>0) replant(host,level,player,pos,original);
        host.action();
    }
    private static void replant(GolemWorkHost host,ServerLevel level,FakePlayer player,BlockPos pos,BlockState original) {
        var center=host.mob().position();
        for (var entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(center,center).inflate(6))) {
            if (entity.tickCount<2) {
                var direction=entity.position().subtract(center).normalize();
                entity.setDeltaMovement(-direction.x/4,.075,-direction.z/4);
            }
            var stack=entity.getItem().copy(); boolean done=false;
            if (stack.is(Items.COCOA_BEANS) && original.is(Blocks.COCOA)) {
                Direction facing=original.getValue(CocoaBlock.FACING);
                if (level.getBlockState(pos).canBeReplaced() && useCopy(player,stack,pos.relative(facing),facing.getOpposite())) {
                    stack.shrink(1);done=true;
                }
            } else if (stack.is(TCItems.MANA_BEAN.get()) && original.is(TCArcane.MANA_POD.get())) {
                if (level.getBlockState(pos).canBeReplaced() && TCArcane.MANA_POD.get().defaultBlockState().canSurvive(level,pos)
                        && useCopy(player,stack,pos.above(),Direction.DOWN)) { stack.shrink(1);done=true; }
            } else if (plantable(stack) && seedFor(stack,level,pos,original)) {
                for (int index=0;index<X.length && !stack.isEmpty();index++)
                    if (useCopy(player,stack,pos.offset(X[index],-1,Z[index]),Direction.UP)) stack.shrink(1);
            }
            if (stack.isEmpty()) entity.discard(); else entity.setItem(stack);
            if (done) break;
        }
    }
    private static boolean seedFor(ItemStack stack,ServerLevel level,BlockPos pos,BlockState original) {
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item) return item.getBlock()==original.getBlock();
        return stack.getItem() instanceof net.minecraftforge.common.IPlantable plant && plant.getPlant(level,pos).is(original.getBlock());
    }
    private static boolean plantable(ItemStack stack) {
        return ModernCropHarvest.plantable(stack);
    }
    private static boolean useCopy(FakePlayer player,ItemStack input,BlockPos pos,Direction face) {
        var previous=player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND,input.copy());
        try {
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false);
            int before=player.getMainHandItem().getCount();
            var result=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            return result.consumesAction() && player.getMainHandItem().getCount()<before;
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND,previous); }
    }
}
