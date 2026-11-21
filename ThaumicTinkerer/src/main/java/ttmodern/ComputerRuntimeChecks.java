package ttmodern;

import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Executes through the actual Lua runtime, not direct calls to peripheral implementation methods. */
final class ComputerRuntimeChecks {
    static void run(GameTestHelper test,boolean turtle) throws Exception {
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));var target=pos.north();
        var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("computercraft:"+(turtle?"turtle_normal":"computer_normal")));
        world.setBlockAndUpdate(pos,block.defaultBlockState());
        world.setBlockAndUpdate(target,turtle?Tinkerer.LIGHT_GAS.get().defaultBlockState():tc4revival.content.TCArcane.ARCANE_EAR.get().defaultBlockState());
        var entity=world.getBlockEntity(pos);ITurtleAccess access=turtle?(ITurtleAccess)entity.getClass().getMethod("getAccess").invoke(entity):null;
        if(turtle){access.getInventory().setItem(0,new ItemStack(Tinkerer.GAS_REMOVER.get()));access.setSelectedSlot(0);access.setFuelLevel(123);}
        Object computer=entity.getClass().getMethod("createServerComputer").invoke(entity);
        var mount=(WritableMount)computer.getClass().getMethod("createRootMount").invoke(computer);
        String body=turtle?"assert(turtle.equipLeft()); assert(turtle.dig())":"local p; for _,s in ipairs(peripheral.getNames()) do local m=peripheral.wrap(s); if m.setNote then p=m; break end end; assert(p,'ear peripheral not attached'); p.setNote(19)";
        String program="local ok,err=pcall(function() "+body+" end); local f=assert(fs.open('tt-result','w')); f.write(ok and 'OK' or tostring(err)); f.close()";
        try(var channel=mount.openForWrite("startup.lua")){channel.write(java.nio.ByteBuffer.wrap(program.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        computer.getClass().getMethod("turnOn").invoke(computer);
        test.runAfterDelay(180,()->{try{
            test.assertTrue(mount.exists("tt-result"),"Actual Lua startup reaches completion");
            String result;try(var channel=mount.openForRead("tt-result")){var buffer=java.nio.ByteBuffer.allocate(4096);channel.read(buffer);buffer.flip();result=java.nio.charset.StandardCharsets.UTF_8.decode(buffer).toString();}
            test.assertTrue(result.equals("OK"),"Lua API succeeds: "+result);
            if(turtle){test.assertTrue(world.isEmptyBlock(target)&&access.getFuelLevel()==123,"Equipped dissipator clears gas without spending movement fuel");test.assertTrue(access.getUpgrade(dan200.computercraft.api.turtle.TurtleSide.LEFT) instanceof FumeTurtleUpgrade,"JSON upgrade is available through actual equipLeft");}
            else test.assertTrue(((tc4revival.content.ArcaneEarBlockEntity)world.getBlockEntity(target)).note()==19,"Lua main-thread call changes the actual ear");test.succeed();
        }catch(java.io.IOException error){throw new RuntimeException(error);}finally{world.removeBlock(pos,false);world.removeBlock(target,false);}});
    }
    private ComputerRuntimeChecks(){}
}
