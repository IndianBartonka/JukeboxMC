package org.jukeboxmc.item;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.entity.passive.EntityHorse;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class ItemHorseSpawnEgg extends Item {

    public ItemHorseSpawnEgg() {
        super( "minecraft:horse_spawn_egg" );
    }

    @Override
    public boolean interact( Player player, BlockFace blockFace, Vector clickedVector, Block clickedBlock ) {
        EntityHorse entityHorse = new EntityHorse();
        entityHorse.setLocation( clickedBlock.getSide( blockFace ).getLocation().add( 0.5f, 0, 0.5f ) );
        entityHorse.spawn();

        return true;
    }
}