package thaumicomniverse.alignment;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tc4revival.content.*;


public class AlignmentCoreEntity extends ArcanePedestalBlockEntity implements WandInteractable,MenuProvider {
    public record Plan(AlignmentDefinitions.Recipe recipe,AlignmentDefinitions.Target target,AlignmentMath.Summary actual,
            List<ArcanePedestalBlockEntity> near,List<AlignmentPedestalEntity> outer,int yield,double risk,boolean exact,boolean ingredients) {}
    private final List<ItemStack> escrow=new ArrayList<>();
    private final List<BlockPos> required=new ArrayList<>();
    private ItemStack pending=ItemStack.EMPTY;
    private int remaining,total;
    private double activeRisk;
    private boolean unstable;
    private String activeRecipe="",targetFingerprint="";
    public String diagnostic="";
    public AlignmentCoreEntity(BlockPos pos,BlockState state) {super(AlignmentContent.CORE_ENTITY.get(),pos,state);}
    public boolean running() {return remaining>0;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {return !running()&&super.canPlaceItem(slot,stack);}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face) {return !running()&&super.canPlaceItemThroughFace(slot,stack,face);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face) {return !running()&&super.canTakeItemThroughFace(slot,stack,face);}
    public Plan preview(ServerPlayer player) {
        if(!(level instanceof ServerLevel server)||running()||!thaumicomniverse.RevivalFeatures.Group.ALIGNMENT.enabled())return null;
        var knowledge=tc4revival.research.KnowledgeSavedData.get(server.getServer()).snapshot(player.getUUID()).orElse(null);
        if(knowledge==null)return null;
        var near=new ArrayList<ArcanePedestalBlockEntity>();var outer=new ArrayList<AlignmentPedestalEntity>();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++) {
            int r=Math.max(Math.abs(x),Math.abs(z));if(r!=2&&r!=4)continue;var pos=worldPosition.offset(x,0,z);if(!level.hasChunkAt(pos))return null;
            var entity=level.getBlockEntity(pos);
            if(r==2 && (x%2==0&&z%2==0) && entity instanceof ArcanePedestalBlockEntity pedestal && !(entity instanceof AlignmentPedestalEntity) && !(entity instanceof AlignmentCoreEntity) && !pedestal.isEmpty())near.add(pedestal);
            if(r==4&&entity instanceof AlignmentPedestalEntity pedestal&&!pedestal.isEmpty())outer.add(pedestal);
        }
        var actual=AlignmentMath.summarize(outer.stream().map(p->AlignmentDefinitions.offering(p.getItem(0),p.sign())).toList());
        Plan fallback=null;
        for(var recipe:AlignmentDefinitions.recipes()) {
            if(!recipe.center().test(getItem(0))||!knowledge.researchMarkers().contains(recipe.research()))continue;
            var target=AlignmentDefinitions.target(server,recipe);
            boolean matches=matchNear(recipe.near(),near.stream().map(p->p.getItem(0)).toList())&&outer.stream().allMatch(p->recipe.offerings().test(p.getItem(0)));
            int steps=actual.count()==0?0:AlignmentMath.penaltySteps(actual.count(),actual.kinds(),target.summary().count(),target.summary().kinds());
            double stability=stabilityPairs()*recipe.stabilityPerPair();
            double risk=Math.max(0,recipe.risk()+recipe.quantityRisk()*Math.max(0,(double)actual.count()/target.summary().count()-1)
                +recipe.kindRisk()*Math.max(0,(double)actual.kinds()/target.summary().kinds()-1)-stability);
            var plan=new Plan(recipe,target,actual,List.copyOf(near),List.copyOf(outer),AlignmentMath.yield(recipe.minimum(),recipe.maximum(),steps),risk,
                actual.count()>0&&AlignmentMath.matches(actual,target.summary().vector()),matches);
            if(plan.ingredients()&&plan.exact())return plan;
            if(fallback==null||matches&&!fallback.ingredients())fallback=plan;
        }
        return fallback;
    }
    /** Bipartite matching avoids greedy ingredient/tag ambiguity. */
    static boolean matchNear(List<net.minecraft.world.item.crafting.Ingredient> ingredients,List<ItemStack> stacks) {
        if(ingredients.size()!=stacks.size())return false;return assign(ingredients,stacks,0,new boolean[stacks.size()]);
    }
    private static boolean assign(List<net.minecraft.world.item.crafting.Ingredient> ingredients,List<ItemStack> stacks,int at,boolean[] used) {
        if(at==ingredients.size())return true;
        for(int i=0;i<stacks.size();i++)if(!used[i]&&stacks.get(i).getCount()==1&&ingredients.get(at).test(stacks.get(i))) {used[i]=true;if(assign(ingredients,stacks,at+1,used))return true;used[i]=false;}return false;
    }
    private int stabilityPairs() {
        int pairs=0;for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {
            if(x<0||x==0&&z<=0)continue;
            var a=worldPosition.offset(x,0,z);var b=worldPosition.offset(-x,0,-z);
            if(level.hasChunkAt(a)&&level.hasChunkAt(b)&&stabilizer(a)&&stabilizer(b))pairs++;
        }return pairs;
    }
    private boolean stabilizer(BlockPos pos) {
        var state=level.getBlockState(pos);var entity=level.getBlockEntity(pos);
        return state.is(InfusionLayout.STABILIZERS)||state.getBlock() instanceof thaumcraft.api.crafting.IInfusionStabiliser s&&s.canStabiliseInfusion(level,pos)
            ||entity instanceof thaumcraft.api.crafting.IInfusionStabiliser s&&s.canStabiliseInfusion(level,pos);
    }
    public boolean start(ServerPlayer player,ItemStack wand) {
        if(running()||!AlignmentAccess.canStart(player,wand)||!level.mayInteract(player,worldPosition))return false;
        Plan plan;
        try {plan=preview(player);}catch(IllegalArgumentException|ArithmeticException failure){diagnostic=failure.getMessage();return false;}
        if(plan==null||!plan.exact()||!plan.ingredients())return false;
        if(plan.near().stream().anyMatch(p->!level.mayInteract(player,p.getBlockPos()))||plan.outer().stream().anyMatch(p->!level.mayInteract(player,p.getBlockPos())))return false;
        escrow.add(removeItemNoUpdate(0));
        for(var pedestal:plan.near()){escrow.add(pedestal.removeItemNoUpdate(0));required.add(pedestal.getBlockPos());pedestal.sync();}
        for(var pedestal:plan.outer()){escrow.add(pedestal.removeItemNoUpdate(0));required.add(pedestal.getBlockPos());pedestal.sync();}
        pending=plan.recipe().result().copyWithCount(plan.yield());remaining=total=plan.recipe().duration();activeRisk=plan.risk();activeRecipe=plan.recipe().id();targetFingerprint=plan.target().fingerprint();
        unstable=level.random.nextDouble()<Math.min(.8,activeRisk/(activeRisk+10));
        if(unstable)pending.setCount(Math.max(plan.recipe().minimum(),pending.getCount()-1));
        diagnostic="";sync();return true;
    }
    public void tick() {
        if(!(level instanceof ServerLevel server)||!running()||!thaumicomniverse.RevivalFeatures.Group.ALIGNMENT.enabled())return;
        if(required.stream().anyMatch(p->!level.hasChunkAt(p)))return;
        if(required.stream().anyMatch(p->!(level.getBlockEntity(p) instanceof ArcanePedestalBlockEntity))) {refund();return;}
        if(remaining%10==0)server.sendParticles(unstable?ParticleTypes.WITCH:ParticleTypes.ENCHANT,worldPosition.getX()+.5,worldPosition.getY()+1.2,worldPosition.getZ()+.5,6,.3,.2,.3,.02);
        if(unstable && remaining==total/2) {
            for(var living:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(worldPosition).inflate(3)))living.hurt(level.damageSources().magic(),(float)Math.min(6,1+activeRisk));
            level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GENERIC_EXTINGUISH_FIRE,net.minecraft.sounds.SoundSource.BLOCKS,.4f,1);
        }
        if(--remaining==0) {
            var result=pending;pending=ItemStack.EMPTY;escrow.clear();required.clear();setFromInfusion(result);
            level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,net.minecraft.sounds.SoundSource.BLOCKS,.6f,1.1f);
        }
        setChanged();if(remaining%10==0)sync();
    }
    public void refund() {
        if(level==null||level.isClientSide)return;
        var returned=List.copyOf(escrow);escrow.clear();pending=ItemStack.EMPTY;required.clear();remaining=0;setChanged();
        for(var stack:returned)Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,stack);
        sync();
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context) {
        if(level.isClientSide)return InteractionResult.SUCCESS;
        boolean accepted=context.getPlayer() instanceof ServerPlayer player&&start(player,wand);
        if(!accepted&&context.getPlayer()!=null)context.getPlayer().displayClientMessage(Component.translatable("thaumicomniverse.alignment.rejected"),true);
        return InteractionResult.CONSUME;
    }
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player) {return Optional.empty();}
    @Override public void onWandUseTick(ItemStack wand,Player player,int remaining) {}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remaining) {}
    @Override public Component getDisplayName() {return Component.translatable("thaumicomniverse.alignment.title");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) {return new AlignmentMenu(id,inventory,this);}
    public CompoundTag progress() {var tag=new CompoundTag();tag.putInt("remaining",remaining);tag.putInt("total",total);tag.putString("active",activeRecipe);tag.putDouble("risk",activeRisk);return tag;}
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.merge(progress());tag.putString("fingerprint",targetFingerprint);tag.putBoolean("unstable",unstable);tag.put("pending",pending.save(new CompoundTag()));
        var items=new ListTag();for(var stack:escrow)items.add(stack.save(new CompoundTag()));tag.put("escrow",items);var positions=new ListTag();for(var pos:required)positions.add(LongTag.valueOf(pos.asLong()));tag.put("required",positions);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);remaining=tag.getInt("remaining");total=tag.getInt("total");activeRecipe=tag.getString("active");activeRisk=tag.getDouble("risk");targetFingerprint=tag.getString("fingerprint");unstable=tag.getBoolean("unstable");pending=ItemStack.of(tag.getCompound("pending"));
        escrow.clear();var items=tag.getList("escrow",Tag.TAG_COMPOUND);for(int i=0;i<items.size();i++)escrow.add(ItemStack.of(items.getCompound(i)));
        required.clear();var positions=tag.getList("required",Tag.TAG_LONG);for(var pos:positions)required.add(BlockPos.of(((LongTag)pos).getAsLong()));
    }
    /** Progress synchronization deliberately excludes escrow, pending random outcomes and hidden target fingerprints. */
    @Override public CompoundTag getUpdateTag() {
        var tag=new CompoundTag();net.minecraft.core.NonNullList<ItemStack> display=net.minecraft.core.NonNullList.withSize(1,ItemStack.EMPTY);display.set(0,getItem(0));ContainerHelper.saveAllItems(tag,display);tag.merge(progress());return tag;
    }
}
