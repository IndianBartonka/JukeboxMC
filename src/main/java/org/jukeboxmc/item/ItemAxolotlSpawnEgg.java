package org.jukeboxmc.item;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.entity.passive.EntityAxolotl;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class ItemAxolotlSpawnEgg extends Item {

    public ItemAxolotlSpawnEgg() {
        super( "minecraft:axolotl_spawn_egg" );
    }

    @Override
    public boolean interact( Player player, BlockFace blockFace, Vector clickedVector, Block clickedBlock ) {
        EntityAxolotl entityAxolotl = new EntityAxolotl();
        entityAxolotl.setLocation( clickedBlock.getSide( blockFace ).getLocation().add( 0.5f, -entityAxolotl.getEyeHeight(), 0.5f ) );
        entityAxolotl.spawn();

        return true;
    }
}