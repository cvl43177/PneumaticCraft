package pneumaticCraft.common.inventory;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import pneumaticCraft.common.network.GuiSynced;
import pneumaticCraft.common.network.NetworkHandler;
import pneumaticCraft.common.network.NetworkUtils;
import pneumaticCraft.common.network.PacketUpdateGui;
import pneumaticCraft.common.tileentity.TileEntityBase;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

public class ContainerPneumaticBase<Tile extends TileEntityBase> extends Container{

    public Tile te;
    private final List<SyncedField> syncedFields;

    public ContainerPneumaticBase(Tile te){
        this.te = te;
        syncedFields = NetworkUtils.getSyncedFields(te, GuiSynced.class);
    }

    protected void addSyncedField(SyncedField field){
        syncedFields.add(field);
    }

    public void updateField(int index, Object value){
        syncedFields.get(index).setValue(value);
        te.onGuiUpdate();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player){
        return te.isGuiUseableByPlayer(player);
    }

    @Override
    public void detectAndSendChanges(){
        super.detectAndSendChanges();
        for(int i = 0; i < syncedFields.size(); i++) {
            if(syncedFields.get(i).update()) {
                sendToCrafters(new PacketUpdateGui(i, syncedFields.get(i)));
            }
        }
    }

    protected void sendToCrafters(IMessage message){
        for(ICrafting crafter : (List<ICrafting>)crafters) {
            if(crafter instanceof EntityPlayerMP) {
                NetworkHandler.sendTo(message, (EntityPlayerMP)crafter);
            }
        }
    }
}
