package org.jukeboxmc.item;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.entity.hostile.EntityPiglinBrute;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class ItemPiglinBruteSpawnEgg extends Item {

    public ItemPiglinBruteSpawnEgg() {
        super( "minecraft:piglin_brute_spawn_egg" );
    }

    @Override
    public boolean interact( Player player, BlockFace blockFace, Vector clickedVector, Block clickedBlock ) {
        EntityPiglinBrute entityPiglinBrute = new EntityPiglinBrute();
        entityPiglinBrute.setLocation( clickedBlock.getSide( blockFace ).getLocation().add( 0.5f, -entityPiglinBrute.getEyeHeight(), 0.5f ) );
        entityPiglinBrute.spawn();

        return true;
    }
}