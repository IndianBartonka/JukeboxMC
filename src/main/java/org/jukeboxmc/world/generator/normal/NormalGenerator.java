package org.jukeboxmc.world.generator.normal;

import org.jukeboxmc.math.Vector;
import org.jukeboxmc.utils.FastRandom;
import org.jukeboxmc.world.Dimension;
import org.jukeboxmc.world.World;
import org.jukeboxmc.world.chunk.Chunk;
import org.jukeboxmc.world.generator.WorldGenerator;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class NormalGenerator extends WorldGenerator {

    private World world;

    @Override
    public Chunk generate( int chunkX, int chunkZ, Dimension dimension ) {
        Chunk chunk = new Chunk( this.world, chunkX, chunkZ, dimension );

        return chunk;
    }

    @Override
    public Vector getSpawnLocation() {
        return new Vector( 0, 150, 0 );
    }

    @Override
    public void populate( Chunk chunk ) {

    }

    @Override
    public void setWorld( World world ) {
        this.world = world;
    }
}
