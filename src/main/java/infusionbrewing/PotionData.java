package infusionbrewing;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;

public record PotionData(int legacyId,int duration,int amplifier) {
    public static final String[] NAMES={"speed","slowness","haste","mining_fatigue","strength","instant_health","instant_damage","jump_boost","nausea","regeneration","resistance","fire_resistance","water_breathing","invisibility","blindness","night_vision","hunger","weakness","poison","wither","health_boost","absorption","saturation"};
    public PotionData { if(!BrewingRules.valid(legacyId)||duration<1||amplifier<0||amplifier>3)throw new IllegalArgumentException("Invalid potion data"); }
    public MobEffect effect(){return net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(new net.minecraft.resources.ResourceLocation("minecraft",NAMES[legacyId-1]));}
    public MobEffectInstance instance(){return new MobEffectInstance(effect(),duration,amplifier);}
    public ItemStack potion(boolean splash){return write(new ItemStack(splash?Items.SPLASH_POTION:Items.POTION));}
    public ItemStack write(ItemStack stack){PotionUtils.setCustomEffects(stack,List.of(instance()));return stack;}
    public static PotionData base(int id){return new PotionData(id,BrewingRules.instant(id)?1:2400,0);}
    public static Optional<PotionData> read(ItemStack stack){
        if(!stack.hasTag()||!stack.getTag().contains("CustomPotionEffects",9))return Optional.empty();
        var effects=PotionUtils.getMobEffects(stack);if(effects.size()!=1)return Optional.empty();
        var e=effects.get(0);var key=net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(e.getEffect());
        if(key==null||!key.getNamespace().equals("minecraft"))return Optional.empty();
        for(int i=0;i<NAMES.length;i++)if(NAMES[i].equals(key.getPath())){
            if(e.getDuration()<1||e.getAmplifier()<0||e.getAmplifier()>3||BrewingRules.noAmplifier(i+1)&&e.getAmplifier()!=0)return Optional.empty();
            return Optional.of(new PotionData(i+1,e.getDuration(),e.getAmplifier()));
        }return Optional.empty();
    }
    public boolean maxDuration(){return BrewingRules.instant(legacyId)||duration==19200;}
}
