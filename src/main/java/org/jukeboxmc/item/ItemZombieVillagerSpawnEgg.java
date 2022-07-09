package org.jukeboxmc.item;

import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.entity.hostile.EntityZombieVillagerV2;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class ItemZombieVillagerSpawnEgg extends Item {

    public ItemZombieVillagerSpawnEgg() {
        super( "minecraft:zombie_villager_spawn_egg" );
    }

    @Override
    public boolean interact( Player player, BlockFace blockFace, Vector clickedVector, Block clickedBlock ) {
        EntityZombieVillagerV2 entityZombieVillager = new EntityZombieVillagerV2();
        entityZombieVillager.setLocation( clickedBlock.getSide( blockFace ).getLocation().add( 0.5f, 0, 0.5f ) );
        entityZombieVillager.spawn();

        return true;
    }
}