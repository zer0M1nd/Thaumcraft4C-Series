package thaumicomniverse.compat.maid;

import java.util.*;
import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.CastingWandItem;

public final class MaidWandTask implements IRangedAttackTask {
    public static final ResourceLocation ID=ResourceLocation.parse("thaumicomniverse:wand_combat");
    @Override public ResourceLocation getUid() { return ID; }
    @Override public ItemStack getIcon() { return new ItemStack(tc4revival.content.TCItems.CASTING_WAND.get()); }
    @Override public SoundEvent getAmbientSound(EntityMaid maid) { return null; }
    @Override public boolean isEnable(EntityMaid maid) { return thaumicomniverse.RevivalFeatures.Group.MAID.enabled(); }
    @Override public boolean enablePanic(EntityMaid maid) { return false; }
    @Override public boolean enableLookAndRandomWalk(EntityMaid maid) { return false; }
    @Override public List<Pair<Integer,BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        // Combat selects the movement distance from the equipped focus, including melee foci.
        return createRideBrainTasks(maid);
    }
    @Override public List<Pair<Integer,BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        // The host appends its schedule/navigation behaviours to this returned list.
        return new ArrayList<>(List.of(Pair.of(3,StartAttacking.<EntityMaid>create(e->e.getMainHandItem().getItem() instanceof CastingWandItem,IRangedAttackTask::findFirstValidAttackTarget)),
                Pair.of(3,StopAttackingIfTargetInvalid.<EntityMaid>create(target->!maid.canAttack(target) || maid.distanceToSqr(target)>24*24)),
                Pair.of(4,new Cast())));
    }
    @Override public void performRangedAttack(EntityMaid maid,LivingEntity target,float distance) { MaidWandCombat.tick(maid,target); }
    private static final class Cast extends Behavior<EntityMaid> {
        Cast() { super(Map.of(MemoryModuleType.ATTACK_TARGET,MemoryStatus.VALUE_PRESENT),1200); }
        @Override protected boolean canStillUse(ServerLevel level,EntityMaid maid,long time) { return maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET); }
        @Override protected void tick(ServerLevel level,EntityMaid maid,long time) {
            maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target->MaidWandCombat.tick(maid,target));
        }
        @Override protected void stop(ServerLevel level,EntityMaid maid,long time) { MaidWandCombat.stop(maid); }
    }
}
