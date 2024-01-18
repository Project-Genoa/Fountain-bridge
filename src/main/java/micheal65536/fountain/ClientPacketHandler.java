package micheal65536.fountain;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.GenoaInventoryDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.GenoaNetworkTransformPacket;
import org.cloudburstmc.protocol.bedrock.packet.GenoaOpenInventoryPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.jetbrains.annotations.NotNull;

public final class ClientPacketHandler implements BedrockPacketHandler
{
	private final BedrockSession bedrockSession;

	private PlayerSession playerSession = null;

	public ClientPacketHandler(@NotNull BedrockSession bedrockSession)
	{
		this.bedrockSession = bedrockSession;
	}

	@Override
	public PacketSignal handlePacket(BedrockPacket packet)
	{
		PacketSignal packetSignal = BedrockPacketHandler.super.handlePacket(packet);
		if (packetSignal != PacketSignal.HANDLED)
		{
			LogManager.getLogger().debug("Unhandled Bedrock packet " + packet.getClass().getSimpleName());
		}
		return PacketSignal.HANDLED;
	}

	@Override
	public PacketSignal handle(LoginPacket packet)
	{
		this.playerSession = new PlayerSession(this.bedrockSession, packet);
		return PacketSignal.HANDLED;
	}

	@Override
	public PacketSignal handle(MovePlayerPacket packet)
	{
		this.playerSession.sendJavaPacket(new ServerboundMovePlayerPosRotPacket(packet.isOnGround(), packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), packet.getRotation().getY(), packet.getRotation().getX()));
		return PacketSignal.HANDLED;
	}

	@Override
	public PacketSignal handle(GenoaNetworkTransformPacket packet)
	{
		// TODO: what does this do?
		return PacketSignal.HANDLED;
	}

	@Override
	public PacketSignal handle(InventoryTransactionPacket packet)
	{
		if (packet.getTransactionType() == InventoryTransactionType.ITEM_USE)
		{
			this.playerSession.playerInteraction(packet);
			return PacketSignal.HANDLED;
		}
		else
		{
			return PacketSignal.UNHANDLED;
		}
	}

	@Override
	public PacketSignal handle(MobEquipmentPacket packet)
	{
		this.playerSession.updateSelectedHotbarItem(packet);
		return PacketSignal.HANDLED;
	}

	@Override
	public PacketSignal handle(GenoaOpenInventoryPacket packet)
	{
		this.playerSession.sendGenoaInventory();
		return PacketSignal.HANDLED;
	}

	@Override
	public PacketSignal handle(GenoaInventoryDataPacket packet)
	{
		this.playerSession.onGenoaInventoryChange(packet);
		return PacketSignal.HANDLED;
	}

	@Override
	public void onDisconnect(String reason)
	{
		LogManager.getLogger().info("Bedrock client has disconnected");
		this.playerSession.disconnectForced();
	}
}