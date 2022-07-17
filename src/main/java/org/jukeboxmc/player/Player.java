package org.jukeboxmc.player;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.*;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.command.CommandData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.math3.util.FastMath;
import org.jukeboxmc.Server;
import org.jukeboxmc.command.Command;
import org.jukeboxmc.command.CommandSender;
import org.jukeboxmc.entity.Entity;
import org.jukeboxmc.entity.EntityLiving;
import org.jukeboxmc.entity.attribute.Attribute;
import org.jukeboxmc.entity.attribute.AttributeType;
import org.jukeboxmc.entity.passive.EntityHuman;
import org.jukeboxmc.entity.projectile.EntityFishingHook;
import org.jukeboxmc.event.entity.EntityDamageByEntityEvent;
import org.jukeboxmc.event.entity.EntityDamageEvent;
import org.jukeboxmc.event.entity.EntityHealEvent;
import org.jukeboxmc.event.inventory.InventoryCloseEvent;
import org.jukeboxmc.event.inventory.InventoryOpenEvent;
import org.jukeboxmc.event.player.PlayerDeathEvent;
import org.jukeboxmc.event.player.PlayerRespawnEvent;
import org.jukeboxmc.form.Form;
import org.jukeboxmc.form.FormListener;
import org.jukeboxmc.form.NpcDialogueForm;
import org.jukeboxmc.inventory.*;
import org.jukeboxmc.inventory.transaction.CraftingTransaction;
import org.jukeboxmc.inventory.transaction.InventoryAction;
import org.jukeboxmc.item.Item;
import org.jukeboxmc.item.ItemAir;
import org.jukeboxmc.item.ItemType;
import org.jukeboxmc.item.enchantment.EnchantmentKnockback;
import org.jukeboxmc.item.enchantment.EnchantmentSharpness;
import org.jukeboxmc.item.enchantment.EnchantmentType;
import org.jukeboxmc.math.Location;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.util.Utils;
import org.jukeboxmc.world.Difficulty;
import org.jukeboxmc.world.Dimension;
import org.jukeboxmc.world.Sound;
import org.jukeboxmc.world.World;
import org.jukeboxmc.world.chunk.Chunk;
import org.jukeboxmc.world.chunk.ChunkLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class Player extends EntityHuman implements ChunkLoader, CommandSender, InventoryHolder {

    private final PlayerConnection playerConnection;

    private Location spawnLocation;
    private GameMode gameMode;
    private final Server server;
    private final AdventureSettings adventureSettings;
    private CraftingTransaction craftingTransaction;

    private ContainerInventory currentInventory;
    private final CraftingTableInventory craftingTableInventory;
    private final CursorInventory cursorInventory;
    private final CartographyTableInventory cartographyTableInventory;
    private final SmithingTableInventory smithingTableInventory;
    private final AnvilInventory anvilInventory;
    private final EnderChestInventory enderChestInventory;
    private final StoneCutterInventory stoneCutterInventory;
    private final GrindstoneInventory grindstoneInventory;

    private EntityFishingHook entityFishingHook;

    private Location respawnLocation = null;

    private File playerFile;

    private long lastBreakTime = 0;
    private Vector lasBreakPosition;
    private boolean breakingBlock = false;

    private int viewDistance = 8;
    private int inAirTicks = 0;

    private final Map<UUID, Set<String>> permissions = new HashMap<>();

    private int formId;
    private int serverSettingsForm = -1;
    private final Int2ObjectMap<Form<?>> forms = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<FormListener<?>> formListeners = new Int2ObjectOpenHashMap<>();

    private final ObjectArrayList<NpcDialogueForm> npcDialogueForms = new ObjectArrayList<>();

    public Player( PlayerConnection playerConnection ) {
        this.playerConnection = playerConnection;
        this.spawnLocation = this.location.getWorld().getSpawnLocation();
        this.gameMode = playerConnection.getServer().getGameMode();
        this.server = playerConnection.getServer();
        this.adventureSettings = new AdventureSettings( this );

        this.cursorInventory = new CursorInventory( this, this.entityId );
        this.craftingTableInventory = new CraftingTableInventory( this );
        this.cartographyTableInventory = new CartographyTableInventory( this );
        this.smithingTableInventory = new SmithingTableInventory( this );
        this.anvilInventory = new AnvilInventory( this );
        this.enderChestInventory = new EnderChestInventory( this );
        this.stoneCutterInventory = new StoneCutterInventory( this );
        this.grindstoneInventory = new GrindstoneInventory( this );

        this.lasBreakPosition = new Vector( 0, 0, 0 );
    }

    @Override
    public void update( long currentTick ) {
        if ( !this.playerConnection.isLoggedIn() ) {
            return;
        }
        super.update( currentTick );

        if ( this.isSpawned() ) {
            Collection<Entity> nearbyEntities = this.getWorld().getNearbyEntities( this.getBoundingBox().grow( 1, 0.5f, 1 ), this.dimension, this );
            if ( nearbyEntities != null ) {
                for ( Entity nearbyEntity : nearbyEntities ) {
                    if ( !nearbyEntity.isClosed() ) {
                        nearbyEntity.onCollideWithPlayer( this );
                    }
                }
            }
        }

        if ( !this.onGround && !this.isOnLadder() ) {
            ++this.inAirTicks;
            if ( this.inAirTicks > 5 ) {
                if ( this.location.getY() > this.highestPosition ) {
                    this.highestPosition = this.location.getY();
                }
            }
        } else {
            if ( this.inAirTicks > 0 ) {
                this.inAirTicks = 0;
            }

            this.fallDistance = this.highestPosition - this.location.getY();
            if ( this.fallDistance > 0 ) {
                this.fall();
                this.highestPosition = this.location.getY();
                this.fallDistance = 0;
            }
        }

        if ( !this.isDead ) {
            Attribute hungerAttribute = this.getAttribute( AttributeType.PLAYER_HUNGER );
            float hunger = hungerAttribute.getCurrentValue();
            float health = -1;
            Difficulty difficulty = this.getWorld().getDifficulty();
            if ( difficulty.equals( Difficulty.PEACEFUL ) && this.foodTicks % 10 == 0 ) {
                if ( hunger < hungerAttribute.getMaxValue() ) {
                    this.addHunger( 1 );
                }
                if ( this.foodTicks % 20 == 0 ) {
                    health = this.getHealth();
                    if ( health < this.getAttribute( AttributeType.HEALTH ).getMaxValue() ) {
                        this.setHeal( 1, EntityHealEvent.Cause.SATURATION );
                    }
                }
            }
            if ( this.foodTicks == 0 ) {
                if ( hunger >= 18 ) {
                    if ( health == -1 ) {
                        health = this.getHealth();
                    }

                    if ( health < 20 ) {
                        this.setHeal( 1, EntityHealEvent.Cause.SATURATION );
                        if ( this.getGameMode().equals( GameMode.SURVIVAL ) ) {
                            this.exhaust( 3 );
                        }
                    }
                } else if ( hunger <= 0 ) {
                    if ( health == -1 ) {
                        health = this.getHealth();
                    }

                    if ( ( health > 10 && difficulty.equals( Difficulty.NORMAL ) ) || ( difficulty.equals( Difficulty.HARD ) && health > 1 ) ) {
                        this.damage( new EntityDamageEvent( this, 1, EntityDamageEvent.DamageSource.STARVE ) );
                    }
                }
            }
            this.foodTicks++;
            if ( this.foodTicks >= 80 ) {
                this.foodTicks = 0;
            }
        }

        this.updateAttributes();
    }

    public void loadPlayerData() {
        this.playerFile = new File( this.server.getPlayersFolder(), this.uuid.toString() + ".dat" );
        if ( !this.playerFile.exists() ) {
            try {
                this.playerFile.createNewFile();
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            } finally {
                ByteBuf buffer = Unpooled.buffer();
                try ( NBTOutputStream stream = NbtUtils.createWriterLE( new ByteBufOutputStream( buffer ) ) ) {
                    NbtMapBuilder root = NbtMap.builder();
                    NbtMapBuilder builder = NbtMap.builder();
                    World defaultWorld = this.server.getDefaultWorld();

                    builder.putFloat( "playerX", defaultWorld.getSpawnLocation().getX() );
                    builder.putFloat( "playerY", defaultWorld.getSpawnLocation().getY() );
                    builder.putFloat( "playerZ", defaultWorld.getSpawnLocation().getZ() );
                    builder.putFloat( "playerYaw", defaultWorld.getSpawnLocation().getYaw() );
                    builder.putFloat( "playerPitch", defaultWorld.getSpawnLocation().getPitch() );
                    builder.putInt( "dimension", this.dimension.ordinal() );
                    List<NbtMap> playerInventoryItems = new ArrayList<>();
                    for ( int slot = 0; slot < this.playerInventory.getContents().length; slot++ ) {
                        NbtMapBuilder itemCompound = NbtMap.builder();
                        Item item = this.playerInventory.getItem( slot );
                        itemCompound.putByte( "Slot", (byte) slot );
                        itemCompound.putString( "Name", item.getIdentifier() );
                        itemCompound.putShort( "Damage", (short) item.getMeta() );
                        itemCompound.putByte( "Count", (byte) item.getAmount() );
                        if ( item.getNBT() != null ) {
                            itemCompound.putCompound( "tag", item.getNBT() );
                        }

                        playerInventoryItems.add( itemCompound.build() );
                    }
                    builder.putList( "playerInventory", NbtType.COMPOUND, playerInventoryItems );

                    List<NbtMap> armorInventoryItems = new ArrayList<>();
                    for ( int slot = 0; slot < this.armorInventory.getContents().length; slot++ ) {
                        NbtMapBuilder itemCompound = NbtMap.builder();
                        Item item = this.playerInventory.getItem( slot );
                        itemCompound.putByte( "Slot", (byte) slot );
                        itemCompound.putString( "Name", item.getIdentifier() );
                        itemCompound.putShort( "Damage", (short) item.getMeta() );
                        itemCompound.putByte( "Count", (byte) item.getAmount() );
                        if ( item.getNBT() != null ) {
                            itemCompound.putCompound( "tag", item.getNBT() );
                        }

                        armorInventoryItems.add( itemCompound.build() );
                    }
                    builder.putList( "armorInventory", NbtType.COMPOUND, armorInventoryItems );

                    builder.putInt( "gamemode", this.gameMode.ordinal() );
                    builder.putFloat( "health", this.getHealth() );
                    builder.putFloat( "hunger", this.getHunger() );
                    builder.putFloat( "saturation", this.getSaturation() );
                    builder.putFloat( "exhaustion", this.getExhaustion() );

                    builder.putFloat( "level", this.getLevel() );
                    builder.putFloat( "experience", this.getExperience() );
                    root.putCompound( defaultWorld.getName(), builder.build() );
                    stream.writeTag( root.build() );
                    try ( ByteBufInputStream byteBufInputStream = new ByteBufInputStream( buffer ) ) {
                        Utils.writeFile( this.playerFile, byteBufInputStream );
                    }
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        } else {
            try ( NBTInputStream stream = NbtUtils.createReaderLE( new FileInputStream( this.playerFile ) ) ) {
                NbtMap nbt = (NbtMap) stream.readTag();
                World world = this.getWorld();
                NbtMap compound = nbt.getCompound( world.getName() );
                float playerX = compound.getFloat( "playerX", world.getSpawnLocation().getX() );
                float playerY = compound.getFloat( "playerY", world.getSpawnLocation().getY() );
                float playerZ = compound.getFloat( "playerZ", world.getSpawnLocation().getZ() );
                float playerYaw = compound.getFloat( "playerYaw", world.getSpawnLocation().getYaw() );
                float playerPitch = compound.getFloat( "playerPitch", world.getSpawnLocation().getPitch() );
                int dimension = compound.getInt( "dimension", Dimension.OVERWORLD.ordinal() );
                this.location = new Location( world, playerX, playerY, playerZ, playerYaw, playerPitch, Dimension.values()[dimension] );

                List<NbtMap> playerInventoryList = compound.getList( "playerInventory", NbtType.COMPOUND );
                if ( playerInventoryList.size() > 0 ) {
                    for ( NbtMap nbtMap : playerInventoryList ) {
                        byte slot = nbtMap.getByte( "Slot" );
                        String name = nbtMap.getString( "Name" );
                        int meta = nbtMap.getShort( "Damage" );
                        byte amount = nbtMap.getByte( "Count" );
                        NbtMap itemNbt = nbtMap.getCompound( "tag" );

                        Item item = ItemType.get( name );
                        item.setMeta( meta );
                        item.setAmount( amount );
                        item.setNBT( itemNbt );

                        this.playerInventory.setItem( slot, item );
                    }
                }

                List<NbtMap> armorInventoryList = compound.getList( "armorInventory", NbtType.COMPOUND );
                if ( armorInventoryList.size() > 0 ) {
                    for ( NbtMap nbtMap : armorInventoryList ) {
                        byte slot = nbtMap.getByte( "Slot" );
                        String name = nbtMap.getString( "Name" );
                        int meta = nbtMap.getShort( "Damage" );
                        byte amount = nbtMap.getByte( "Count" );
                        NbtMap itemNbt = nbtMap.getCompound( "tag" );

                        Item item = ItemType.get( name );
                        item.setMeta( meta );
                        item.setAmount( amount );
                        item.setNBT( itemNbt );

                        this.armorInventory.setItem( slot, item );
                    }
                }

                this.setGameMode( GameMode.values()[compound.getInt( "gamemode", this.server.getGameMode().getId() )] );
                this.setHealth( compound.getFloat( "health", 20 ) );
                this.setHunger( (int) compound.getFloat( "hunger", 20 ) );
                this.setSaturation( compound.getFloat( "saturation", 20 ) );
                this.setExhaustion( compound.getFloat( "exhaustion", 0.41f ) );
                this.setLevel( compound.getFloat( "level", 0 ) );
                this.setExperience( compound.getFloat( "experience", 0 ) );

            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    public void savePlayerData() {
        if ( this.playerFile != null && this.playerFile.exists() ) {
            ByteBuf buffer = Unpooled.buffer();
            try ( NBTOutputStream stream = NbtUtils.createWriterLE( new ByteBufOutputStream( buffer ) ) ) {
                NbtMapBuilder root = NbtMap.builder();
                NbtMapBuilder builder = NbtMap.builder();
                World world = this.location.getWorld();

                builder.putFloat( "playerX", this.location.getX() );
                builder.putFloat( "playerY", this.location.getY() );
                builder.putFloat( "playerZ", this.location.getZ() );
                builder.putFloat( "playerYaw", this.location.getYaw() );
                builder.putFloat( "playerPitch", this.location.getPitch() );
                builder.putInt( "dimension", this.dimension.ordinal() );
                List<NbtMap> playerInventoryItems = new ArrayList<>();
                for ( int slot = 0; slot < this.playerInventory.getContents().length; slot++ ) {
                    NbtMapBuilder itemCompound = NbtMap.builder();
                    Item item = this.playerInventory.getItem( slot );
                    itemCompound.putByte( "Slot", (byte) slot );
                    itemCompound.putString( "Name", item.getIdentifier() );
                    itemCompound.putShort( "Damage", (short) item.getMeta() );
                    itemCompound.putByte( "Count", (byte) item.getAmount() );
                    if ( item.getNBT() != null ) {
                        itemCompound.putCompound( "tag", item.getNBT() );
                    }

                    playerInventoryItems.add( itemCompound.build() );
                }
                builder.putList( "playerInventory", NbtType.COMPOUND, playerInventoryItems );

                List<NbtMap> armorInventoryItems = new ArrayList<>();
                for ( int slot = 0; slot < this.armorInventory.getContents().length; slot++ ) {
                    NbtMapBuilder itemCompound = NbtMap.builder();
                    Item item = this.armorInventory.getItem( slot );
                    itemCompound.putByte( "Slot", (byte) slot );
                    itemCompound.putString( "Name", item.getIdentifier() );
                    itemCompound.putShort( "Damage", (short) item.getMeta() );
                    itemCompound.putByte( "Count", (byte) item.getAmount() );
                    if ( item.getNBT() != null ) {
                        itemCompound.putCompound( "tag", item.getNBT() );
                    }

                    armorInventoryItems.add( itemCompound.build() );
                }
                builder.putList( "armorInventory", NbtType.COMPOUND, armorInventoryItems );

                builder.putInt( "gamemode", this.gameMode.ordinal() );
                builder.putFloat( "health", this.getHealth() );
                builder.putFloat( "hunger", this.getHunger() );
                builder.putFloat( "saturation", this.getSaturation() );
                builder.putFloat( "exhaustion", this.getExhaustion() );

                builder.putFloat( "level", this.getLevel() );
                builder.putFloat( "experience", this.getExperience() );

                root.putCompound( world.getName(), builder.build() );
                stream.writeTag( root.build() );
                try ( ByteBufInputStream byteBufInputStream = new ByteBufInputStream( buffer ) ) {
                    Utils.writeFile( this.playerFile, byteBufInputStream );
                }
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Server getServer() {
        return this.server;
    }

    public boolean isSpawned() {
        return this.playerConnection.isSpawned();
    }

    public boolean isClosed() {
        return this.playerConnection.isClosed();
    }

    public boolean isBreakingBlock() {
        return this.breakingBlock;
    }

    public void setBreakingBlock( boolean breakingBlock ) {
        this.breakingBlock = breakingBlock;
    }

    public long getLastBreakTime() {
        return this.lastBreakTime;
    }

    public void setLastBreakTime( long lastBreakTime ) {
        this.lastBreakTime = lastBreakTime;
    }

    public Vector getLasBreakPosition() {
        return this.lasBreakPosition;
    }

    public void setLasBreakPosition( Vector lasBreakPosition ) {
        this.lasBreakPosition = lasBreakPosition;
    }

    public String getXuid() {
        return this.playerConnection.getLoginData().getXuid();
    }

    public AdventureSettings getAdventureSettings() {
        return this.adventureSettings;
    }

    public ContainerInventory getCurrentInventory() {
        return this.currentInventory;
    }

    public CraftingTableInventory getCraftingTableInventory() {
        return this.craftingTableInventory;
    }

    public CursorInventory getCursorInventory() {
        return this.cursorInventory;
    }

    public CartographyTableInventory getCartographyTableInventory() {
        return this.cartographyTableInventory;
    }

    public SmithingTableInventory getSmithingTableInventory() {
        return this.smithingTableInventory;
    }

    public AnvilInventory getAnvilInventory() {
        return this.anvilInventory;
    }

    public EnderChestInventory getEnderChestInventory() {
        return this.enderChestInventory;
    }

    public StoneCutterInventory getStoneCutterInventory() {
        return this.stoneCutterInventory;
    }

    public GrindstoneInventory getGrindstoneInventory() {
        return this.grindstoneInventory;
    }

    public Inventory getInventory( WindowId windowIdById, int slot ) {
        return switch ( windowIdById ) {
            case PLAYER -> this.getInventory();
            case CURSOR_DEPRECATED -> this.getCursorInventory();
            case ARMOR_DEPRECATED -> this.getArmorInventory();
            default -> this.getCurrentInventory();
        };
    }

    public Location getSpawnLocation() {
        return this.spawnLocation;
    }

    public void setSpawnLocation( Location spawnLocation ) {
        this.spawnLocation = spawnLocation;

        SetSpawnPositionPacket setSpawnPositionPacket = new SetSpawnPositionPacket();
        setSpawnPositionPacket.setSpawnType( SetSpawnPositionPacket.Type.PLAYER_SPAWN );
        setSpawnPositionPacket.setDimensionId( this.dimension.ordinal() );
        setSpawnPositionPacket.setSpawnPosition( spawnLocation.toVector3i() );
        setSpawnPositionPacket.setBlockPosition( this.location.getWorld().getSpawnLocation().toVector3i() );
        this.playerConnection.sendPacket( setSpawnPositionPacket );
    }

    public GameMode getGameMode() {
        return this.gameMode;
    }

    public void setGameMode( GameMode gameMode ) {
        this.gameMode = gameMode;


        this.adventureSettings.set( AdventureSettings.Type.WORLD_IMMUTABLE, this.gameMode.ordinal() == 3 );
        this.adventureSettings.set( AdventureSettings.Type.ALLOW_FLIGHT, this.gameMode.ordinal() > 0 );
        this.adventureSettings.set( AdventureSettings.Type.NO_CLIP, this.gameMode.ordinal() == 3 );
        this.adventureSettings.set( AdventureSettings.Type.FLYING, this.gameMode.ordinal() == 3 );
        this.adventureSettings.set( AdventureSettings.Type.ATTACK_MOBS, this.gameMode.ordinal() < 2 );
        this.adventureSettings.set( AdventureSettings.Type.ATTACK_PLAYERS, this.gameMode.ordinal() < 2 );
        this.adventureSettings.set( AdventureSettings.Type.NO_PVM, this.gameMode.ordinal() == 3 );
        this.adventureSettings.update();

        SetPlayerGameTypePacket setPlayerGameTypePacket = new SetPlayerGameTypePacket();
        setPlayerGameTypePacket.setGamemode( gameMode.getId() );
        this.playerConnection.sendPacket( setPlayerGameTypePacket );
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public void setViewDistance( int viewDistance ) {
        this.viewDistance = viewDistance;

        ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
        chunkRadiusUpdatedPacket.setRadius( viewDistance );
        this.playerConnection.sendPacket( chunkRadiusUpdatedPacket );
    }

    public int getInAirTicks() {
        return this.inAirTicks;
    }

    public void setInAirTicks( int inAirTicks ) {
        this.inAirTicks = inAirTicks;
    }

    public Location getRespawnLocation() {
        return respawnLocation;
    }

    public void setRespawnLocation( Location respawnLocation ) {
        this.respawnLocation = respawnLocation;
    }

    public long getPing() {
        return this.playerConnection.getPing();
    }

    public void sendEntityData() {
        SetEntityDataPacket setEntityDataPacket = new SetEntityDataPacket();
        setEntityDataPacket.setRuntimeEntityId( this.entityId );
        setEntityDataPacket.getMetadata().putAll( this.metadata.getEntityDataMap() );
        setEntityDataPacket.setTick( this.server.getCurrentTick() );
        this.sendPacket( setEntityDataPacket );
    }

    public void updateAttributes() {
        UpdateAttributesPacket updateAttributesPacket = null;
        for ( Attribute attribute : this.getAttributes() ) {
            if ( attribute.isDirty() ) {
                if ( updateAttributesPacket == null ) {
                    updateAttributesPacket = new UpdateAttributesPacket();
                    updateAttributesPacket.setRuntimeEntityId( this.entityId );
                }
                updateAttributesPacket.getAttributes().add( attribute.toNetwork() );
            }
        }

        if ( updateAttributesPacket != null ) {
            updateAttributesPacket.setTick( this.server.getCurrentTick() );
            this.playerConnection.sendPacket( updateAttributesPacket );
        }
    }

    public void teleport( Location location, MovePlayerPacket.Mode mode ) {
        World currentWorld = this.getWorld();
        World world = location.getWorld();

        this.highestPosition = 0;
        this.fallDistance = 0;
        this.inAirTicks = 0;
        this.playerConnection.getChunkLoadQueue().clear();

        LongIterator iterator = this.getPlayerConnection().getLoadedChunks().iterator();
        while ( iterator.hasNext() ) {
            long hash = iterator.nextLong();
            int x = Utils.fromHashX( hash );
            int z = Utils.fromHashZ( hash );

            if ( FastMath.abs( x - location.getChunkX() ) > viewDistance || FastMath.abs( z - location.getChunkZ() ) > viewDistance ) {
                this.getWorld().removeChunkLoader( x, z, this.getDimension(), this );
                iterator.remove();
            }
        }

        if ( currentWorld != world ) {
            this.despawn();
            currentWorld.getPlayers().forEach( player -> player.despawn( this ) );

            this.getChunk().removeEntity( this );
            currentWorld.removeEntity( this );

            this.playerConnection.resetChunks();

            this.setLocation( location );
            this.playerConnection.needNewChunks();

            world.addEntity( this );
            this.getChunk().addEntity( this );
            this.spawn();
            world.getPlayers().forEach( player -> player.spawn( this ) );
            this.movePlayer( location, mode );
            return;
        }

        this.setLocation( location );
        this.movePlayer( location, mode );
    }

    public void teleport( Location location ) {
        this.teleport( location, MovePlayerPacket.Mode.TELEPORT );
    }

    public void teleport( Player player ) {
        this.teleport( player.getLocation() );
    }

    public void movePlayer( Location location, MovePlayerPacket.Mode mode ) {
        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId( this.entityId );
        movePlayerPacket.setPosition( location.toVector3f().add( 0, this.getEyeHeight(), 0 ) );
        movePlayerPacket.setRotation( Vector3f.from( location.getPitch(), location.getYaw(), location.getYaw() ) );
        movePlayerPacket.setMode( mode );
        if ( mode == MovePlayerPacket.Mode.TELEPORT ) {
            movePlayerPacket.setTeleportationCause( MovePlayerPacket.TeleportationCause.BEHAVIOR );
        }
        movePlayerPacket.setOnGround( this.onGround );
        movePlayerPacket.setRidingRuntimeEntityId( 0 );
        movePlayerPacket.setTick( this.server.getCurrentTick() );
        this.sendPacket( movePlayerPacket );
    }

    public void movePlayer( Player player, MovePlayerPacket.Mode mode ) {
        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId( player.getEntityId() );
        movePlayerPacket.setPosition( player.getLocation().toVector3f().add( 0, player.getEyeHeight(), 0 ) );
        movePlayerPacket.setRotation( Vector3f.from( player.getLocation().getPitch(), player.getLocation().getYaw(), player.getLocation().getYaw() ) );
        movePlayerPacket.setMode( mode );
        if ( mode == MovePlayerPacket.Mode.TELEPORT ) {
            movePlayerPacket.setTeleportationCause( MovePlayerPacket.TeleportationCause.BEHAVIOR );
        }
        movePlayerPacket.setOnGround( player.isOnGround() );
        movePlayerPacket.setRidingRuntimeEntityId( 0 );
        movePlayerPacket.setTick( this.server.getCurrentTick() );
        this.sendPacket( movePlayerPacket );
    }

    @Override
    public void sendMessage( String text ) {
        TextPacket textPacket = new TextPacket();
        textPacket.setType( TextPacket.Type.RAW );
        textPacket.setMessage( text );
        textPacket.setNeedsTranslation( false );
        textPacket.setXuid( this.getXuid() );
        textPacket.setPlatformChatId( this.deviceInfo.getDeviceId() );
        this.sendPacket( textPacket );
    }

    public void sendTip( String text ) {
        TextPacket textPacket = new TextPacket();
        textPacket.setType( TextPacket.Type.TIP );
        textPacket.setMessage( text );
        textPacket.setNeedsTranslation( false );
        textPacket.setXuid( this.getXuid() );
        textPacket.setPlatformChatId( this.deviceInfo.getDeviceId() );
        this.sendPacket( textPacket );
    }

    public void sendPopup( String text ) {
        TextPacket textPacket = new TextPacket();
        textPacket.setType( TextPacket.Type.POPUP );
        textPacket.setMessage( text );
        textPacket.setNeedsTranslation( false );
        textPacket.setXuid( this.getXuid() );
        textPacket.setPlatformChatId( this.deviceInfo.getDeviceId() );
        this.sendPacket( textPacket );
    }

    @Override
    public boolean hasPermission( String permission ) {
        return this.permissions.containsKey( this.uuid ) && this.permissions.get( this.uuid ).contains( permission.toLowerCase() ) || this.isOp() || permission.isEmpty();
    }

    public void addPermission( String permission ) {
        if ( !this.permissions.containsKey( this.uuid ) ) {
            this.permissions.put( this.uuid, new HashSet<>() );
        }
        this.permissions.get( this.uuid ).add( permission.toLowerCase() );
        this.sendCommandData();
    }

    public void addPermissions( Collection<String> permissions ) {
        if ( !this.permissions.containsKey( this.uuid ) ) {
            this.permissions.put( this.uuid, new HashSet<>( permissions ) );
        } else {
            this.permissions.get( this.uuid ).addAll( permissions );
        }
        this.sendCommandData();
    }

    public void removePermission( String permission ) {
        if ( this.permissions.containsKey( this.uuid ) ) {
            this.permissions.get( this.uuid ).remove( permission );
            this.sendCommandData();
        }
    }

    public void removePermissions( Collection<String> permissions ) {
        if ( this.permissions.containsKey( this.uuid ) ) {
            this.permissions.get( this.uuid ).removeAll( permissions );
            this.sendCommandData();
        }
    }

    public boolean isOp() {
        return this.adventureSettings.get( AdventureSettings.Type.OPERATOR );
    }

    public void setOp( boolean value ) {
        this.sendCommandData();
        this.adventureSettings.set( AdventureSettings.Type.OPERATOR, value );
        this.adventureSettings.update();
        if ( value ) {
            this.server.addOperatorToFile( this.getName() );
        } else {
            this.server.removeOperatorFromFile( this.getName() );
        }
        this.sendCommandData();
    }

    public void sendChunk( Chunk chunk ) {
        this.playerConnection.sendChunk( chunk );
    }

    public boolean isChunkLoaded( int chunkX, int chunkZ ) {
        return this.playerConnection.isChunkLoaded( chunkX, chunkZ );
    }

    public void sendCommandData() {
        AvailableCommandsPacket availableCommandsPacket = new AvailableCommandsPacket();
        Set<CommandData> commandList = new HashSet<>();
        for ( Command command : this.server.getPluginManager().getCommandManager().getCommands() ) {
            if ( !this.hasPermission( command.getCommandData().getPermission() ) ) {
                continue;
            }
            commandList.add( command.getCommandData().toNetwork() );
        }
        availableCommandsPacket.getCommands().addAll( commandList );
        this.sendPacket( availableCommandsPacket );
    }

    public void openInventory( ContainerInventory inventory, Vector position, byte windowId ) {
        InventoryOpenEvent inventoryOpenEvent = new InventoryOpenEvent( inventory, this );
        Server.getInstance().getPluginManager().callEvent( inventoryOpenEvent );
        if ( inventoryOpenEvent.isCancelled() ) {
            return;
        }

        if ( this.currentInventory != null ) {
            this.closeInventory( this.currentInventory );
        }
        inventory.addViewer( this, position, windowId );

        this.currentInventory = inventory;
    }

    public void openInventory( ContainerInventory inventory, Vector position ) {
        this.openInventory( inventory, position, (byte) WindowId.OPEN_CONTAINER.getId() );
    }

    public void openInventory( ContainerInventory inventory ) {
        this.openInventory( inventory, this.location );
    }

    public void closeInventory( ContainerInventory inventory ) {
        if ( this.currentInventory == inventory ) {
            this.closeInventory( WindowId.OPEN_CONTAINER.getId(), true );
        }
    }

    public void closeInventory( int windowId, boolean isServerSide ) {
        if ( this.currentInventory != null ) {
            this.currentInventory.removeViewer( this );

            ContainerClosePacket containerClosePacket = new ContainerClosePacket();
            containerClosePacket.setId( (byte) windowId );
            containerClosePacket.setUnknownBool0( isServerSide );
            this.playerConnection.sendPacket( containerClosePacket );

            Server.getInstance().getPluginManager().callEvent( new InventoryCloseEvent( this.currentInventory, this ) );

            this.currentInventory = null;
        }
    }

    public void playSound( Sound sound ) {
        this.playSound( this.location, sound, 1, 1 );
    }

    public void playSound( Sound sound, float volume, float pitch ) {
        this.playSound( this.location, sound, volume, pitch );
    }

    public void playSound( Vector position, Sound sound ) {
        this.playSound( position, sound, 1, 1 );
    }

    public void playSound( Vector position, Sound sound, float volume, float pitch ) {
        PlaySoundPacket playSoundPacket = new PlaySoundPacket();
        playSoundPacket.setPosition( position.toVector3f() );
        playSoundPacket.setSound( sound.getSound() );
        playSoundPacket.setVolume( volume );
        playSoundPacket.setPitch( pitch );
        this.playerConnection.sendPacket( playSoundPacket );
    }

    public boolean attackWithItemInHand( Entity target ) {
        if ( target instanceof EntityLiving living ) {
            boolean success = false;

            EntityDamageEvent.DamageSource damageSource = EntityDamageEvent.DamageSource.ENTITY_ATTACK;
            float damage = this.getAttackDamage();

            EnchantmentSharpness sharpness = (EnchantmentSharpness) this.playerInventory.getItemInHand().getEnchantment( EnchantmentType.SHARPNESS );
            if ( sharpness != null ) {
                damage += sharpness.getLevel() * 1.25f;
            }

            int knockbackLevel = 0;

            EnchantmentKnockback knockback = (EnchantmentKnockback) this.playerInventory.getItemInHand().getEnchantment( EnchantmentType.KNOCKBACK );
            if ( knockback != null ) {
                knockbackLevel += knockback.getLevel();
            }

            if ( damage > 0 ) {
                boolean crit = this.fallDistance > 0 && !this.onGround && !this.isOnLadder() && !this.isInWater();
                if ( crit && damage > 0.0f ) {
                    damage *= 1.5;
                }
                if ( success = living.damage( new EntityDamageByEntityEvent( living, this, damage, damageSource ) ) ) {
                    if ( knockbackLevel > 0 ) {
                        Vector targetVelocity = target.getVelocity();
                        living.setVelocity( targetVelocity.add(
                                (float) ( -Math.sin( this.getYaw() * (float) Math.PI / 180.0F ) * (float) knockbackLevel * 0.3 ),
                                0.1f,
                                (float) ( Math.cos( this.getYaw() * (float) Math.PI / 180.0F ) * (float) knockbackLevel * 0.3 ) ) );

                        Vector ownVelocity = this.getVelocity();
                        ownVelocity.setX( ownVelocity.getX() * 0.6F );
                        ownVelocity.setZ( ownVelocity.getZ() * 0.6F );
                        this.setVelocity( ownVelocity );

                        this.setSprinting( false );
                    }
                }
            }
            if ( this.getGameMode().equals( GameMode.SURVIVAL ) ) {
                this.exhaust( 0.3f );
            }

            return success;
        }
        return false;
    }

    @Override
    public boolean damage( EntityDamageEvent event ) {
        if ( this.adventureSettings.get( AdventureSettings.Type.ALLOW_FLIGHT ) && event.getDamageSource().equals( EntityDamageEvent.DamageSource.FALL ) ) {
            return false;
        }
        return !this.gameMode.equals( GameMode.CREATIVE ) && !this.gameMode.equals( GameMode.SPECTATOR ) && super.damage( event );
    }

    @Override
    protected float applyArmorReduction( EntityDamageEvent event, boolean damageArmor ) {
        if ( event.getDamageSource().equals( EntityDamageEvent.DamageSource.FALL ) ||
                event.getDamageSource().equals( EntityDamageEvent.DamageSource.VOID ) ||
                event.getDamageSource().equals( EntityDamageEvent.DamageSource.DROWNING ) ) {
            return event.getDamage();
        }

        float damage = event.getDamage();
        float maxReductionDiff = 25.0f - this.armorInventory.getTotalArmorValue() * 0.04f;
        float amplifiedDamage = damage * maxReductionDiff;
        if ( damageArmor ) {
            this.armorInventory.damageEvenly( damage );
        }

        return amplifiedDamage / 25.0F;
    }

    @Override
    protected void killEntity() {
        if ( !this.isDead ) {
            EntityEventPacket entityEventPacket = new EntityEventPacket();
            entityEventPacket.setRuntimeEntityId( this.entityId );
            entityEventPacket.setType( EntityEventType.DEATH );
            Server.getInstance().broadcastPacket( entityEventPacket );

            this.fallDistance = 0;
            this.highestPosition = 0;
            this.inAirTicks = 0;
            this.fireTicks = 0;

            this.setBurning( false );
            String deathMessage;
            if ( this.lastDamageSource != null ) {
                deathMessage = switch ( this.lastDamageSource ) {
                    case ENTITY_ATTACK -> this.getNameTag() + " was slain by " + this.getLastDamageEntity().getNameTag();
                    case FALL -> this.getNameTag() + " fell from a high place";
                    case LAVA -> this.getNameTag() + " tried to swim in lava";
                    case FIRE -> this.getNameTag() + " went up in flames";
                    case VOID -> this.getNameTag() + " fell out of the world";
                    case CACTUS -> this.getNameTag() + " was pricked to death";
                    case STARVE -> this.getNameTag() + " starved to death";
                    case ON_FIRE -> this.getNameTag() + " burned to death";
                    case DROWNING -> this.getNameTag() + " drowned";
                    case HARM_EFFECT -> this.getNameTag() + " was killed by magic";
                    case ENTITY_EXPLODE -> this.getNameTag() + " blew up";
                    case PROJECTILE -> this.getNameTag() + " has been shot";
                    case API -> this.getNameTag() + " was killed by setting health to 0";
                    case COMMAND -> this.getNameTag() + " died";
                };
            } else {
                deathMessage = this.getNameTag() + " died";
            }

            PlayerDeathEvent playerDeathEvent = new PlayerDeathEvent( this, deathMessage, true, this.getDrops() );
            this.server.getPluginManager().callEvent( playerDeathEvent );

            if ( playerDeathEvent.isDropInventory() ) {
                for ( Item dropItem : playerDeathEvent.getDrops() ) {
                    if ( dropItem != null ) {
                        this.getWorld().dropItem( dropItem, this.location, null ).spawn();
                    }
                }

                this.playerInventory.clear();
                this.cursorInventory.clear();
                this.armorInventory.clear();
            }

            if ( playerDeathEvent.getDeathMessage() != null && !playerDeathEvent.getDeathMessage().isEmpty() ) {
                this.server.broadcastMessage( playerDeathEvent.getDeathMessage() );
            }

            this.respawnLocation = this.getWorld().getSpawnLocation().add( 0, this.getEyeHeight(), 0 );

            RespawnPacket respawnPositionPacket = new RespawnPacket();
            respawnPositionPacket.setRuntimeEntityId( this.entityId );
            respawnPositionPacket.setState( RespawnPacket.State.SERVER_SEARCHING );
            respawnPositionPacket.setPosition( this.respawnLocation.toVector3f() );
            this.playerConnection.sendPacket( respawnPositionPacket );
        }
    }

    public void respawn() {
        if ( this.isDead ) {
            PlayerRespawnEvent playerRespawnEvent = new PlayerRespawnEvent( this, this.respawnLocation );
            this.server.getPluginManager().callEvent( playerRespawnEvent );

            this.lastDamageEntity = null;
            this.lastDamageSource = null;
            this.lastDamage = 0;

            this.updateMetadata();
            for ( Attribute attribute : this.attributes.values() ) {
                attribute.reset();
            }
            this.updateAttributes();

            this.spawn();

            if ( playerRespawnEvent.getRespawnLocation() != null ) {
                this.teleport( playerRespawnEvent.getRespawnLocation() );
            }

            this.respawnLocation = null;

            this.setBurning( false );
            this.setVelocity( Vector.zero() );

            this.playerInventory.sendContents( this );
            this.cursorInventory.sendContents( this );
            this.armorInventory.sendArmorContent( this );

            EntityEventPacket entityEventPacket = new EntityEventPacket();
            entityEventPacket.setRuntimeEntityId( this.entityId );
            entityEventPacket.setType( EntityEventType.RESPAWN );
            this.getWorld().sendDimensionPacket( entityEventPacket, this.dimension );

            this.playerInventory.getItemInHand().addToHand( this );

            this.isDead = false;
        }
    }

    public List<Item> getDrops() {
        List<Item> drops = new ArrayList<>();
        for ( Item content : this.playerInventory.getContents() ) {
            if ( content != null && !( content instanceof ItemAir ) ) {
                drops.add( content );
            }
        }
        for ( Item content : this.cursorInventory.getContents() ) {
            if ( content != null && !( content instanceof ItemAir ) ) {
                drops.add( content );
            }
        }
        for ( Item content : this.armorInventory.getContents() ) {
            if ( content != null && !( content instanceof ItemAir ) ) {
                drops.add( content );
            }
        }
        return drops;
    }

    public EntityFishingHook getEntityFishingHook() {
        return this.entityFishingHook;
    }

    public void setEntityFishingHook( EntityFishingHook entityFishingHook ) {
        this.entityFishingHook = entityFishingHook;
    }

    public void createCraftingTransaction( List<InventoryAction> inventoryTransactions ) {
        this.craftingTransaction = new CraftingTransaction( this, inventoryTransactions );
    }

    public CraftingTransaction getCraftingTransaction() {
        return this.craftingTransaction;
    }

    public void resetCraftingTransaction() {
        this.craftingTransaction = null;
    }

    public void sendServerSettings( Player player ) {
        if ( this.serverSettingsForm != -1 ) {
            Form<?> form = this.forms.get( this.serverSettingsForm );

            ServerSettingsResponsePacket response = new ServerSettingsResponsePacket();
            response.setFormId( this.serverSettingsForm );
            response.setFormData( form.toJSON().toJSONString() );
            player.sendPacket( response );
        }
    }

    public <R> FormListener<R> showForm( Form<R> form ) {
        int formId = this.formId++;
        this.forms.put( formId, form );
        FormListener<R> formListener = new FormListener<R>();
        this.formListeners.put( formId, formListener );

        String json = form.toJSON().toJSONString();
        ModalFormRequestPacket packetModalRequest = new ModalFormRequestPacket();
        packetModalRequest.setFormId( formId );
        packetModalRequest.setFormData( json );
        this.sendPacket( packetModalRequest );
        return formListener;
    }

    public <R> FormListener<R> setSettingsForm( Form<R> form ) {
        if ( this.serverSettingsForm != -1 ) {
            this.removeSettingsForm();
        }

        int formId = this.formId++;
        this.forms.put( formId, form );

        FormListener<R> formListener = new FormListener<R>();
        this.formListeners.put( formId, formListener );
        this.serverSettingsForm = formId;
        return formListener;
    }

    public void removeSettingsForm() {
        if ( this.serverSettingsForm != -1 ) {
            this.forms.remove( this.serverSettingsForm );
            this.formListeners.remove( this.serverSettingsForm );
            this.serverSettingsForm = -1;
        }
    }

    public void parseGUIResponse( int formId, String json ) {
        // Get the listener and the form
        Form<?> form = this.forms.get( formId );
        if ( form != null ) {
            // Get listener
            FormListener formListener = this.formListeners.get( formId );

            if ( this.serverSettingsForm != formId ) {
                this.forms.remove( formId );
                this.formListeners.remove( formId );
            }

            if ( json.equals( "null" ) ) {
                formListener.getCloseConsumer().accept( null );
            } else {
                Object resp = form.parseResponse( json );
                if ( resp == null ) {
                    formListener.getCloseConsumer().accept( null );
                } else {
                    formListener.getResponseConsumer().accept( resp );
                }
            }
        }
    }

    public PlayerConnection getPlayerConnection() {
        return this.playerConnection;
    }

    public void sendPacket( BedrockPacket packet ) {
        this.playerConnection.sendPacket( packet );
    }

    public void sendToast( String title, String content ) {
        ToastRequestPacket packet = new ToastRequestPacket();
        packet.setTitle( title );
        packet.setContent( content );

        this.sendPacket( packet );
    }

    public void addNpcDialogueForm( NpcDialogueForm npcDialogueForm ) {
        this.npcDialogueForms.add( npcDialogueForm );
    }

    public void removeNpcDialogueForm( NpcDialogueForm npcDialogueForm ) {
        this.npcDialogueForms.remove( npcDialogueForm );
    }

    public Set<NpcDialogueForm> getOpenNpcDialogueForms() {
        return new HashSet<>( this.npcDialogueForms );
    }
}
