package tc4revival.content;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.HitResult;

/** The TC egg places a mob; it cannot edit spawners or create baby domestic animals. */
public  class TaintedSpawnEggItem extends Item {
    private final java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type;
    private final int secondaryColor;
    public TaintedSpawnEggItem(TaintedAnimalEntity.Kind kind) { this(()->TCEntities.taintedAnimal(kind),kind.eggColor); }
    public TaintedSpawnEggItem(java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> type,int color) { super(new Properties());this.type=type;secondaryColor=color; }
    public int color(int layer) { return layer==0?0xffc0ff:secondaryColor; }
    @Override public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        var pos=context.getClickedPos().relative(context.getClickedFace());
        double lift=context.getClickedFace()==Direction.UP && context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof FenceBlock?.5:0;
        spawn((ServerLevel)context.getLevel(),context.getPlayer(),context.getItemInHand(),pos.getX()+.5,pos.getY()+lift,pos.getZ()+.5);
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(level.isClientSide)return InteractionResultHolder.pass(stack);
        var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
        var pos=hit.getBlockPos();
        if(!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,hit.getDirection(),stack))return InteractionResultHolder.fail(stack);
        if(!(level.getBlockState(pos).getBlock() instanceof LiquidBlock))return InteractionResultHolder.pass(stack);
        return spawn((ServerLevel)level,player,stack,pos.getX(),pos.getY(),pos.getZ())?InteractionResultHolder.consume(stack):InteractionResultHolder.fail(stack);
    }
    private boolean spawn(ServerLevel level,Player player,ItemStack stack,double x,double y,double z) {
        var mob=type.get().create(level);if(mob==null)return false;
        float yaw=Mth.wrapDegrees(level.random.nextFloat()*360);
        mob.moveTo(x,y,z,yaw,0);mob.yHeadRot=yaw;mob.yBodyRot=yaw;
        mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.SPAWN_EGG,null,null);
        if(!level.addFreshEntity(mob))return false;
        mob.playAmbientSound();if(stack.hasCustomHoverName())mob.setCustomName(stack.getHoverName());
        if(player==null || !player.getAbilities().instabuild)stack.shrink(1);
        return true;
    }
}
