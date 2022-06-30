package org.jukeboxmc.world.generator.biome;

import org.jukeboxmc.world.generator.populator.Populator;
import org.jukeboxmc.world.generator.populator.TallGrassPopulator;

import java.util.List;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class PlainsBiome extends GrassyBiome {

    @Override
    public List<Populator> getPopulators() {
        TallGrassPopulator tallGrassPopulator = new TallGrassPopulator();
        tallGrassPopulator.setBaseAmount( 20 );
        return List.of( tallGrassPopulator );
    }
}
