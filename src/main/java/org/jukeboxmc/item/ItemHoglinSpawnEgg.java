package org.jukeboxmc.item;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.entity.hostile.EntityHoglin;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class ItemHoglinSpawnEgg extends Item {

    public ItemHoglinSpawnEgg() {
        super( "minecraft:hoglin_spawn_egg" );
    }

    @Override
    public boolean interact( Player player, BlockFace blockFace, Vector clickedVector, Block clickedBlock ) {
        EntityHoglin entityHoglin = new EntityHoglin();
        entityHoglin.setLocation( clickedBlock.getSide( blockFace ).getLocation().add( 0.5f, -entityHoglin.getEyeHeight(), 0.5f ) );
        entityHoglin.spawn();

        return true;
    }
}