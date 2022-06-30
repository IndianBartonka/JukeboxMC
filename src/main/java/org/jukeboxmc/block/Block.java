package org.jukeboxmc.block;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.jukeboxmc.Server;
import org.jukeboxmc.block.direction.BlockFace;
import org.jukeboxmc.block.direction.Direction;
import org.jukeboxmc.block.type.UpdateReason;
import org.jukeboxmc.blockentity.BlockEntity;
import org.jukeboxmc.entity.Entity;
import org.jukeboxmc.item.Item;
import org.jukeboxmc.item.type.ItemTierType;
import org.jukeboxmc.item.type.ItemToolType;
import org.jukeboxmc.math.AxisAlignedBB;
import org.jukeboxmc.math.Location;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;
import org.jukeboxmc.world.World;
import org.jukeboxmc.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jukeboxmc.block.BlockType.Companion.update;

/**
 * @author LucGamesYT
 * @version 1.0
 */
@EqualsAndHashCode(exclude = {"location", "layer", "world"})
public abstract class Block implements Cloneable {

    public static final Object2ObjectMap<String, Object2ObjectMap<NbtMap, Integer>> STATES = new Object2ObjectLinkedOpenHashMap<>();

    protected int runtimeId;
    protected String identifier;
    protected NbtMap blockStates;

    protected World world;
    protected Location location;
    protected int layer = 0;

    public Block( String identifier ) {
        this( identifier, null );
    }

    public Block( String identifier, NbtMap blockStates ) {
        this.identifier = identifier.toLowerCase();

        if ( !STATES.containsKey( this.identifier ) ) {
            Object2ObjectMap<NbtMap, Integer> toRuntimeId = new Object2ObjectLinkedOpenHashMap<>();
            for ( NbtMap blockMap : BlockPalette.searchBlocks( blockMap -> blockMap.getString( "name" ).toLowerCase().equals( this.identifier ) ) ) {
                toRuntimeId.put( blockMap.getCompound( "states" ), BlockPalette.getRuntimeId( blockMap ) );
            }
            STATES.put( this.identifier, toRuntimeId );
            for ( NbtMap state : toRuntimeId.keySet() ) {
                try {
                    int runtimeId = toRuntimeId.get( state );
                    Block block = this.getClass().newInstance();
                    block.runtimeId = runtimeId;
                    block.identifier = identifier;
                    block.blockStates = state;
                    BlockPalette.RUNTIME_TO_BLOCK.put( runtimeId, block );
                } catch ( InstantiationException | IllegalAccessException e ) {
                    e.printStackTrace();
                }
            }
        }

        if ( blockStates == null ) {
            List<NbtMap> states = new ArrayList<>( STATES.get( this.identifier ).keySet() );
            blockStates = states.isEmpty() ? NbtMap.EMPTY : states.get( 0 );
        }

        this.blockStates = blockStates;
        this.runtimeId = STATES.get( this.identifier ).get( this.blockStates );
    }

    public <B extends Block> B setState( String state, Object value ) {
        if ( !this.blockStates.containsKey( state ) ) {
            throw new AssertionError( "State " + state + " was not found in block " + this.identifier );
        }
        if ( this.blockStates.get( state ).getClass() != value.getClass() ) {
            throw new AssertionError( "State " + state + " type is not the same for value  " + value );
        }

        boolean valid = this.checkValidity();

        NbtMapBuilder nbtMapBuilder = this.blockStates.toBuilder();
        nbtMapBuilder.put( state, value );
        for ( Map.Entry<NbtMap, Integer> entry : STATES.get( this.identifier ).entrySet() ) {
            NbtMap blockMap = entry.getKey();
            if ( blockMap.equals( nbtMapBuilder ) ) {
                this.blockStates = blockMap;
            }
        }

        this.runtimeId = STATES.get( this.identifier ).get( this.blockStates );
        if ( Server.getInstance().isInitiating() ) {
            update( this.runtimeId, this );
        }
        if ( valid ) {
            this.getWorld().sendBlockUpdate( this );
            this.getChunk().setBlock( this.location, this.layer, this.runtimeId );
        }
        return (B) this;
    }

    public boolean stateExists( String value ) {
        return this.blockStates.containsKey( value );
    }

    public String getStringState( String value ) {
        return this.blockStates.getString( value ).toUpperCase();
    }

    public byte getByteState( String value ) {
        return this.blockStates.getByte( value );
    }

    public int getIntState( String value ) {
        return this.blockStates.getInt( value );
    }

    public boolean placeBlock( Player player, World world, Vector blockPosition, Vector placePosition, Vector clickedPosition, Item itemIndHand, BlockFace blockFace ) {
        if ( this.getType() != BlockType.AIR ) {
            world.setBlock( placePosition, this, 0, player.getDimension(), true );
            return true;
        } else {
            Server.getInstance().getLogger().debug( "Try to place block -> " + this.getName() );
        }
        return false;
    }

    public boolean interact( Player player, Vector blockPosition, Vector clickedPosition, BlockFace blockFace, Item itemInHand ) {
        return false;
    }

    public boolean onBlockBreak( Vector breakPosition ) {
        this.world.setBlock( breakPosition, new BlockAir(), 0 );
        return true;
    }

    public void playBlockBreakSound() {
        this.world.playSound( this.location, SoundEvent.BREAK, this.runtimeId );
    }

    public boolean canBeReplaced( Block block ) {
        return false;
    }

    public boolean canPassThrough() {
        return false;
    }

    public abstract Item toItem();

    public abstract BlockType getType();

    public boolean hasBlockEntity() {
        return false;
    }

    public BlockEntity getBlockEntity() {
        return null;
    }

    public boolean isSolid() {
        return true;
    }

    public boolean isTransparent() {
        return false;
    }

    public double getHardness() {
        return 0;
    }

    public boolean canBreakWithHand() {
        return true;
    }

    public ItemToolType getToolType() {
        return ItemToolType.NONE;
    }

    public ItemTierType getTierType() {
        return ItemTierType.WOODEN;
    }

    public List<Item> getDrops( Item itemInHand ) {
        return this.getDrops( itemInHand, 1 );
    }

    public List<Item> getDrops( Item itemInHand, int amount ) {
        if ( itemInHand == null ) {
            return Collections.singletonList( this.toItem().setAmount( amount ) );
        }
        if ( itemInHand.getTierType().ordinal() >= this.getTierType().ordinal() ) {
            return Collections.singletonList( this.toItem().setAmount( amount ) );
        }
        return Collections.emptyList();
    }

    public long onUpdate( UpdateReason updateReason ) {
        return -1;
    }

    public void enterBlock( Player player ) {
    }

    public void leaveBlock( Player player ) {
    }

    public void onEntityCollision( Entity entity) {

    }

    public int getTickRate() {
        return 10;
    }

    public boolean canBeFlowedInto() {
        return false;
    }

    public void setBlock( Block block ) {
        this.world.setBlock( this.location, block, 0 );
    }

    public void setBlock( Block block, int layer ) {
        this.world.setBlock( this.location, block, layer );
    }

    public AxisAlignedBB getBoundingBox() {
        return new AxisAlignedBB(
                this.location.getX(),
                this.location.getY(),
                this.location.getZ(),
                this.location.getX() + 1,
                this.location.getY() + 1,
                this.location.getZ() + 1
        );
    }

    //========= Block Break =========
    public double getBreakTime( Item item, Player player ) {
        double hardness = this.getHardness();
        if ( hardness == 0 ) {
            return 0;
        }

        BlockType blockType = this.getType();
        boolean correctTool = this.correctTool0( this.getToolType(),
                item.getItemToolType() ) ||
                item.getItemToolType().equals( ItemToolType.SHEARS ) &&
                        ( blockType.equals( BlockType.WEB ) ||
                                blockType.equals( BlockType.OAK_LEAVES ) ||
                                blockType.equals( BlockType.SPRUCE_LEAVES ) ||
                                blockType.equals( BlockType.BIRCH_LEAVES ) ||
                                blockType.equals( BlockType.JUNGLE_LEAVES ) ||
                                blockType.equals( BlockType.ACACIA_LEAVES ) ||
                                blockType.equals( BlockType.DARK_OAK_LEAVES )
                                );
        boolean canBreakWithHand = this.canBreakWithHand();
        ItemToolType itemToolType = item.getItemToolType();
        ItemTierType itemTier = item.getTierType();
        int efficiencyLoreLevel = 0;
        int hasteEffectLevel = 0;
        boolean insideOfWaterWithoutAquaAffinity = false;
        boolean outOfWaterButNotOnGround = !player.isOnGround();
        return breakTime0( item, hardness, correctTool, canBreakWithHand, blockType, itemToolType, itemTier, efficiencyLoreLevel, hasteEffectLevel, insideOfWaterWithoutAquaAffinity, outOfWaterButNotOnGround );
    }

    private double breakTime0( Item item, double blockHardness, boolean correctTool, boolean canHarvestWithHand, BlockType blockType, ItemToolType itemToolType, ItemTierType itemTierType, int efficiencyLoreLevel, int hasteEffectLevel, boolean insideOfWaterWithoutAquaAffinity, boolean outOfWaterButNotOnGround ) {
        double baseTime;
        if ( canHarvest( item ) || canHarvestWithHand ) {
            baseTime = 1.5 * blockHardness;
        } else {
            baseTime = 5.0 * blockHardness;
        }
        double speed = 1.0 / baseTime;
        if ( correctTool ) {
            speed *= this.toolBreakTimeBonus0( itemToolType, itemTierType, blockType );
        }
        speed += this.speedBonusByEfficiencyLore0( efficiencyLoreLevel );
        speed *= this.speedRateByHasteLore0( hasteEffectLevel );
        if ( insideOfWaterWithoutAquaAffinity ) speed *= 0.2;
        if ( outOfWaterButNotOnGround ) speed *= 0.2;
        return 1.0 / speed;
    }

    private double toolBreakTimeBonus0( ItemToolType itemToolType, ItemTierType itemTierType, BlockType blockType ) {
        if ( itemToolType.equals( ItemToolType.SWORD ) ) return blockType.equals( BlockType.WEB ) ? 15.0 : 1.0;
        if ( itemToolType.equals( ItemToolType.SHEARS ) ) {
            if ( blockType.equals( BlockType.WOOL ) ||
                    blockType.equals( BlockType.OAK_LEAVES ) ||
                    blockType.equals( BlockType.SPRUCE_LEAVES ) ||
                    blockType.equals( BlockType.BIRCH_LEAVES ) ||
                    blockType.equals( BlockType.JUNGLE_LEAVES ) ||
                    blockType.equals( BlockType.ACACIA_LEAVES ) ||
                    blockType.equals( BlockType.DARK_OAK_LEAVES ) ) {
                return 5.0;
            } else if ( blockType.equals( BlockType.WEB ) ) {
                return 15.0;
            }
            return 1.0;
        }
        if ( itemToolType.equals( ItemToolType.NONE ) ) return 1.0;
        return switch ( itemTierType ) {
            case WOODEN -> 2.0;
            case STONE -> 4.0;
            case IRON -> 6.0;
            case DIAMOND -> 8.0;
            case NETHERITE -> 9.0;
            case GOLD -> 12.0;
            default -> 1.0;
        };
    }

    private double speedBonusByEfficiencyLore0( int efficiencyLoreLevel ) {
        if ( efficiencyLoreLevel == 0 ) return 0;
        return efficiencyLoreLevel * efficiencyLoreLevel + 1;
    }

    private double speedRateByHasteLore0( int hasteLoreLevel ) {
        return 1.0 + ( 0.2 * hasteLoreLevel );
    }

    public boolean canHarvest( Item item ) {
        return this.getTierType().equals( ItemTierType.NONE ) || this.getToolType().equals( ItemToolType.NONE ) || this.correctTool0( this.getToolType(), item.getItemToolType() ) && item.getTierType().ordinal() >= this.getTierType().ordinal();
    }

    private boolean correctTool0( ItemToolType blockItemToolType, ItemToolType itemToolType ) {
        return ( blockItemToolType.equals( ItemToolType.SWORD ) && itemToolType.equals( ItemToolType.SWORD ) ) ||
                ( blockItemToolType.equals( ItemToolType.SHOVEL ) && itemToolType.equals( ItemToolType.SHOVEL ) ) ||
                ( blockItemToolType.equals( ItemToolType.PICKAXE ) && itemToolType.equals( ItemToolType.PICKAXE ) ) ||
                ( blockItemToolType.equals( ItemToolType.AXE ) && itemToolType.equals( ItemToolType.AXE ) ) ||
                ( blockItemToolType.equals( ItemToolType.HOE ) && itemToolType.equals( ItemToolType.HOE ) ) ||
                ( blockItemToolType.equals( ItemToolType.SHEARS ) && itemToolType.equals( ItemToolType.SHEARS ) ) ||
                blockItemToolType == ItemToolType.NONE;
    }

    //========= Other =========

    public Block getSide( Direction direction, int layer ) {
        return switch ( direction ) {
            case SOUTH -> this.getRelative( Vector.south(), layer );
            case NORTH -> this.getRelative( Vector.north(), layer );
            case EAST -> this.getRelative( Vector.east(), layer );
            case WEST -> this.getRelative( Vector.west(), layer );
        };
    }

    public Block getSide( Direction direction ) {
        return this.getSide( direction, 0 );
    }

    public Block getSide( BlockFace blockFace, int layer ) {
        return switch ( blockFace ) {
            case DOWN -> this.getRelative( Vector.down(), layer );
            case UP -> this.getRelative( Vector.up(), layer );
            case SOUTH -> this.getRelative( Vector.south(), layer );
            case NORTH -> this.getRelative( Vector.north(), layer );
            case EAST -> this.getRelative( Vector.east(), layer );
            case WEST -> this.getRelative( Vector.west(), layer );
        };
    }

    public Block getSide( BlockFace blockFace ) {
        return this.getSide( blockFace, 0 );
    }

    public Block getRelative( Vector position, int layer ) {
        int x = this.location.getBlockX() + position.getBlockX();
        int y = this.location.getBlockY() + position.getBlockY();
        int z = this.location.getBlockZ() + position.getBlockZ();
        return this.world.getBlock( x, y, z, layer, this.location.getDimension() );
    }

    public int getRuntimeId() {
        return this.runtimeId;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public NbtMap getBlockStates() {
        return this.blockStates;
    }

    public World getWorld() {
        return this.world;
    }

    public void setWorld( World world ) {
        this.world = world;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation( Location location ) {
        this.world = location.getWorld();
        this.location = location;
    }

    public int getLayer() {
        return this.layer;
    }

    public void setLayer( int layer ) {
        this.layer = layer;
    }

    public Chunk getChunk() {
        return this.world.getChunk( this.location.getBlockX() >> 4, this.location.getBlockZ() >> 4, this.location.getDimension() );
    }

    public boolean checkValidity() {
        return this.world != null && this.location != null && this.world.getBlockRuntimeId( this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ(), this.layer, this.location.getDimension() ) == this.runtimeId;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    public boolean isWater() {
        return this instanceof BlockWater;
    }

    public final boolean canWaterloggingFlowInto() {
        return this.canBeFlowedInto() || ( this instanceof BlockWaterlogable && ( (BlockWaterlogable) this ).getWaterloggingLevel() > 1 );
    }

    //========= With Packets =========

    public void sendBlockUpdate( Player player ) {
        if ( this.location == null ) {
            return;
        }
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setRuntimeId( this.runtimeId );
        updateBlockPacket.setBlockPosition( this.location.toVector3i() );
        updateBlockPacket.getFlags().addAll( UpdateBlockPacket.FLAG_ALL_PRIORITY );
        updateBlockPacket.setDataLayer( this.layer );
        player.sendPacket( updateBlockPacket );
    }

    @Override
    public String toString() {
        return "Block{" +
                "runtimeId=" + this.runtimeId +
                ", identifier='" + this.identifier + '\'' +
                ", blockStates=" + this.blockStates.toString() +
                ", world=" + this.world +
                ", position=" + this.location +
                ", layer=" + this.layer +
                '}';
    }

    @Override
    @SneakyThrows
    public Block clone() {
        Block block = (Block) super.clone();
        block.identifier = this.identifier;
        block.runtimeId = this.runtimeId;
        block.layer = this.layer;
        block.blockStates = this.blockStates;
        return block;
    }

}
