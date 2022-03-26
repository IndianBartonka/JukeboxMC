package org.jukeboxmc.crafting.data;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public enum CraftingDataType {

    SHAPELESS,
    SHAPED,
    FURNACE,
    FURNACE_DATA,
    MULTI,
    SHULKER_BOX,
    SHAPELESS_CHEMISTRY,
    SHAPED_CHEMISTRY;

    private static final CraftingDataType[] VALUES = values();

    public static CraftingDataType byId(int id) {
        if (id >= 0 && id < VALUES.length) {
            return VALUES[id];
        }
        throw new UnsupportedOperationException("Unknown CraftingDataType ID: " + id);
    }
}
