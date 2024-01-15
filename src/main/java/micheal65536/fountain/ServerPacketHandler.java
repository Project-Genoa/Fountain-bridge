package micheal65536.fountain;

import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDelimiterPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchStartPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

public final class ServerPacketHandler extends SessionAdapter
{
	private final PlayerSession playerSession;

	public ServerPacketHandler(@NotNull PlayerSession playerSession)
	{
		this.playerSession = playerSession;
	}

	@Override
	public void packetReceived(Session session, Packet packet)
	{
		if (packet instanceof ClientboundLoginPacket)
		{
			this.playerSession.onJavaLogin((ClientboundLoginPacket) packet);
		}
		else if (packet instanceof ClientboundChangeDifficultyPacket)
		{
			this.playerSession.onJavaDifficultyChanged(((ClientboundChangeDifficultyPacket) packet).getDifficulty());
		}
		else if (packet instanceof ClientboundRegistryDataPacket)
		{
			this.playerSession.loadJavaBiomes(((CompoundTag) ((ClientboundRegistryDataPacket) packet).getRegistry().get("minecraft:worldgen/biome")).get("value"));
		}
		else if (packet instanceof ClientboundDisconnectPacket)
		{
			// empty
		}

		else if (packet instanceof ClientboundDelimiterPacket)
		{
			// empty
		}

		else if (packet instanceof ClientboundSetTimePacket)
		{
			this.playerSession.updateTime(((ClientboundSetTimePacket) packet).getTime());
		}

		else if (packet instanceof ClientboundLevelChunkWithLightPacket)
		{
			this.playerSession.onJavaLevelChunk((ClientboundLevelChunkWithLightPacket) packet);
		}
		else if (packet instanceof ClientboundChunkBatchStartPacket)
		{
			// empty
		}
		else if (packet instanceof ClientboundChunkBatchFinishedPacket)
		{
			this.playerSession.sendJavaPacket(new ServerboundChunkBatchReceivedPacket(20.0f));
		}
		else if (packet instanceof ClientboundBlockUpdatePacket)
		{
			this.playerSession.onJavaBlockUpdate(((ClientboundBlockUpdatePacket) packet).getEntry());
		}
		else if (packet instanceof ClientboundSectionBlocksUpdatePacket)
		{
			for (BlockChangeEntry blockChangeEntry : ((ClientboundSectionBlocksUpdatePacket) packet).getEntries())
			{
				this.playerSession.onJavaBlockUpdate(blockChangeEntry);
			}
		}

		else
		{
			LogManager.getLogger().debug("Unhandled Java packet " + packet.getClass().getSimpleName());
		}
	}

	@Override
	public void disconnected(DisconnectedEvent event)
	{
		this.playerSession.disconnectForced();
	}
}