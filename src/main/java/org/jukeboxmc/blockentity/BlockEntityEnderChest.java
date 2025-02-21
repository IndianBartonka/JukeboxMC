package org.jukeboxmc.blockentity;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.item.Item;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class BlockEntityEnderChest extends BlockEntity {

    public BlockEntityEnderChest( Block block, BlockEntityType blockEntityType ) {
        super( block, blockEntityType );
    }

    @Override
    public boolean interact( Player player, Vector blockPosition, Vector clickedPosition, BlockFace blockFace, Item itemInHand ) {
        player.getEnderChestInventory().setPosition( blockPosition );
        player.openInventory( player.getEnderChestInventory(), blockPosition );
        return true;
    }
}
