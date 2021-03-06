package pneumaticCraft.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import pneumaticCraft.api.IHeatExchangerLogic;
import pneumaticCraft.api.PneumaticRegistry.IPneumaticCraftInterface;
import pneumaticCraft.api.client.pneumaticHelmet.IBlockTrackEntry;
import pneumaticCraft.api.client.pneumaticHelmet.IEntityTrackEntry;
import pneumaticCraft.api.client.pneumaticHelmet.IHackableBlock;
import pneumaticCraft.api.client.pneumaticHelmet.IHackableEntity;
import pneumaticCraft.api.drone.ICustomBlockInteract;
import pneumaticCraft.api.drone.IPathfindHandler;
import pneumaticCraft.api.item.IInventoryItem;
import pneumaticCraft.api.tileentity.IHeatExchanger;
import pneumaticCraft.client.render.pneumaticArmor.blockTracker.BlockTrackEntryList;
import pneumaticCraft.client.render.pneumaticArmor.hacking.HackableHandler.HackingEntityProperties;
import pneumaticCraft.common.entity.living.EntityDrone;
import pneumaticCraft.common.heat.HeatExchangerLogic;
import pneumaticCraft.common.heat.HeatExchangerLogicConstant;
import pneumaticCraft.common.heat.HeatExchangerManager;
import pneumaticCraft.common.heat.SimpleHeatExchanger;
import pneumaticCraft.common.item.ItemMachineUpgrade;
import pneumaticCraft.common.item.Itemss;
import pneumaticCraft.common.progwidgets.IProgWidget;
import pneumaticCraft.common.progwidgets.ProgWidgetArea;
import pneumaticCraft.common.progwidgets.ProgWidgetCustomBlockInteract;
import pneumaticCraft.common.progwidgets.ProgWidgetDropItem;
import pneumaticCraft.common.progwidgets.ProgWidgetGoToLocation;
import pneumaticCraft.common.progwidgets.ProgWidgetStart;
import pneumaticCraft.common.progwidgets.ProgWidgetSuicide;
import pneumaticCraft.common.tileentity.TileEntityProgrammer;
import pneumaticCraft.common.util.PneumaticCraftUtils;
import pneumaticCraft.lib.Log;

/**
 * With this class you can register your entities to give more info in the tooltip of the Entity Tracker.
 */
public class PneumaticCraftAPIHandler implements IPneumaticCraftInterface{
    private final static PneumaticCraftAPIHandler INSTANCE = new PneumaticCraftAPIHandler();
    public final List<Class<? extends IEntityTrackEntry>> entityTrackEntries = new ArrayList<Class<? extends IEntityTrackEntry>>();
    public final Map<Class<? extends Entity>, Class<? extends IHackableEntity>> hackableEntities = new HashMap<Class<? extends Entity>, Class<? extends IHackableEntity>>();
    public final Map<Block, Class<? extends IHackableBlock>> hackableBlocks = new HashMap<Block, Class<? extends IHackableBlock>>();
    public final Map<String, Class<? extends IHackableEntity>> stringToEntityHackables = new HashMap<String, Class<? extends IHackableEntity>>();
    public final Map<String, Class<? extends IHackableBlock>> stringToBlockHackables = new HashMap<String, Class<? extends IHackableBlock>>();
    public final Map<Block, IPathfindHandler> pathfindableBlocks = new HashMap<Block, IPathfindHandler>();
    public final List<IInventoryItem> inventoryItems = new ArrayList<IInventoryItem>();
    public final List<Integer> concealableRenderIds = new ArrayList<Integer>();
    public final Map<Fluid, Integer> liquidXPs = new HashMap<Fluid, Integer>();
    public final Map<Fluid, Integer> liquidFuels = new HashMap<Fluid, Integer>();

    private PneumaticCraftAPIHandler(){
        concealableRenderIds.add(0);
        concealableRenderIds.add(31);
        concealableRenderIds.add(39);
        concealableRenderIds.add(10);
        concealableRenderIds.add(16);
        concealableRenderIds.add(26);
    }

    public static PneumaticCraftAPIHandler getInstance(){
        return INSTANCE;
    }

    @Override
    public void registerEntityTrackEntry(Class<? extends IEntityTrackEntry> entry){
        if(entry == null) throw new IllegalArgumentException("Can't register null!");
        entityTrackEntries.add(entry);
    }

    @Override
    public void addHackable(Class<? extends Entity> entityClazz, Class<? extends IHackableEntity> iHackable){
        if(entityClazz == null) throw new NullPointerException("Entity class is null!");
        if(iHackable == null) throw new NullPointerException("IHackableEntity is null!");
        if(Entity.class.isAssignableFrom(iHackable)) {
            Log.warning("Entities that implement IHackableEntity shouldn't be registered as hackable! Registering entity: " + entityClazz.getCanonicalName());
        } else {
            try {
                IHackableEntity hackableEntity = iHackable.newInstance();
                if(hackableEntity.getId() != null) stringToEntityHackables.put(hackableEntity.getId(), iHackable);
                hackableEntities.put(entityClazz, iHackable);
            } catch(InstantiationException e) {
                Log.error("Not able to register hackable entity: " + iHackable.getName() + ". Does the class have a parameterless constructor?");
                e.printStackTrace();
            } catch(IllegalAccessException e) {
                Log.error("Not able to register hackable entity: " + iHackable.getName() + ". Is the class a public class?");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addHackable(Block block, Class<? extends IHackableBlock> iHackable){
        if(block == null) throw new NullPointerException("Block is null!");
        if(iHackable == null) throw new NullPointerException("IHackableBlock is null!");

        if(Block.class.isAssignableFrom(iHackable)) {
            Log.warning("Blocks that implement IHackableBlock shouldn't be registered as hackable! Registering block: " + block.getLocalizedName());
        } else {
            try {
                IHackableBlock hackableBlock = iHackable.newInstance();
                if(hackableBlock.getId() != null) stringToBlockHackables.put(hackableBlock.getId(), iHackable);
                hackableBlocks.put(block, iHackable);
            } catch(InstantiationException e) {
                Log.error("Not able to register hackable block: " + iHackable.getName() + ". Does the class have a parameterless constructor?");
                e.printStackTrace();
            } catch(IllegalAccessException e) {
                Log.error("Not able to register hackable block: " + iHackable.getName() + ". Is the class a public class?");
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<IHackableEntity> getCurrentEntityHacks(Entity entity){
        HackingEntityProperties hackingProps = (HackingEntityProperties)entity.getExtendedProperties("PneumaticCraftHacking");
        if(hackingProps != null) {
            List<IHackableEntity> hackables = hackingProps.getCurrentHacks();
            if(hackables != null) return hackables;
        } else {
            Log.warning("Extended entity props HackingEntityProperties couldn't be found in the entity " + entity.getCommandSenderName());
        }
        return new ArrayList<IHackableEntity>();
    }

    @Override
    public void registerBlockTrackEntry(IBlockTrackEntry entry){
        if(entry == null) throw new IllegalArgumentException("Block Track Entry can't be null!");
        BlockTrackEntryList.instance.trackList.add(entry);
    }

    @Override
    public void addPathfindableBlock(Block block, IPathfindHandler handler){
        if(block == null) throw new IllegalArgumentException("Block can't be null!");
        pathfindableBlocks.put(block, handler);
    }

    @Override
    public int getProtectingSecurityStations(World world, int x, int y, int z, EntityPlayer player, boolean showRangeLines){
        if(world.isRemote) throw new IllegalArgumentException("This method can only be called from the server side!");
        return PneumaticCraftUtils.getProtectingSecurityStations(world, x, y, z, player, showRangeLines, false);
    }

    @Override
    public void registerInventoryItem(IInventoryItem handler){
        inventoryItems.add(handler);
    }

    @Override
    public void registerConcealableRenderId(int id){
        concealableRenderIds.add(id);
    }

    @Override
    public void registerXPLiquid(Fluid fluid, int liquidToPointRatio){
        if(fluid == null) throw new NullPointerException("Fluid can't be null!");
        if(liquidToPointRatio <= 0) throw new IllegalArgumentException("liquidToPointRatio can't be <= 0");
        liquidXPs.put(fluid, liquidToPointRatio);
    }

    @Override
    public IHeatExchangerLogic getHeatExchangerLogic(){
        return new HeatExchangerLogic();
    }

    public void registerBlockExchanger(Block block, IHeatExchanger heatExchanger){
        HeatExchangerManager.getInstance().registerBlockExchanger(block, heatExchanger);
    }

    public void registerBlockExchanger(Block block, IHeatExchangerLogic heatExchangerLogic){
        registerBlockExchanger(block, new SimpleHeatExchanger(heatExchangerLogic));
    }

    @Override
    public void registerBlockExchanger(Block block, double temperature, double thermalResistance){
        registerBlockExchanger(block, new HeatExchangerLogicConstant(temperature, thermalResistance));
    }

    @Override
    public void registerFuel(Fluid fluid, int mLPerBucket){
        if(fluid == null) throw new NullPointerException("Fluid can't be null!");
        if(mLPerBucket < 0) throw new IllegalArgumentException("mLPerBucket can't be < 0");
        if(liquidFuels.containsKey(fluid)) {
            Log.info("Overriding liquid fuel entry " + fluid.getLocalizedName(new FluidStack(fluid, 1)) + " (" + fluid.getName() + ") with a fuel value of " + mLPerBucket + " (previously " + liquidFuels.get(fluid) + ")");
            if(mLPerBucket == 0) liquidFuels.remove(fluid);
        }
        if(mLPerBucket > 0) liquidFuels.put(fluid, mLPerBucket);
    }

    @Override
    public void registerCustomBlockInteractor(ICustomBlockInteract interactor){
        TileEntityProgrammer.registeredWidgets.add(new ProgWidgetCustomBlockInteract().setInteractor(interactor));
    }

    @Override
    public EntityCreature deliverItemsAmazonStyle(World world, int x, int y, int z, ItemStack... deliveredStacks){
        if(world.isRemote) return null;
        if(deliveredStacks.length == 0) throw new IllegalArgumentException("You need to deliver at least 1 stack!");
        if(deliveredStacks.length > 65) throw new IllegalArgumentException("You can only deliver up to 65 stacks at once!");
        for(ItemStack stack : deliveredStacks) {
            if(stack == null) throw new IllegalArgumentException("You can't supply a null stack to be delivered!");
            if(stack.getItem() == null) throw new IllegalArgumentException("You can't supply a stack with a null item to be delivered!");
        }

        EntityDrone drone = new EntityDrone(world);

        NBTTagCompound tag = new NBTTagCompound();
        drone.writeEntityToNBT(tag);

        int requiredDispenserUpgrades = deliveredStacks.length - 1;
        NBTTagList upgradeList = new NBTTagList();
        NBTTagCompound slotEntry = new NBTTagCompound();
        slotEntry.setByte("Slot", (byte)0);
        new ItemStack(Itemss.machineUpgrade, requiredDispenserUpgrades, ItemMachineUpgrade.UPGRADE_DISPENSER_DAMAGE).writeToNBT(slotEntry);
        upgradeList.appendTag(slotEntry);

        slotEntry = new NBTTagCompound();
        slotEntry.setByte("Slot", (byte)1);
        new ItemStack(Itemss.machineUpgrade, 10, ItemMachineUpgrade.UPGRADE_SPEED_DAMAGE).writeToNBT(slotEntry);
        upgradeList.appendTag(slotEntry);

        NBTTagCompound inv = new NBTTagCompound();

        inv.setTag("Items", upgradeList);
        tag.setTag("Inventory", inv);
        tag.setFloat("currentAir", 100000);

        drone.readEntityFromNBT(tag);
        drone.setCustomNameTag(StatCollector.translateToLocal("drone.amazonDeliveryDrone"));

        //Program the drone
        int startY = world.getHeightValue(x + 30, z) + 30;
        drone.setPosition(x + 30, startY, z);
        List<IProgWidget> widgets = drone.progWidgets;

        drone.naturallySpawned = true;//Don't let the drone be dropped when wrenching it.

        ProgWidgetStart start = new ProgWidgetStart();
        start.setX(92);
        start.setY(41);
        widgets.add(start);

        ProgWidgetDropItem drop = new ProgWidgetDropItem();
        drop.setX(92);
        drop.setY(52);
        widgets.add(drop);

        ProgWidgetGoToLocation gotoPiece = new ProgWidgetGoToLocation();
        gotoPiece.setX(92);
        gotoPiece.setY(74);
        widgets.add(gotoPiece);

        ProgWidgetSuicide suicide = new ProgWidgetSuicide();
        suicide.setX(92);
        suicide.setY(85);
        widgets.add(suicide);

        ProgWidgetArea area = new ProgWidgetArea();
        area.setX(107);
        area.setY(52);
        area.x1 = x;
        area.z1 = z;
        if(drone.isBlockValidPathfindBlock(x, y, z)) {
            for(int i = 0; i < 5 && drone.isBlockValidPathfindBlock(area.x1, i + y + 1, area.z1); i++) {
                area.y1 = y + i;
            }
        } else {
            area.y1 = world.getHeightValue(x, z) + 10;
            if(!drone.isBlockValidPathfindBlock(area.x1, area.y1, area.z1)) area.y1 = 260;//Worst case scenario, there are definately no blocks here.
        }
        widgets.add(area);

        area = new ProgWidgetArea();
        area.setX(107);
        area.setY(74);
        area.x1 = x + 30;
        area.y1 = startY;
        area.z1 = z;
        widgets.add(area);

        TileEntityProgrammer.updatePuzzleConnections(widgets);

        for(int i = 0; i < deliveredStacks.length; i++) {
            drone.getInventory().setInventorySlotContents(i, deliveredStacks[i].copy());
        }
        world.spawnEntityInWorld(drone);
        return drone;
    }
}
