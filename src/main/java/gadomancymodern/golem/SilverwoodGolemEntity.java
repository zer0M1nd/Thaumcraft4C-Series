package gadomancymodern.golem;
public class SilverwoodGolemEntity extends tc4revival.content.GolemEntity {
    public SilverwoodGolemEntity(net.minecraft.world.entity.EntityType<? extends tc4revival.content.GolemEntity> type,net.minecraft.world.level.Level level){super(type,level);}
    @Override public net.minecraft.network.chat.Component getName(){return hasCustomName()?getCustomName():net.minecraft.network.chat.Component.translatable("item.gadomancy.silverwood_golem");}
}
