package gadomancymodern.golem;
import gadomancymodern.Gadomancy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;

public class GadGolems {
    public static final ResourceLocation SILVERWOOD=ResourceLocation.parse("gadomancy:silverwood"),BREAK=ResourceLocation.parse("gadomancy:break"),BODYGUARD=ResourceLocation.parse("gadomancy:bodyguard"),DATA=ResourceLocation.parse("gadomancy:golem");
    public static boolean silverwood(GolemEntity golem){return SILVERWOOD.equals(golem.configuration().customMaterial());}
    public static boolean breaking(GolemEntity golem){return BREAK.equals(golem.configuration().customTask());}
    public static boolean bodyguard(GolemEntity golem){return BODYGUARD.equals(golem.configuration().customTask());}
    public static CompoundTag data(GolemEntity golem){return golem.configuration().addonData(DATA);}
    public static void data(GolemEntity golem,CompoundTag data){thaumcraft.api.golems.GolemApi.data(golem,DATA,data);}
    public static boolean sitting(GolemEntity golem){return data(golem).getBoolean("Sitting");}
    public static net.minecraft.world.entity.player.Player owner(GolemEntity golem){return golem.level().players().stream().filter(player->player.getGameProfile().getName().equals(golem.ownerName())).findFirst().orElse(null);}
    public static void register(){
        thaumcraft.api.golems.GolemApi.registerMaterial(SILVERWOOD,new thaumcraft.api.golems.GolemApi.Material(20,9,.38f,false,3,8,75,1,false,ResourceLocation.parse("gadomancy:textures/models/golem_silverwood.png")));
        thaumcraft.api.golems.GolemApi.registerTask(BREAK,(golem,work,targets)->{work.addGoal(0,new BreakGoal(golem));work.addGoal(5,new GolemOpenDoorGoal(golem));work.addGoal(6,new GolemReturnHomeGoal(golem));work.addGoal(7,new LookAtPlayerGoal(golem,net.minecraft.world.entity.player.Player.class,6));work.addGoal(8,new RandomLookAroundGoal(golem));});
        thaumcraft.api.golems.GolemApi.registerTask(BODYGUARD,(golem,work,targets)->{golem.markers().replace(java.util.List.of());golem.clearRestriction();targets.addGoal(1,new BodyguardGoals.OwnerTarget(golem,true));targets.addGoal(2,new BodyguardGoals.OwnerTarget(golem,false));targets.addGoal(3,new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(golem).setAlertOthers());work.addGoal(1,new BodyguardGoals.Sit(golem));if(golem.configuration().decorated('R'))work.addGoal(2,new BodyguardGoals.Dart(golem));work.addGoal(3,new BodyguardGoals.Melee(golem));work.addGoal(4,new BodyguardGoals.Follow(golem));work.addGoal(5,new GolemOpenDoorGoal(golem));work.addGoal(7,new LookAtPlayerGoal(golem,net.minecraft.world.entity.player.Player.class,6));work.addGoal(8,new RandomLookAroundGoal(golem));});
    }
    public static boolean install(GolemEntity golem,ResourceLocation task){if(golem.configuration().hasTask())return false;golem.configuration().installCore(task.equals(BREAK)?GolemCoreType.EMPTY:GolemCoreType.GUARD);thaumcraft.api.golems.GolemApi.configure(golem,golem.configuration().customMaterial(),task,golem.configuration().blockAppearance());golem.level().broadcastEntityEvent(golem,(byte)7);return true;}
    public static ItemStack portable(GolemEntity golem,boolean dismantle){
        var config=golem.configuration();var result=new ItemStack(silverwood(golem)?Gadomancy.SILVERWOOD_GOLEM.get():TCItems.golem(config.material()));var tag=result.getOrCreateTag();config.saveExtension(tag);
        if(config.advanced())tag.putByte("advanced",(byte)1);
        tag.getCompound("AddonData").getCompound(DATA.toString()).remove("Sitting");
        if(dismantle){tag.remove("AddonTask");var data=tag.getCompound("AddonData").getCompound(DATA.toString());data.remove("Sitting");}
        else{if(golem.hasCustomName())result.setHoverName(golem.getCustomName());if(!config.decorations().isEmpty())tag.putString("deco",config.decorations());if(config.core()!=null)tag.putByte("core",(byte)config.core().ordinal());tag.putByteArray("upgrades",config.upgrades());golem.markers().inDimension(golem.level().dimension().location());tag.put("markers",golem.markers().save());var samples=new CompoundTag();golem.filters().save(samples);tag.put("Inventory",samples.getList("Inventory",net.minecraft.nbt.Tag.TAG_COMPOUND));}
        return result;
    }
}
