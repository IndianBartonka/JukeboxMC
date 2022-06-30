package org.jukeboxmc.item;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.entity.hostile.EntitySilverfish;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class ItemSilverfishSpawnEgg extends Item {

    public ItemSilverfishSpawnEgg() {
        super( "minecraft:silverfish_spawn_egg" );
    }

    @Override
    public boolean interact( Player player, BlockFace blockFace, Vector clickedVector, Block clickedBlock ) {
        EntitySilverfish entitySilverfish = new EntitySilverfish();
        entitySilverfish.setLocation( clickedBlock.getSide( blockFace ).getLocation().add( 0.5f, -entitySilverfish.getEyeHeight(), 0.5f ) );
        entitySilverfish.spawn();

        return true;
    }
}