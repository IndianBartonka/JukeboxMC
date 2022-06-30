package org.jukeboxmc.item;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.entity.passive.EntityCow;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class ItemCowSpawnEgg extends Item {

    public ItemCowSpawnEgg() {
        super( "minecraft:cow_spawn_egg" );
    }

    @Override
    public boolean interact( Player player, BlockFace blockFace, Vector clickedVector, Block clickedBlock ) {
        EntityCow entityCow = new EntityCow();
        entityCow.setLocation( clickedBlock.getSide( blockFace ).getLocation().add( 0.5f, 0, 0.5f ) );
        entityCow.spawn();
        return true;
    }
}