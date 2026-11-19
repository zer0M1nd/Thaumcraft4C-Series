package heartwork;

import com.google.gson.*;
import heartwork.compat.blade.*;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.inventory.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("heartwork") @PrefixGameTestTemplate(false)
public class HeartworkBladeTests {
    private static ItemStack blade(GameTestHelper h,String name) {
        return h.getLevel().registryAccess().registryOrThrow(SlashBladeDefinition.REGISTRY_KEY).get(new ResourceLocation("heartwork",name)).getBlade();
    }
    private static CraftingContainer grid() {return new TransientCraftingContainer(new AbstractContainerMenu(null,0) {
        public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
        public boolean stillValid(Player p){return true;}
    },3,3);}
    @GameTest(template="empty") public static void legacyAdvancementsLoadAndMatchNamedBlade(GameTestHelper h) {
        var manager=h.getLevel().getServer().getAdvancements();int count=0;
        for(var a:manager.getAllAdvancements())if(a.getId().getNamespace().equals("heartwork"))count++;
        h.assertTrue(count>=50,"The enabled legacy advancement tree is parsed by the real server");
        var advancement=manager.getAdvancement(new ResourceLocation("heartwork:blades/roukan"));
        h.assertTrue(advancement!=null,"The named blade achievement exists");
        // Forge explicitly suppresses all advancement grants for FakePlayer.
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"HeartworkProgress"));
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND),player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener) {}
        };
        var wrong=blade(h,"bailou");player.getInventory().setItem(0,wrong);
        net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.trigger(player,player.getInventory(),wrong);
        h.assertTrue(!player.getAdvancements().getOrStartProgress(advancement).isDone(),"A different named blade never grants this achievement");
        var right=blade(h,"roukan");player.getInventory().setItem(0,right);
        net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.trigger(player,player.getInventory(),right);
        h.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),"Real inventory trigger awards the matching named blade");
        h.succeed();
    }
    @GameTest(template="empty") public static void allEnabledRecipesLoadWithoutSilentLoss(GameTestHelper h) {
        int enabled=0;
        for(var entry:h.getLevel().getServer().getResourceManager().listResources("recipes",id -> id.getNamespace().equals("heartwork") && id.getPath().endsWith(".json")).entrySet()) {
            try(var reader=entry.getValue().openAsReader()) {
                var json=JsonParser.parseReader(reader).getAsJsonObject();boolean active=true;
                if(json.has("conditions"))for(var element:json.getAsJsonArray("conditions")) {
                    var condition=element.getAsJsonObject();
                    switch(condition.get("type").getAsString()) {
                        case "forge:mod_loaded" -> active &= net.minecraftforge.fml.ModList.get().isLoaded(condition.get("modid").getAsString());
                        case "forge:item_exists" -> active &= ForgeRegistries.ITEMS.containsKey(new ResourceLocation(condition.get("item").getAsString()));
                        default -> throw new IllegalStateException("Unhandled test condition "+condition);
                    }
                }
                if(!active)continue;
                var id=new ResourceLocation("heartwork",entry.getKey().getPath().substring(8,entry.getKey().getPath().length()-5));
                h.assertTrue(h.getLevel().getRecipeManager().byKey(id).isPresent(),"Enabled recipe was actually parsed: "+id);enabled++;
            } catch(java.io.IOException ex) {throw new java.io.UncheckedIOException(ex);}
        }
        h.assertTrue(enabled>=140,"Entire core recipe set is covered");h.succeed();
    }
    @GameTest(template="empty") public static void fusionInheritsOnlyCenterAndRoundTripsNetwork(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(r -> r instanceof HeartworkBladeRecipe && r.getResultItem(h.getLevel().registryAccess()).getOrCreateTag().getString("HeartworkBlade").equals("heartwork:smith_3")).findFirst().orElseThrow();
        var grid=grid();
        for(int i=0;i<9;i++)if(!recipe.getIngredients().get(i).isEmpty())grid.setItem(i,recipe.getIngredients().get(i).getItems()[0].copy());
        grid.setItem(3,blade(h,"bamboolight_yin"));grid.setItem(4,blade(h,"smith_2"));grid.setItem(5,blade(h,"louguan_fake"));
        for(int i:new int[]{3,4,5}) {
            final int slot=i;grid.getItem(i).getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
                state.setKillCount(slot==4?77:1000);state.setProudSoulCount(slot==4?123:9999);state.setRefine(slot==4?9:100);
            });
        }
        h.assertTrue(recipe.matches(grid,h.getLevel()),"Real fusion accepts its three original blades");
        var output=recipe.assemble(grid,h.getLevel().registryAccess());
        var state=output.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
        h.assertTrue(state.getKillCount()==77&&state.getProudSoulCount()==123&&state.getRefine()==9,"Side blades are consumed without adding their growth");
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var serializer=BladeCompat.CRAFTING.get();serializer.toNetwork(buffer,(HeartworkBladeRecipe)recipe);
            var decoded=serializer.fromNetwork(recipe.getId(),buffer);
            h.assertTrue(decoded.matches(grid,h.getLevel()),"Network recipe retains named blade conditions");
            h.assertTrue(decoded.assemble(grid,h.getLevel().registryAccess()).getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new).getKillCount()==77,"Network recipe retains inheritance rule");
        } finally {buffer.release();}
        grid.setItem(3,blade(h,"bamboolight_nice"));h.assertTrue(!recipe.matches(grid,h.getLevel()),"Wrong side blade cannot awaken the fusion");h.succeed();
    }
    @GameTest(template="empty") public static void namedBladesAndInfusionOutputsKeepState(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var registry=h.getLevel().registryAccess().registryOrThrow(SlashBladeDefinition.REGISTRY_KEY);
        h.assertTrue(registry.keySet().stream().filter(id -> id.getNamespace().equals("heartwork")).count()==75,"All 72 named and three base definitions load");
        var result=h.getLevel().getRecipeManager().byKey(new ResourceLocation("heartwork:thaumcraft/infuse_roukan")).orElseThrow().getResultItem(h.getLevel().registryAccess());
        var saved=ItemStack.of(result.save(new net.minecraft.nbt.CompoundTag()));
        var state=saved.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
        h.assertTrue(state.getTranslationKey().equals("item.heartwork.roukan")&&state.getMaxDamage()==100&&state.getBaseAttackModifier()==8,"Infusion result retains real blade identity and original combat values across save/load");
        h.assertTrue(state.getSlashArtsKey().equals(new ResourceLocation("heartwork:saxianshizhan")),"Infusion output retains executable art");h.succeed();
    }
    @GameTest(template="empty") public static void specialArtsConsumeOriginalSoulAndBuffDurations(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var user=h.spawn(EntityType.ZOMBIE,4,3,4);var stack=blade(h,"bailou_tx");user.setItemSlot(EquipmentSlot.MAINHAND,stack);
        var state=stack.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);state.setProudSoulCount(100);
        HeartworkArts.perform("sabailou2",user);
        h.assertTrue(state.getProudSoulCount()==40,"Second Bailou art costs 60 souls");
        var speed=user.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);var blind=user.getEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        h.assertTrue(speed!=null&&speed.getAmplifier()==2&&speed.getDuration()==1200,"Original speed III for 60 seconds");
        h.assertTrue(blind!=null&&blind.getDuration()==100,"Original five-second blindness tradeoff");
        state.setProudSoulCount(50);user.setYRot(180);HeartworkArts.perform("sakuya",user);
        h.assertTrue(state.getProudSoulCount()==50,"Targeted art with no target spends no soul");user.discard();h.succeed();
    }
}
