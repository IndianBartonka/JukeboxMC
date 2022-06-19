package org.jukeboxmc.entity;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jukeboxmc.entity.item.EntityItem;
import org.jukeboxmc.entity.passive.EntityCow;
import org.jukeboxmc.entity.passive.EntityHuman;
import org.jukeboxmc.entity.passive.EntityNPC;
import org.jukeboxmc.entity.projectile.EntityArrow;
import org.jukeboxmc.entity.projectile.EntityEgg;
import org.jukeboxmc.entity.projectile.EntityFishingHook;
import org.jukeboxmc.entity.projectile.EntitySnowball;

/**
 * @author LucGamesYT
 * @version 1.0
 */
@AllArgsConstructor
public enum EntityType {

    HUMAN( EntityHuman.class, "minecraft:player" ),
    ITEM( EntityItem.class, "minecraft:item" ),
    ARROW( EntityArrow.class, "minecraft:arrow" ),
    SNOWBALL( EntitySnowball.class, "minecraft:snowball" ),
    FISHING_HOOK( EntityFishingHook.class, "minecraft:fishing_hook" ),
    EGG( EntityEgg.class, "minecraft:egg" ),
    NPC( EntityNPC.class, "minecraft:npc" ),
    COW( EntityCow.class, "minecraft:cow" );

    private final Class<? extends Entity> entityClass;
    private final String identifier;

    @SneakyThrows
    public <E extends Entity> E createEntity() {
        return (E) this.entityClass.getConstructor().newInstance();
    }

    public String getIdentifier() {
        return this.identifier;
    }
}
