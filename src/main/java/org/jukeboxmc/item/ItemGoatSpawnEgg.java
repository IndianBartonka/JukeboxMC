package org.jukeboxmc.item;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.entity.passive.EntityGoat;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class ItemGoatSpawnEgg extends Item {

    public ItemGoatSpawnEgg() {
        super( "minecraft:goat_spawn_egg" );
    }

    @Override
    public boolean interact( Player player, BlockFace blockFace, Vector clickedVector, Block clickedBlock ) {
        EntityGoat entityGoat = new EntityGoat();
        entityGoat.setLocation( clickedBlock.getSide( blockFace ).getLocation().add( 0.5f, 0, 0.5f ) );
        entityGoat.spawn();

        return true;
    }
}