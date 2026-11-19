package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/** Floating dungeon keys retain their old looping visual age and never expire. */
public  class PermanentItemEntity extends SpecialItemEntity {
    private int visualAge;
    public PermanentItemEntity(EntityType<? extends ItemEntity> type,Level level) { super(type,level); setUnlimitedLifetime(); }
    @Override public void tick() {
        super.tick(); if (++visualAge+5>=lifespan) visualAge=0;
    }
    @Override public int getAge() { return visualAge; }
    @Override public float getSpin(float partial) { return (visualAge+partial)/20f+bobOffs; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("VisualAge",visualAge); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); visualAge=Math.max(0,tag.getInt("VisualAge")); setUnlimitedLifetime();
    }
}
