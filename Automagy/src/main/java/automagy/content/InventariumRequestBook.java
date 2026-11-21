package automagy.content;

import automagy.logic.RequestLedger;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.ItemStack;

/** World-independent persistence and accounting; actual carriers must reconcile their claim IDs. */
public final class InventariumRequestBook {
    private final RequestLedger<RequestedItem,GlobalPos> ledger=new RequestLedger<>();
    private final Runnable changed;
    private UUID identity=UUID.randomUUID();
    private final Map<Long,UUID> carriers=new HashMap<>();
    public boolean bind(long claim,UUID carrier){if(ledger.claims().stream().noneMatch(c->c.id()==claim))return false;UUID old=carriers.putIfAbsent(claim,carrier);if(old!=null&&!old.equals(carrier))return false;changed.run();return true;}
    public boolean owned(long claim,UUID carrier){return carrier.equals(carriers.get(claim));}
    public UUID identity(){return identity;}
    public InventariumRequestBook(Runnable changed){this.changed=Objects.requireNonNull(changed);}
    public void request(ItemStack item,GlobalPos destination,long count){if(!item.isEmpty()&&count>0){ledger.request(new RequestedItem(item),destination,count);changed.run();}}
    public long cancel(RequestedItem item,GlobalPos destination,long count){long removed=ledger.cancel(item,destination,count);if(removed>0)changed.run();return removed;}
    public void cancelDestination(GlobalPos destination){for(var request:ledger.requests())if(request.destination().equals(destination))ledger.cancel(request.item(),destination,Long.MAX_VALUE);changed.run();}
    public List<RequestLedger.Request<RequestedItem,GlobalPos>> requests(){return ledger.requests();}
    public List<RequestLedger.Claim<RequestedItem,GlobalPos>> claims(){return ledger.claims();}
    public Optional<RequestLedger.Claim<RequestedItem,GlobalPos>> claim(RequestedItem item,GlobalPos destination,int capacity){var result=ledger.claim(item,destination,capacity);if(result.isPresent())changed.run();return result;}
    public int deliveryLimit(long id){return ledger.deliveryLimit(id);}
    public int finish(long id,int delivered){carriers.remove(id);int result=ledger.finish(id,delivered);changed.run();return result;}
    public void fail(long id){carriers.remove(id);ledger.fail(id);changed.run();}
    /** Only call after all carrier data has been loaded, never from a partial chunk entity scan. */
    public void reconcile(Set<Long> liveClaims){ledger.restore(ledger.requests(),ledger.claims(),liveClaims);carriers.keySet().retainAll(liveClaims);changed.run();}
    private static CompoundTag entry(RequestedItem item,GlobalPos destination,long count){
        var tag=new CompoundTag();tag.put("Item",item.stack().save(new CompoundTag()));tag.putString("Dimension",destination.dimension().location().toString());
        tag.put("Position",NbtUtils.writeBlockPos(destination.pos()));tag.putLong("Amount",count);return tag;
    }
    public CompoundTag save(){
        var tag=new CompoundTag();var requests=new ListTag();var claims=new ListTag();
        for(var request:ledger.requests())requests.add(entry(request.item(),request.destination(),request.amount()));
        for(var claim:ledger.claims()){var saved=entry(claim.item(),claim.destination(),claim.amount());saved.putLong("Claim",claim.id());claims.add(saved);}
        ListTag owners=new ListTag();carriers.forEach((id,uuid)->{var owner=new CompoundTag();owner.putLong("Claim",id);owner.putUUID("Carrier",uuid);owners.add(owner);});tag.put("Carriers",owners);
        tag.put("Requests",requests);tag.put("Claims",claims);tag.putUUID("Identity",identity);return tag;
    }
    public void load(CompoundTag tag){
        identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):UUID.randomUUID();
        List<RequestLedger.Request<RequestedItem,GlobalPos>> requests=new ArrayList<>();
        List<RequestLedger.Claim<RequestedItem,GlobalPos>> claims=new ArrayList<>();Set<Long> ids=new HashSet<>();
        for(String key:List.of("Requests","Claims"))for(Tag raw:tag.getList(key,Tag.TAG_COMPOUND)){
            CompoundTag saved=(CompoundTag)raw;ItemStack stack=ItemStack.of(saved.getCompound("Item"));
            var dimension=ResourceLocation.tryParse(saved.getString("Dimension"));long amount=saved.getLong("Amount");
            if(stack.isEmpty()||dimension==null||amount<=0||!saved.contains("Position",Tag.TAG_COMPOUND))continue;
            var item=new RequestedItem(stack);var destination=GlobalPos.of(ResourceKey.create(Registries.DIMENSION,dimension),NbtUtils.readBlockPos(saved.getCompound("Position")));
            if(key.equals("Requests"))requests.add(new RequestLedger.Request<>(item,destination,amount));
            else {long id=saved.getLong("Claim");if(id>0&&amount<=Integer.MAX_VALUE&&ids.add(id))claims.add(new RequestLedger.Claim<>(id,item,destination,(int)amount));}
        }
        // Preserve reservations until the carrier system can distinguish unloaded and dead carriers.
        ledger.restore(requests,claims,ids);
        carriers.clear();for(Tag raw:tag.getList("Carriers",Tag.TAG_COMPOUND)){var owner=(CompoundTag)raw;long id=owner.getLong("Claim");if(ids.contains(id)&&owner.hasUUID("Carrier"))carriers.putIfAbsent(id,owner.getUUID("Carrier"));}
    }
}
