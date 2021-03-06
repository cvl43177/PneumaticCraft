package pneumaticCraft.common;

import java.util.Iterator;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import pneumaticCraft.PneumaticCraft;
import pneumaticCraft.api.block.IPneumaticWrenchable;
import pneumaticCraft.api.item.IPressurizable;
import pneumaticCraft.client.render.pneumaticArmor.EntityTrackUpgradeHandler;
import pneumaticCraft.client.render.pneumaticArmor.HUDHandler;
import pneumaticCraft.client.render.pneumaticArmor.hacking.HackableHandler;
import pneumaticCraft.client.render.pneumaticArmor.hacking.entity.HackableEnderman;
import pneumaticCraft.common.block.Blockss;
import pneumaticCraft.common.block.pneumaticPlants.BlockPlants;
import pneumaticCraft.common.block.pneumaticPlants.BlockPneumaticPlantBase;
import pneumaticCraft.common.item.ItemMachineUpgrade;
import pneumaticCraft.common.item.ItemPlasticPlants;
import pneumaticCraft.common.item.ItemPneumaticArmor;
import pneumaticCraft.common.item.Itemss;
import pneumaticCraft.common.network.NetworkHandler;
import pneumaticCraft.common.network.PacketPlaySound;
import pneumaticCraft.common.network.PacketSetMobTarget;
import pneumaticCraft.common.remote.GlobalVariableManager;
import pneumaticCraft.common.thirdparty.ModInteractionUtilImplementation;
import pneumaticCraft.common.util.PneumaticCraftUtils;
import pneumaticCraft.lib.TileEntityConstants;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EventHandlerPneumaticCraft{

    // piece of code to convert Q-ed seeds immediately into EntityItems that can
    // plant themselves.
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event){
        if(!event.entity.worldObj.isRemote && !event.entity.isDead) {
            if(event.entity instanceof EntityPotion) {
                PneumaticCraft.tickHandler.potionEntities.add((EntityPotion)event.entity);
            }
        }
    }

    private static ItemStack IRON_INGOT = new ItemStack(Items.iron_ingot);
    private static ItemStack IRON_BLOCK = new ItemStack(Blocks.iron_block);

    @SubscribeEvent
    public void handleIronExplosions(ExplosionEvent.Detonate event){
        Iterator<Entity> iterator = event.getAffectedEntities().iterator();
        while(iterator.hasNext()) {
            Entity entity = iterator.next();
            if(entity instanceof EntityItem) {
                ItemStack stack = ((EntityItem)entity).getEntityItem();
                if(stack != null && !entity.isDead && PneumaticCraftUtils.isSameOreDictStack(stack, IRON_INGOT) || PneumaticCraftUtils.isSameOreDictStack(stack, IRON_BLOCK)) {
                    Random rand = new Random();
                    if(stack.stackSize >= 3 || rand.nextDouble() >= Config.configCompressedIngotLossRate / 100D) {
                        Item newItem = PneumaticCraftUtils.isSameOreDictStack(stack, IRON_INGOT) ? Itemss.ingotIronCompressed : Item.getItemFromBlock(Blockss.compressedIron);
                        ItemStack newStack = new ItemStack(newItem, stack.stackSize, stack.getItemDamage());
                        if(stack.stackSize >= 3) {
                            newStack.stackSize = (int)(stack.stackSize * (rand.nextDouble() * Math.min(Config.configCompressedIngotLossRate * 0.02D, 0.2D) + (Math.max(0.9D, 1D - Config.configCompressedIngotLossRate * 0.01D) - Config.configCompressedIngotLossRate * 0.01D)));
                        }
                        ((EntityItem)entity).setEntityItemStack(newStack);
                        iterator.remove();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityConstruction(EntityConstructing event){
        HackableHandler.onEntityConstruction(event.entity);
        ItemPlasticPlants.onEntityConstruction(event.entity);
    }

    @SubscribeEvent
    public void onPlayerPickup(EntityItemPickupEvent event){
        if(event.item != null && event.item.getEntityItem() != null && event.item.getEntityItem().getItem() == Itemss.plasticPlant && event.item.getEntityItem().getItemDamage() > 15) {
            event.item.getEntityItem().setItemDamage(event.item.getEntityItem().getItemDamage() - 16);
        }
        /* if(event.item != null && event.item.getEntityItem() != null && event.item.getEntityItem().getItem() == Items.skull) {
             NBTTagCompound tag = new NBTTagCompound();
             tag.setString("SkullOwner", "MineMaarten");
             event.item.getEntityItem().setTagCompound(tag);
         }*/
    }

    // add slime seeds as mobdrop to Slimes.
    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event){
        if(!event.entity.worldObj.isRemote) {
            if(Config.enableSlimeSeedDrop && event.entity instanceof EntitySlime && Math.random() < 0.1D) {
                ItemPlasticPlants.markInactive(event.entity.entityDropItem(new ItemStack(Itemss.plasticPlant, 1, ItemPlasticPlants.SLIME_PLANT_DAMAGE), 0));
            } else if(Config.enableCreeperSeedDrop && event.entity instanceof EntityCreeper && Math.random() < 0.05D) {
                if(Config.enableCreeperDropExplosion) event.entity.worldObj.createExplosion(event.entity, event.entity.posX, event.entity.posY + event.entityLiving.height / 2D, event.entity.posZ, 0.5F, event.entity.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing"));
                int dropAmount = (int)(Math.random() * 3D) + 1;
                for(int i = 0; i < dropAmount; i++)
                    ItemPlasticPlants.markInactive(event.entity.entityDropItem(new ItemStack(Itemss.plasticPlant, 1, ItemPlasticPlants.CREEPER_PLANT_DAMAGE), 0));
            } else if(Config.enableSquidSeedDrop && event.entity instanceof EntitySquid && Math.random() < 0.05D) {
                ItemPlasticPlants.markInactive(event.entity.entityDropItem(new ItemStack(Itemss.plasticPlant, 1, ItemPlasticPlants.SQUID_PLANT_DAMAGE), 0));
            }
        }
    }

    @SubscribeEvent
    public void onEnderTeleport(EnderTeleportEvent event){
        if(!HackableEnderman.onEndermanTeleport(event.entity)) {
            event.setCanceled(true);
        } else {
            if(Config.enableEndermanSeedDrop && Math.random() < 0.05D) {
                if(!event.entity.worldObj.isRemote) ItemPlasticPlants.markInactive(event.entity.entityDropItem(new ItemStack(Itemss.plasticPlant, 1, ItemPlasticPlants.ENDER_PLANT_DAMAGE), 0));
            }
        }
    }

    // bone meal event, to grow plants
    @SubscribeEvent
    public void onFertilization(BonemealEvent event){
        if(event.world.isRemote) return; // why would we want to handle this on the client-side?

        if(event.block == Blocks.netherrack || event.block == Blocks.end_stone || event.block.canSustainPlant(event.world, event.x, event.y, event.z, ForgeDirection.UP, Blocks.red_flower)) { // can bonemeal Biomes O' Plenty grass, etc.    			    			
            boolean onGrass = event.block instanceof BlockGrass;
            if(onGrass && Config.includePlantsOnBonemeal || !onGrass && Config.allowDirtBonemealing) {
                // we'll try to spawn plants in a 5x5 area which is centered on the block that has been bonemealed
                for(int x = event.x - 2; x < event.x + 3; x++) {
                    for(int z = event.z - 2; z < event.z + 3; z++) {
                        if(event.world.isAirBlock(x, event.y + 1, z)) {
                            if(event.world.rand.nextInt(8) == 1) { // increase .nextInt(x) to lower the chances of spawning a plant
                                BlockPneumaticPlantBase trySpawn = BlockPlants.allPlants.get(event.world.rand.nextInt(BlockPlants.allPlants.size() - 1)); // select a random plant    							
                                if(trySpawn.canPlantGrowOnThisBlock(event.world.getBlock(x, event.y, z), event.world, x, event.y, z)) { // make sure that the plant we selected can grow on the soil
                                    event.world.setBlock(x, event.y + (trySpawn.isPlantHanging() ? -1 : 1), z, trySpawn);
                                }
                            }
                        }
                    }

                    /*
                     * vanilla mechanics will spawn flowers etc. when bonemeal is used on grass,
                     * so we cannot set Result.ALLOW in this case because it would stop event-propagation
                     */
                    if(!onGrass) event.setResult(Result.ALLOW);
                }
            }
        }
    }

    @SubscribeEvent
    public void FillBucket(FillBucketEvent event){
        if(event.current == null || event.current.getItem() != Items.bucket) return;
        ItemStack result = attemptFill(event.world, event.target);
        if(result != null) {
            event.result = result;
            event.setResult(Result.ALLOW);
        }
    }

    private ItemStack attemptFill(World world, MovingObjectPosition p){
        Block id = world.getBlock(p.blockX, p.blockY, p.blockZ);
        if(id == Blockss.etchingAcid) {
            if(world.getBlockMetadata(p.blockX, p.blockY, p.blockZ) == 0) // Check that it is a source block
            {
                world.setBlock(p.blockX, p.blockY, p.blockZ, net.minecraft.init.Blocks.air); // Remove the fluid block
                return new ItemStack(Itemss.bucketEtchingAcid); // Return the filled bucked item here.
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onPlayerClick(PlayerInteractEvent event){
        Block interactedBlock = event.world.getBlock(event.x, event.y, event.z);
        if(!event.entityPlayer.capabilities.isCreativeMode || !event.entityPlayer.canCommandSenderUseCommand(2, "securityStation")) {
            if(event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR && event.world != null && !event.world.isRemote) {
                if(interactedBlock != Blockss.securityStation || event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
                    ItemStack heldItem = event.entityPlayer.getCurrentEquippedItem();
                    boolean tryingToPlaceSecurityStation = heldItem != null && heldItem.getItem() instanceof ItemBlock && ((ItemBlock)heldItem.getItem()).field_150939_a == Blockss.securityStation;
                    int blockingStations = PneumaticCraftUtils.getProtectingSecurityStations(event.entity.worldObj, event.x, event.y, event.z, event.entityPlayer, true, tryingToPlaceSecurityStation);
                    if(blockingStations > 0) {
                        event.setCanceled(true);
                        event.entityPlayer.addChatComponentMessage(new ChatComponentText(StatCollector.translateToLocalFormatted(tryingToPlaceSecurityStation ? "message.securityStation.stationPlacementPrevented" : "message.securityStation.accessPrevented", blockingStations)));
                    }
                }
            }
        }

        /**
         * Due to some weird quirk that causes Block#onBlockActivated not getting called on the server when the player is sneaking, this is a workaround.
         */
        if(!event.isCanceled() && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && !event.world.isRemote) {
            if(event.entityPlayer.isSneaking() && interactedBlock == Blockss.elevatorCaller) {
                Blockss.elevatorCaller.onBlockActivated(event.world, event.x, event.y, event.z, event.entityPlayer, event.face, 0, 0, 0);
                event.setCanceled(true);
            } else if(event.entityPlayer.getCurrentEquippedItem() != null && ModInteractionUtilImplementation.getInstance().isModdedWrench(event.entityPlayer.getCurrentEquippedItem().getItem())) {
                if(interactedBlock instanceof IPneumaticWrenchable) {
                    ((IPneumaticWrenchable)interactedBlock).rotateBlock(event.world, event.entityPlayer, event.x, event.y, event.z, ForgeDirection.getOrientation(event.face));
                }
            }
        }
    }

    /**
     * Used by PneumaticHelmet
     * @param event
     */
    @SubscribeEvent
    public void onMobTargetSet(LivingSetAttackTargetEvent event){
        if(event.entity instanceof EntityCreature) {
            if(!event.entity.worldObj.isRemote) {
                NetworkHandler.sendToAllAround(new PacketSetMobTarget((EntityCreature)event.entity, event.target), new NetworkRegistry.TargetPoint(event.entity.worldObj.provider.dimensionId, event.entity.posX, event.entity.posY, event.entity.posZ, TileEntityConstants.PACKET_UPDATE_DISTANCE));
            } else {
                warnPlayerIfNecessary(event);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void warnPlayerIfNecessary(LivingSetAttackTargetEvent event){
        EntityPlayer player = FMLClientHandler.instance().getClient().thePlayer;
        if(event.target == player && (event.entityLiving instanceof EntityGolem || event.entityLiving instanceof EntityMob)) {
            ItemStack helmetStack = player.getCurrentArmor(3);
            if(helmetStack != null && helmetStack.getItem() == Itemss.pneumaticHelmet && ((IPressurizable)helmetStack.getItem()).getPressure(helmetStack) > 0 && ItemPneumaticArmor.getUpgrades(ItemMachineUpgrade.UPGRADE_ENTITY_TRACKER, helmetStack) > 0) {
                HUDHandler.instance().getSpecificRenderer(EntityTrackUpgradeHandler.class).warnIfNecessary(event.entityLiving);
            }
        } else {
            HUDHandler.instance().getSpecificRenderer(EntityTrackUpgradeHandler.class).removeTargetingEntity(event.entityLiving);
        }
    }

    @SubscribeEvent
    public void quetziMoo(ServerChatEvent event){
        if(event.username.equals("Quetzz") && event.message.equals("m00")) {
            for(int i = 0; i < 4; i++)
                NetworkHandler.sendTo(new PacketPlaySound("mob.cow.say", event.player.posX, event.player.posY, event.player.posZ, 1, 1, true), event.player);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event){
        if(!event.world.isRemote && event.world.provider.dimensionId == 0) {
            GlobalVariableManager.overworld = event.world;
            event.world.loadItemData(GlobalVariableManager.class, GlobalVariableManager.DATA_KEY);
        }
    }
}
