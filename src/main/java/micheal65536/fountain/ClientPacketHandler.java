package micheal65536.fountain;

import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
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
	public void onDisconnect(String reason)
	{
		LogManager.getLogger().info("Bedrock client has disconnected");
		this.playerSession.disconnectForced();
	}
}