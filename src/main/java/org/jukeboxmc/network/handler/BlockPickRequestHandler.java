package org.jukeboxmc.network.handler;

import com.nukkitx.protocol.bedrock.packet.BlockPickRequestPacket;
import org.jukeboxmc.Server;
import org.jukeboxmc.block.Block;
import org.jukeboxmc.item.Item;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.Player;

/**
 * @author Kaooot
 * @version 1.0
 */
public class BlockPickRequestHandler implements PacketHandler<BlockPickRequestPacket> {

    @Override
    public void handle( BlockPickRequestPacket packet, Server server, Player player ) {
        Vector blockPosition = new Vector( packet.getBlockPosition() );
        Block block = player.getWorld().getBlock( blockPosition );
        Item item = block.toItem();

        if ( !player.getInventory().contains( item ) ) {
            player.getInventory().setItemInHand( item );
        } else {
            for ( int i = 0; i < player.getInventory().getSize(); i++ ) {
                if ( player.getInventory().getItem( i ).equalsExact( item ) && player.getInventory().getItemInHandSlot() != i ) {
                    player.getInventory().setItemInHandSlot( i );
                    player.sendPacket( player.getInventory().createMobEquipmentPacket( player ) );
                    player.getInventory().sendContents( i, player );
                    break;
                }
            }
        }
    }
}