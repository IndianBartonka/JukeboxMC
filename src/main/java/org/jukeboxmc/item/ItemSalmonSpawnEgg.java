package org.jukeboxmc.item;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.entity.passive.EntitySalmon;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class ItemSalmonSpawnEgg extends Item {

    public ItemSalmonSpawnEgg() {
        super( "minecraft:salmon_spawn_egg" );
    }

    @Override
    public boolean interact( Player player, BlockFace blockFace, Vector clickedVector, Block clickedBlock ) {
        EntitySalmon entitySalmon = new EntitySalmon();
        entitySalmon.setLocation( clickedBlock.getSide( blockFace ).getLocation().add( 0.5f, 0, 0.5f ) );
        entitySalmon.spawn();

        return true;
    }
}