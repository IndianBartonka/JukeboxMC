package org.jukeboxmc.blockentity;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.inventory.BrewingStandInventory;
import org.jukeboxmc.inventory.InventoryHolder;
import org.jukeboxmc.item.Item;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class BlockEntityBrewingStand extends BlockEntity implements InventoryHolder {

    private final BrewingStandInventory brewingStandInventory;

    public BlockEntityBrewingStand( Block block, BlockEntityType blockEntityType ) {
        super( block, blockEntityType );
        this.brewingStandInventory = new BrewingStandInventory( this );
    }

    @Override
    public boolean interact( Player player, Vector blockPosition, Vector clickedPosition, BlockFace blockFace, Item itemInHand ) {
        player.openInventory( this.brewingStandInventory, blockPosition );
        return true;
    }

    public BrewingStandInventory getBrewingStandInventory() {
        return this.brewingStandInventory;
    }
}
