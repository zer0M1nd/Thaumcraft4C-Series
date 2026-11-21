package arcanearteries;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import wayoftime.bloodmagic.common.tile.TileAltar;

/** Isolated validation payload, absent from every published archive. */
@Mod.EventBusSubscriber(modid=ArcaneArteries.ID)
public class ArteriesAudit {
    public static final BlockPos ALTAR=new BlockPos(0,64,0);
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        if(!Boolean.getBoolean("arcanearteries.validation"))return;
        event.getDispatcher().register(Commands.literal("arteriesaudit").requires(s->s.hasPermission(2))
            .then(Commands.literal("hard").then(Commands.argument("value",com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c->{
                ArteriesConfig.HARD_CRAFTING.set(com.mojang.brigadier.arguments.BoolArgumentType.getBool(c,"value"));
                c.getSource().sendSuccess(()->Component.literal("Difficulty set; run reload to reselect recipes and synchronize clients"),false);return 1;})))
            .then(Commands.literal("setup").executes(c->{
                var server=c.getSource().getServer();var level=server.overworld();var p=server.getPlayerList().getPlayerByName("ArteriesAudit");
                if(p==null)throw new IllegalStateException("The isolated network player must join first");
                for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=62;y<=69;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),(y==62?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState());
                level.setBlockAndUpdate(ALTAR,ForgeRegistries.BLOCKS.getValue(new ResourceLocation("bloodmagic:altar")).defaultBlockState());
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)level.setBlockAndUpdate(ALTAR.offset(x,-1,z),(x==0?ArcaneArteries.MANA_RUNE:ArcaneArteries.THAUMIC_RUNE).get().defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(3,63,0),ArcaneArteries.THAUMIC_RUNE.get().defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(4,63,0),ArcaneArteries.MANA_RUNE.get().defaultBlockState());
                p.teleportTo(level,0.5,65,2.5,180,0);p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.setHealth(20);p.getFoodData().setFoodLevel(20);
                p.getInventory().clearContent();int slot=0;for(var ref:ArcaneArteries.CONTENT.values())p.getInventory().setItem(slot++,new ItemStack(ref.get()));
                var tablet=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("botania:mana_tablet")));tablet.getOrCreateTag().putInt("mana",5000);p.getInventory().setItem(15,tablet);
                p.getInventory().setItem(8,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumcraft:thaumonomicon"))));
                p.getInventory().selected=0;p.inventoryMenu.broadcastChanges();
                c.getSource().sendSuccess(()->Component.literal("Network interaction scene ready; altar=0 64 0"),false);return 1;
            }))
            .then(Commands.literal("custom").executes(c->{ArteriesConfig.CUSTOM_SELF.set(true);ArteriesConfig.CUSTOM_MOB.set(true);ArteriesConfig.SELF_NAME.set("Server Razor");ArteriesConfig.MANA_COST.set(137);
                c.getSource().sendSuccess(()->Component.literal("Server custom name/mana changed; reload to synchronize"),false);return 1;}))
            .then(Commands.literal("cow").executes(c->{var level=c.getSource().getServer().overworld();var cow=EntityType.COW.create(level);cow.setNoAi(true);cow.moveTo(0.5,65,0.5);level.addFreshEntity(cow);return 1;}))
            .then(Commands.literal("state").executes(c->{var p=c.getSource().getServer().getPlayerList().getPlayerByName("ArteriesAudit");var a=(TileAltar)c.getSource().getServer().overworld().getBlockEntity(ALTAR);
                String value="hp="+(p==null?"none":p.getHealth())+";altar="+(a==null?"none":a.serialize(new net.minecraft.nbt.CompoundTag()))+";hard="+ArteriesConfig.HARD_CRAFTING.get();
                c.getSource().sendSuccess(()->Component.literal(value),false);return 1;})));
    }
}
