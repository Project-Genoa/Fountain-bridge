package micheal65536.fountain;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundPingPacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundPongPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDelimiterPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundAnimatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundDamageEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityMotionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTakeItemEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundBlockChangedAckPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockDestructionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchStartPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLightUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
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
		else if (packet instanceof ClientboundChangeDifficultyPacket)
		{
			this.playerSession.onJavaDifficultyChanged(((ClientboundChangeDifficultyPacket) packet).getDifficulty());
		}
		else if (packet instanceof ClientboundGameEventPacket && ((ClientboundGameEventPacket) packet).getNotification() == GameEvent.CHANGE_GAMEMODE)
		{
			this.playerSession.onJavaGameModeChanged((GameMode) ((ClientboundGameEventPacket) packet).getValue());
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
		else if (packet instanceof ClientboundLightUpdatePacket)
		{
			// empty
		}
		else if (packet instanceof ClientboundForgetLevelChunkPacket)
		{
			// empty
		}

		else if (packet instanceof ClientboundBlockDestructionPacket)
		{
			// TODO: figure out block damage stuff for GenoaUpdateBlockPacket
		}

		else if (packet instanceof ClientboundPlayerPositionPacket)
		{
			this.playerSession.sendJavaPacket(new ServerboundAcceptTeleportationPacket(((ClientboundPlayerPositionPacket) packet).getTeleportId()));
		}
		else if (packet instanceof ClientboundBlockChangedAckPacket)
		{
			// empty
		}
		else if (packet instanceof ClientboundContainerSetContentPacket)
		{
			this.playerSession.onJavaContainerSetContent((ClientboundContainerSetContentPacket) packet);
		}
		else if (packet instanceof ClientboundContainerSetSlotPacket)
		{
			this.playerSession.onJavaContainerSetSlot((ClientboundContainerSetSlotPacket) packet);
		}
		else if (packet instanceof ClientboundSetCarriedItemPacket)
		{
			this.playerSession.onJavaSetCarriedItem(((ClientboundSetCarriedItemPacket) packet).getSlot());
		}
		else if (packet instanceof ClientboundCustomPayloadPacket && ((ClientboundCustomPayloadPacket) packet).getChannel().equals("fountain:item_particle"))
		{
			this.playerSession.onJavaItemPickupParticle(((ClientboundCustomPayloadPacket) packet).getData());
		}

		else if (packet instanceof ClientboundAddEntityPacket)
		{
			this.playerSession.onJavaEntityAdd((ClientboundAddEntityPacket) packet);
		}
		else if (packet instanceof ClientboundRemoveEntitiesPacket)
		{
			for (int entityInstanceId : ((ClientboundRemoveEntitiesPacket) packet).getEntityIds())
			{
				this.playerSession.onJavaEntityRemove(entityInstanceId);
			}
		}
		else if (packet instanceof ClientboundMoveEntityPosPacket || packet instanceof ClientboundMoveEntityRotPacket || packet instanceof ClientboundMoveEntityPosRotPacket || packet instanceof ClientboundTeleportEntityPacket)
		{
			int entityId;
			Vector3f pos;
			Vector2f rot;
			boolean onGround;
			if (packet instanceof ClientboundMoveEntityPosPacket)
			{
				entityId = ((ClientboundMoveEntityPosPacket) packet).getEntityId();
				pos = Vector3f.from(((ClientboundMoveEntityPosPacket) packet).getMoveX(), ((ClientboundMoveEntityPosPacket) packet).getMoveY(), ((ClientboundMoveEntityPosPacket) packet).getMoveZ());
				rot = null;
				onGround = ((ClientboundMoveEntityPosPacket) packet).isOnGround();
			}
			else if (packet instanceof ClientboundMoveEntityRotPacket)
			{
				entityId = ((ClientboundMoveEntityRotPacket) packet).getEntityId();
				pos = null;
				rot = Vector2f.from(((ClientboundMoveEntityRotPacket) packet).getPitch(), ((ClientboundMoveEntityRotPacket) packet).getYaw());
				onGround = ((ClientboundMoveEntityRotPacket) packet).isOnGround();
			}
			else if (packet instanceof ClientboundMoveEntityPosRotPacket)
			{
				entityId = ((ClientboundMoveEntityPosRotPacket) packet).getEntityId();
				pos = Vector3f.from(((ClientboundMoveEntityPosRotPacket) packet).getMoveX(), ((ClientboundMoveEntityPosRotPacket) packet).getMoveY(), ((ClientboundMoveEntityPosRotPacket) packet).getMoveZ());
				rot = Vector2f.from(((ClientboundMoveEntityPosRotPacket) packet).getPitch(), ((ClientboundMoveEntityPosRotPacket) packet).getYaw());
				onGround = ((ClientboundMoveEntityPosRotPacket) packet).isOnGround();
			}
			else if (packet instanceof ClientboundTeleportEntityPacket)
			{
				entityId = ((ClientboundTeleportEntityPacket) packet).getEntityId();
				pos = Vector3f.from(((ClientboundTeleportEntityPacket) packet).getX(), ((ClientboundTeleportEntityPacket) packet).getY(), ((ClientboundTeleportEntityPacket) packet).getZ());
				rot = Vector2f.from(((ClientboundTeleportEntityPacket) packet).getPitch(), ((ClientboundTeleportEntityPacket) packet).getYaw());
				onGround = ((ClientboundTeleportEntityPacket) packet).isOnGround();
			}
			else
			{
				assert false;
				return;
			}
			this.playerSession.onJavaEntityMove(entityId, pos, rot, onGround, !(packet instanceof ClientboundTeleportEntityPacket));
		}
		else if (packet instanceof ClientboundSetEntityMotionPacket)
		{
			this.playerSession.onJavaEntitySetVelocity(((ClientboundSetEntityMotionPacket) packet).getEntityId(), Vector3f.from(((ClientboundSetEntityMotionPacket) packet).getMotionX(), ((ClientboundSetEntityMotionPacket) packet).getMotionY(), ((ClientboundSetEntityMotionPacket) packet).getMotionZ()));
		}
		else if (packet instanceof ClientboundRotateHeadPacket)
		{
			this.playerSession.onJavaEntityRotateHead(((ClientboundRotateHeadPacket) packet).getEntityId(), ((ClientboundRotateHeadPacket) packet).getHeadYaw());
		}
		else if (packet instanceof ClientboundSetEntityDataPacket)
		{
			this.playerSession.onJavaEntityUpdateData(((ClientboundSetEntityDataPacket) packet).getEntityId(), ((ClientboundSetEntityDataPacket) packet).getMetadata());
		}
		else if (packet instanceof ClientboundUpdateAttributesPacket)
		{
			this.playerSession.onJavaEntityUpdateAttributes(((ClientboundUpdateAttributesPacket) packet).getEntityId(), ((ClientboundUpdateAttributesPacket) packet).getAttributes());
		}
		else if (packet instanceof ClientboundSetEquipmentPacket)
		{
			this.playerSession.onJavaEntitySetEquipment(((ClientboundSetEquipmentPacket) packet).getEntityId(), ((ClientboundSetEquipmentPacket) packet).getEquipment());
		}
		else if (packet instanceof ClientboundEntityEventPacket)
		{
			this.playerSession.onJavaEntityEvent(((ClientboundEntityEventPacket) packet).getEntityId(), ((ClientboundEntityEventPacket) packet).getEvent());
		}
		else if (packet instanceof ClientboundAnimatePacket)
		{
			this.playerSession.onJavaEntityAnimation(((ClientboundAnimatePacket) packet).getEntityId(), ((ClientboundAnimatePacket) packet).getAnimation());
		}
		else if (packet instanceof ClientboundDamageEventPacket)
		{
			this.playerSession.onJavaEntityHurt(((ClientboundDamageEventPacket) packet).getEntityId());
		}
		else if (packet instanceof ClientboundTakeItemEntityPacket)
		{
			this.playerSession.onJavaEntityTaken((ClientboundTakeItemEntityPacket) packet);
		}

		else if (packet instanceof ClientboundPingPacket)
		{
			this.playerSession.sendJavaPacket(new ServerboundPongPacket(((ClientboundPingPacket) packet).getId()));
		}

		else
		{
			LogManager.getLogger().debug("Unhandled Java packet " + packet.getClass().getSimpleName());
		}
	}

	@Override
	public void disconnected(DisconnectedEvent event)
	{
		LogManager.getLogger().info("Java server has disconnected");
		this.playerSession.disconnectForced();
	}
}