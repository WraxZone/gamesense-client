package com.gamesense.client.module.modules.combat;
import com.gamesense.api.setting.values.BooleanSetting;
import com.gamesense.api.setting.values.DoubleSetting;
import com.gamesense.api.setting.values.ModeSetting;
import com.gamesense.api.util.player.InventoryUtil;
import com.gamesense.api.util.player.PlayerUtil;
import com.gamesense.api.util.world.BlockUtil;
import com.gamesense.client.module.Category;
import com.gamesense.client.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


import java.util.Arrays;


@Module.Declaration(name = "Burrow", category = Category.Combat)
public class Burrow extends Module {
	
	BooleanSetting rotate = registerBoolean("Rotate", true);
	ModeSetting type = registerMode("Type", Arrays.asList("Packet", "Normal"), "Packet");
    ModeSetting block = registerMode("Block", Arrays.asList("All", "EChest", "Chest", "WhiteList"), "All");
    DoubleSetting force = registerDouble("Force", 1.5, -5.0, 10.0);
    BooleanSetting instant = registerBoolean("Instant", true);
    BooleanSetting center = registerBoolean("Center", false);
    BooleanSetting bypass = registerBoolean("Bypass", false);

    int swapBlock = -1;
    Vec3d centerBlock = Vec3d.ZERO;
    BlockPos oldPos;
    Block blockW = Blocks.OBSIDIAN;
    boolean flag;

    @Override
    public void onEnable() {
    	if ((mc.player == null || mc.world == null || mc.player.isDead)) {
            this.disable();
            return;
        }
        
        flag = false;
        if (AutoCrystal.INSTANCE.isEnabled()) {
            flag = true;
            AutoCrystal.INSTANCE.disable();
        }

        mc.player.motionX = 0;
        mc.player.motionZ = 0;

        centerBlock = this.getCenter(mc.player.posX, mc.player.posY, mc.player.posZ);
        if (centerBlock != Vec3d.ZERO && center.getValue()) {
            double x_diff = Math.abs(centerBlock.x - mc.player.posX);
            double z_diff = Math.abs(centerBlock.z - mc.player.posZ);
            if (x_diff <= 0.1 && z_diff <= 0.1) {
                centerBlock = Vec3d.ZERO;
            } else {
                double motion_x = centerBlock.x - mc.player.posX;
                double motion_z = centerBlock.z - mc.player.posZ;
                mc.player.motionX = motion_x / 2;
                mc.player.motionZ = motion_z / 2;
            }
        }

        oldPos = PlayerUtil.getPlayerPos();
        switch (block.getValue()) {
            case "All":
                swapBlock = PlayerUtil.findObiInHotbar();
                break;
            case "EChest":
                swapBlock = InventoryUtil.findHotbarBlock(BlockEnderChest.class);
                break;
            case "Chest":
                swapBlock = InventoryUtil.findHotbarBlock(BlockChest.class);
                break;
            case "WhiteList":
                swapBlock = InventoryUtil.findHotbarBlock(blockW.getClass());
        }
        if (swapBlock == -1) {
            this.disable();
            return;
        }
        if (instant.getValue()) {
            this.setTimer(50f);
        }
        if (type.is("Normal")) {
            mc.player.jump();
        }
    }
    
    
    @Override
    public void onUpdate() {
        if (type.is("Normal")) {
            if (mc.player.posY > (oldPos.getY() + 1.04)) {
                int old = mc.player.inventory.currentItem;
                this.switchToSlot(swapBlock);
                BlockUtil.placeBlock(oldPos, EnumHand.MAIN_HAND, rotate.getValue(), true, false);
                this.switchToSlot(old);
                mc.player.motionY = force.value;
                this.disable();
            }
        } else {
            mc.player.connection.sendPacket(
                    new CPacketPlayer.Position(
                            mc.player.posX,
                            mc.player.posY + 0.41999998688698,
                            mc.player.posZ,
                            true
                    )
            );
            mc.player.connection.sendPacket(
                    new CPacketPlayer.Position(
                            mc.player.posX,
                            mc.player.posY + 0.7531999805211997,
                            mc.player.posZ,
                            true
                    )
            );
            mc.player.connection.sendPacket(
                    new CPacketPlayer.Position(
                            mc.player.posX,
                            mc.player.posY + 1.00133597911214,
                            mc.player.posZ,
                            true
                    )
            );
            mc.player.connection.sendPacket(
                    new CPacketPlayer.Position(
                            mc.player.posX,
                            mc.player.posY + 1.16610926093821,
                            mc.player.posZ,
                            true
                    )
            );
            int old = mc.player.inventory.currentItem;
            this.switchToSlot(swapBlock);
            BlockUtil.placeBlock(oldPos, EnumHand.MAIN_HAND, rotate.getValue(), true, false);
            this.switchToSlot(old);
            mc.player.connection.sendPacket(
                    new CPacketPlayer.Position(
                            mc.player.posX,
                            mc.player.posY + force.getValue(),
                            mc.player.posZ,
                            false
                    )
            );
            if(bypass.getValue() && !mc.player.isSneaking()) {
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING));
                mc.player.setSneaking(true);
                mc.playerController.updateController();
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
                mc.player.setSneaking(false);
                mc.playerController.updateController();
            }
            this.disable();
        }
    }

    @Override
    public void onDisable(){
    	
    if(instant.getValue() && !(mc.player == null || mc.world == null || mc.player.isDead)){
            this.setTimer(1f);
     }
        
        if (flag) {
            AutoCrystal.INSTANCE.enable();
        }
    }

   
	private void switchToSlot(final int slot) {
        mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
        mc.player.inventory.currentItem = slot;
        mc.playerController.updateController();
    }
		private void setTimer(float value) {
    	
    	mc.timer.tickLength = 50f / value;
    }
  

    private Vec3d getCenter(double posX, double posY, double posZ) {
        double x = Math.floor(posX) + 0.5D;
        double y = Math.floor(posY);
        double z = Math.floor(posZ) + 0.5D ;

        return new Vec3d(x, y, z);
    }

    @Override
    public String getDisplayInfo() {
        return this.type.getValue();
    }

    public void setBlock(Block b){
        this.blockW = b;
    }

    public Block getBlock(){
        return this.blockW;
    }
    
    

}
