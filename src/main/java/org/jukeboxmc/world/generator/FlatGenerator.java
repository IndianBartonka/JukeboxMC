package org.jukeboxmc.world.generator;

import org.jukeboxmc.block.BlockBedrock;
import org.jukeboxmc.block.BlockDirt;
import org.jukeboxmc.block.BlockGrass;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.world.Biome;
import org.jukeboxmc.world.Dimension;
import org.jukeboxmc.world.World;
import org.jukeboxmc.world.chunk.Chunk;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class FlatGenerator extends WorldGenerator {

    private World world;
    private final BlockGrass blockGrass;
    private final BlockDirt blockDirt;
    private final BlockBedrock blockBedrock;

    public FlatGenerator() {
        this.blockGrass = new BlockGrass();
        this.blockDirt = new BlockDirt();
        this.blockBedrock = new BlockBedrock();
    }

    @Override
    public Chunk generate( int chunkX, int chunkZ, Dimension dimension ) {
        Chunk chunk = new Chunk( this.world, chunkX, chunkX, dimension );
        for ( int blockX = 0; blockX < 16; blockX++ ) {
            for ( int blockZ = 0; blockZ < 16; blockZ++ ) {
                for ( int blockY = 0; blockY < 16; blockY++ ) {
                    chunk.setBiome( blockX, blockY, blockZ, Biome.PLAINS );
                }

                chunk.setBlock( blockX, 0, blockZ, 0, this.blockBedrock );
                chunk.setBlock( blockX, 1, blockZ, 0, this.blockDirt );
                chunk.setBlock( blockX, 2, blockZ, 0, this.blockDirt );
                chunk.setBlock( blockX, 3, blockZ, 0, this.blockGrass );
            }
        }
        return chunk;
    }

    @Override
    public Vector getSpawnLocation() {
        return new Vector( 0, 4, 0 );
    }

    @Override
    public void populate( Chunk chunk ) {

    }

    @Override
    public void setWorld( World world ) {
        this.world = world;
    }
}
