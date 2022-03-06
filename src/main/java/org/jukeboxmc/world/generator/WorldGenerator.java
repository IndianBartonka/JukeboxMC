package org.jukeboxmc.world.generator;

import org.jukeboxmc.math.Vector;
import org.jukeboxmc.world.Dimension;
import org.jukeboxmc.world.World;
import org.jukeboxmc.world.chunk.Chunk;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public abstract class WorldGenerator {

    public abstract Chunk generate( int chunkX, int chunkZ, Dimension dimension );

    public abstract Vector getSpawnLocation();

    public abstract void populate( Chunk chunk );

    public abstract void setWorld( World world );
}
