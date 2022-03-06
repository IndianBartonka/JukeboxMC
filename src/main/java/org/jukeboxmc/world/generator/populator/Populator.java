package org.jukeboxmc.world.generator.populator;

import org.jukeboxmc.utils.FastRandom;
import org.jukeboxmc.world.World;
import org.jukeboxmc.world.chunk.Chunk;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public interface Populator {

    void populate( World world, Chunk chunk, FastRandom random );
}
