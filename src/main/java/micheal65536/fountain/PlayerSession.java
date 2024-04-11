package micheal65536.fountain;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.Animation;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.data.game.level.event.LevelEvent;
import com.github.steveice10.mc.protocol.data.game.level.event.LevelEventData;
import com.github.steveice10.mc.protocol.data.game.level.event.LevelEventType;
import com.github.steveice10.mc.protocol.data.game.level.particle.Particle;
import com.github.steveice10.mc.protocol.data.game.level.sound.Sound;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTakeItemEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.math.vector.Vector4f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.GamePublishSetting;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.AvailableEntityIdentifiersPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.cloudburstmc.protocol.bedrock.packet.GenoaGameplaySettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.GenoaInventoryDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.GenoaItemParticlePacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDifficultyPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.connector.PlayerConnectorPluginWrapper;
import micheal65536.fountain.connector.plugin.ConnectorPlugin;
import micheal65536.fountain.connector.plugin.DisconnectResponse;
import micheal65536.fountain.connector.plugin.Inventory;
import micheal65536.fountain.registry.EarthEntitiesRegistry;
import micheal65536.fountain.registry.JavaItems;
import micheal65536.fountain.utils.ChunkManager;
import micheal65536.fountain.utils.EffectManager;
import micheal65536.fountain.utils.EntityManager;
import micheal65536.fountain.utils.EntityTranslator;
import micheal65536.fountain.utils.FabricRegistryManager;
import micheal65536.fountain.utils.InventoryManager;
import micheal65536.fountain.utils.ItemTranslator;
import micheal65536.fountain.utils.entities.ItemJavaEntityInstance;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class PlayerSession
{
	private static final int MAX_NONEMPTY_CHUNK_RADIUS = 2;
	private static final int HARDCODED_CHUNK_RADIUS = 20;
	private static final Vector3i HARDCODED_CHUNK_CENTER = Vector3i.from(0, 128, 0);

	private static final int JAVA_MAIN_INVENTORY_OFFSET = 9;
	private static final int JAVA_HOTBAR_OFFSET = JAVA_MAIN_INVENTORY_OFFSET + 27;

	private final FabricRegistryManager fabricRegistryManager;
	private final InventoryManager inventoryManager;
	private final ChunkManager chunkManager;
	private final EntityManager entityManager;
	private final EffectManager effectManager;

	private final Thread tickThread;

	private final BedrockSession bedrock;
	private long bedrockPlayerEntityId;
	private boolean bedrockDoDaylightCycle = true;
	private int bedrockSelectedHotbarSlot = 0;

	private final TcpClientSession java;
	private int javaPlayerEntityId;
	private final HashMap<Integer, String> javaBiomes = new HashMap<>();
	private float javaPlayerHealth = 20.0f;

	private final PlayerConnectorPluginWrapper playerConnectorPluginWrapper;
	private final Consumer<PlayerSession> disconnectCallback;
	private boolean disconnected = false;

	final ReentrantLock mutex = new ReentrantLock(true);

	public PlayerSession(@NotNull BedrockSession bedrockSession, @NotNull TcpClientSession javaSession, @NotNull Inventory initialInventory, @NotNull PlayerConnectorPluginWrapper playerConnectorPluginWrapper, @NotNull Consumer<PlayerSession> disconnectCallback)
	{
		this.bedrock = bedrockSession;
		this.java = javaSession;
		this.playerConnectorPluginWrapper = playerConnectorPluginWrapper;
		this.disconnectCallback = disconnectCallback;

		this.fabricRegistryManager = new FabricRegistryManager(this, (MinecraftCodecHelper) this.java.getCodecHelper());
		this.inventoryManager = new InventoryManager(this, (MinecraftCodecHelper) this.java.getCodecHelper(), initialInventory, this.playerConnectorPluginWrapper);
		this.chunkManager = new ChunkManager(MAX_NONEMPTY_CHUNK_RADIUS, this, this.fabricRegistryManager);
		this.entityManager = new EntityManager(this);
		this.effectManager = new EffectManager(this);

		this.tickThread = new Thread(() ->
		{
			final long tickInterval = 50;
			long nextTickTime = System.nanoTime() / 1000000l + tickInterval;
			while (!Thread.interrupted())
			{
				long sleepTime = nextTickTime - System.nanoTime() / 1000000l;
				if (sleepTime > 0)
				{
					try
					{
						Thread.sleep(sleepTime);
					}
					catch (InterruptedException exception)
					{
						break;
					}
				}

				this.tick();

				nextTickTime += tickInterval;
			}
		});
		this.tickThread.start();
	}

	public void disconnect(boolean fromServer)
	{
		if (this.disconnected)
		{
			return;
		}
		this.disconnected = true;

		this.tickThread.interrupt();
		while (this.tickThread.isAlive())
		{
			try
			{
				this.tickThread.join();
			}
			catch (InterruptedException exception)
			{
				// empty
			}
		}

		DisconnectResponse disconnectResponse;
		try
		{
			disconnectResponse = this.playerConnectorPluginWrapper.onPlayerDisconnected(this.inventoryManager.getInventoryForConnectorPlugin());
		}
		catch (ConnectorPlugin.ConnectorPluginException exception)
		{
			LogManager.getLogger().error("Connector plugin threw exception when handling player disconnect", exception);
			disconnectResponse = null;
		}

		if (this.bedrock.isConnected())
		{
			if (fromServer)
			{
				// TODO: send Genoa disconnect packets
			}
			else
			{
				LogManager.getLogger().debug("Session disconnect with Bedrock session still connected and fromServer = false???");
			}

			this.bedrock.disconnect();
		}

		this.java.disconnect("");

		this.disconnectCallback.accept(this);
	}

	private void tick()
	{
		this.chunkManager.tick();
	}

	public void onJavaLogin(@NotNull ClientboundLoginPacket clientboundLoginPacket)
	{
		this.javaPlayerEntityId = clientboundLoginPacket.getEntityId();
		this.bedrockPlayerEntityId = this.entityManager.registerLocalPlayerEntity(this.javaPlayerEntityId);

		PlayStatusPacket playStatusPacket = new PlayStatusPacket();
		playStatusPacket.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
		this.sendBedrockPacket(playStatusPacket);

		StartGamePacket startGamePacket = new StartGamePacket();
		//startGamePacket.getGamerules().add(new GameRuleData<>("dodaylightcycle", false));
		startGamePacket.setUniqueEntityId(this.bedrockPlayerEntityId);
		startGamePacket.setRuntimeEntityId(this.bedrockPlayerEntityId);
		startGamePacket.setPlayerGameType(GameType.valueOf(clientboundLoginPacket.getCommonPlayerSpawnInfo().getGameMode().name()));
		startGamePacket.setPlayerPosition(Vector3f.from(0.0f, 69.0f, 0.0f));
		startGamePacket.setRotation(Vector2f.from(1.0f, 1.0f));
		startGamePacket.setSeed(0);
		startGamePacket.setDimensionId(0);
		startGamePacket.setGeneratorId(1);
		startGamePacket.setLevelGameType(GameType.valueOf(clientboundLoginPacket.getCommonPlayerSpawnInfo().getGameMode().name()));
		startGamePacket.setDifficulty(1);
		startGamePacket.setDefaultSpawn(Vector3i.ZERO);
		startGamePacket.setAchievementsDisabled(true);
		//startGamePacket.setDayCycleStopTime(6000);    // TODO: set according to buildplate day/night mode
		startGamePacket.setDayCycleStopTime(0);
		startGamePacket.setEduEditionOffers(0);
		startGamePacket.setEduFeaturesEnabled(false);
		startGamePacket.setRainLevel(0.0f);
		startGamePacket.setLightningLevel(0.0f);
		startGamePacket.setPlatformLockedContentConfirmed(false);
		startGamePacket.setMultiplayerGame(true);
		startGamePacket.setBroadcastingToLan(true);
		startGamePacket.setXblBroadcastMode(GamePublishSetting.PUBLIC);
		startGamePacket.setPlatformBroadcastMode(GamePublishSetting.PUBLIC);
		startGamePacket.setCommandsEnabled(true);
		startGamePacket.setTexturePacksRequired(false);
		startGamePacket.setBonusChestEnabled(false);
		startGamePacket.setStartingWithMap(false);
		startGamePacket.setTrustingPlayers(true);
		startGamePacket.setDefaultPlayerPermission(PlayerPermission.MEMBER);
		startGamePacket.setServerChunkTickRange(clientboundLoginPacket.getSimulationDistance());
		startGamePacket.setBehaviorPackLocked(false);
		startGamePacket.setResourcePackLocked(false);
		startGamePacket.setFromWorldTemplate(false);
		startGamePacket.setUsingMsaGamertagsOnly(false);
		startGamePacket.setFromWorldTemplate(false);
		startGamePacket.setWorldTemplateOptionLocked(false);
		startGamePacket.setLevelId("");
		startGamePacket.setLevelName("");
		startGamePacket.setPremiumWorldTemplateId("");
		startGamePacket.setTrial(false);
		startGamePacket.setCurrentTick(0); // TODO
		startGamePacket.setEnchantmentSeed(0);
		startGamePacket.setMultiplayerCorrelationId("");
		this.sendBedrockPacket(startGamePacket);

		GenoaGameplaySettingsPacket genoaGameplaySettingsPacket = new GenoaGameplaySettingsPacket();
		genoaGameplaySettingsPacket.setMultiplePlayersOnline(false);
		genoaGameplaySettingsPacket.setOwnerRuntimeId(this.bedrockPlayerEntityId);
		this.sendBedrockPacket(genoaGameplaySettingsPacket);

		AvailableEntityIdentifiersPacket availableEntityIdentifiersPacket = new AvailableEntityIdentifiersPacket();
		availableEntityIdentifiersPacket.setIdentifiers(NbtMap.builder().putList("idlist", NbtType.COMPOUND, Arrays.stream(EarthEntitiesRegistry.getEntities()).map(entityInfo -> NbtMap.builder()
				.putInt("rid", entityInfo.rid)
				.putString("id", entityInfo.id)
				.putString("bid", entityInfo.bid)
				.putBoolean("summonable", entityInfo.summonable)
				.putBoolean("hasspawnegg", entityInfo.hasSpawnEgg)
				.putBoolean("experimental", false)
				.build()
		).toList()).build());
		this.sendBedrockPacket(availableEntityIdentifiersPacket);

		playStatusPacket = new PlayStatusPacket();
		playStatusPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
		this.sendBedrockPacket(playStatusPacket);

		ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
		chunkRadiusUpdatedPacket.setRadius(HARDCODED_CHUNK_RADIUS);
		this.sendBedrockPacket(chunkRadiusUpdatedPacket);

		NetworkChunkPublisherUpdatePacket networkChunkPublisherUpdatePacket = new NetworkChunkPublisherUpdatePacket();
		networkChunkPublisherUpdatePacket.setPosition(HARDCODED_CHUNK_CENTER);
		networkChunkPublisherUpdatePacket.setRadius(HARDCODED_CHUNK_RADIUS * 16);
		this.sendBedrockPacket(networkChunkPublisherUpdatePacket);

		for (int chunkX = -HARDCODED_CHUNK_RADIUS; chunkX < HARDCODED_CHUNK_RADIUS; chunkX++)
		{
			for (int chunkZ = -HARDCODED_CHUNK_RADIUS; chunkZ < HARDCODED_CHUNK_RADIUS; chunkZ++)
			{
				LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
				levelChunkPacket.setChunkX(chunkX);
				levelChunkPacket.setChunkZ(chunkZ);
				levelChunkPacket.setData(Unpooled.wrappedBuffer(new byte[0]));
				this.sendBedrockPacket(levelChunkPacket);
			}
		}

		ServerboundClientInformationPacket serverboundClientInformationPacket = new ServerboundClientInformationPacket("en_GB", HARDCODED_CHUNK_RADIUS, ChatVisibility.FULL, true, Arrays.asList(SkinPart.values()), HandPreference.RIGHT_HAND, false, true);
		this.sendJavaPacket(serverboundClientInformationPacket);

		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
		byteBuf.writeBoolean(true);
		byte[] data = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(data);
		ServerboundCustomPayloadPacket serverboundCustomPayloadPacket = new ServerboundCustomPayloadPacket("fountain:earth_mode", data);
		this.sendJavaPacket(serverboundCustomPayloadPacket);

		this.inventoryManager.initialiseServerInventory();

		ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket = new ServerboundSetCarriedItemPacket(this.bedrockSelectedHotbarSlot);
		this.sendJavaPacket(serverboundSetCarriedItemPacket);
	}

	public void onJavaChannelRegister(@NotNull String channel)
	{
		if (channel.equals("fabric:registry/sync/complete"))
		{
			this.fabricRegistryManager.enableRegistrySyncChannel();
		}
	}

	public void updateTime(long javaTime)
	{
		int bedrockTime = (int) (Math.abs(javaTime) % (24000 * 8));
		boolean doDaylightCycle = javaTime >= 0;

		if (doDaylightCycle != this.bedrockDoDaylightCycle)
		{
			GameRulesChangedPacket gameRulesChangedPacket = new GameRulesChangedPacket();
			gameRulesChangedPacket.getGameRules().add(new GameRuleData<>("dodaylightcycle", doDaylightCycle));
			this.sendBedrockPacket(gameRulesChangedPacket);

			this.bedrockDoDaylightCycle = doDaylightCycle;
		}

		SetTimePacket setTimePacket = new SetTimePacket();
		setTimePacket.setTime(bedrockTime);
		this.sendBedrockPacket(setTimePacket);
	}

	public void onJavaDifficultyChanged(@NotNull Difficulty difficulty)
	{
		SetDifficultyPacket setDifficultyPacket = new SetDifficultyPacket();
		setDifficultyPacket.setDifficulty(difficulty.ordinal());
		this.sendBedrockPacket(setDifficultyPacket);
	}

	public void onJavaGameModeChanged(@NotNull GameMode gameMode)
	{
		SetPlayerGameTypePacket setPlayerGameTypePacket = new SetPlayerGameTypePacket();
		setPlayerGameTypePacket.setGamemode(GameType.valueOf(gameMode.name()).ordinal());
		this.sendBedrockPacket(setPlayerGameTypePacket);
	}

	public void loadJavaBiomes(ListTag biomesListTag)
	{
		this.javaBiomes.clear();
		for (Tag tag : biomesListTag)
		{
			CompoundTag compoundTag = (CompoundTag) tag;
			int id = ((IntTag) compoundTag.get("id")).getValue();
			String name = ((StringTag) compoundTag.get("name")).getValue();
			this.javaBiomes.put(id, name);
		}
	}

	public void handleFabricRegistrySyncData(byte[] data)
	{
		this.fabricRegistryManager.handleRegistrySyncData(data);
	}

	public void onJavaLevelChunk(@NotNull ClientboundLevelChunkWithLightPacket clientboundLevelChunkWithLightPacket)
	{
		this.chunkManager.onJavaLevelChunk(clientboundLevelChunkWithLightPacket, this.javaBiomes, (MinecraftCodecHelper) this.java.getCodecHelper());
	}

	public void onJavaBlockUpdate(@NotNull BlockChangeEntry blockChangeEntry)
	{
		this.chunkManager.onJavaBlockUpdate(blockChangeEntry);
	}

	public void onJavaBlockEntityUpdate(@NotNull ClientboundBlockEntityDataPacket clientboundBlockEntityDataPacket)
	{
		this.chunkManager.onJavaBlockEntityUpdate(clientboundBlockEntityDataPacket);
	}

	public void onJavaBlockEvent(@NotNull ClientboundBlockEventPacket clientboundBlockEventPacket)
	{
		this.chunkManager.onJavaBlockEvent(clientboundBlockEventPacket);
	}

	public void onJavaEntityAdd(@NotNull ClientboundAddEntityCustomPacket clientboundAddEntityPacket)
	{
		EntityManager.JavaEntityInstance entityInstance;
		if (clientboundAddEntityPacket.getType() != null)
		{
			entityInstance = EntityTranslator.createEntityInstance(clientboundAddEntityPacket.getType(), clientboundAddEntityPacket.getData());
			if (entityInstance == null)
			{
				LogManager.getLogger().warn("Ignoring Java entity with type {}", clientboundAddEntityPacket.getType().name());
				return;
			}
		}
		else
		{
			String name = this.fabricRegistryManager.getEntityName(clientboundAddEntityPacket.typeId);
			if (name == null)
			{
				LogManager.getLogger().warn("Add entity packet with unknown type ID");
				return;
			}
			entityInstance = EntityTranslator.createEntityInstance(name, clientboundAddEntityPacket.getData());
			if (entityInstance == null)
			{
				LogManager.getLogger().warn("Ignoring Java entity with type {}", name);
				return;
			}
		}

		entityInstance.setInitialPosition(
				Vector3f.from(clientboundAddEntityPacket.getX(), clientboundAddEntityPacket.getY(), clientboundAddEntityPacket.getZ()),
				Vector3f.from(clientboundAddEntityPacket.getMotionX(), clientboundAddEntityPacket.getMotionY(), clientboundAddEntityPacket.getMotionZ()),
				true,
				clientboundAddEntityPacket.getYaw(),
				clientboundAddEntityPacket.getPitch(),
				clientboundAddEntityPacket.getHeadYaw()
		);
		this.entityManager.registerJavaEntity(clientboundAddEntityPacket.getEntityId(), entityInstance);
	}

	public void onJavaEntityRemove(int javaEntityInstanceId)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			LogManager.getLogger().debug("Ignoring entity remove for local player entity");
			return;
		}
		EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
		if (entityInstance != null)
		{
			entityInstance.remove();
		}
	}

	public void onJavaEntityMove(int javaEntityInstanceId, @Nullable Vector3f pos, @Nullable Vector2f rot, boolean onGround, boolean relative)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			LogManager.getLogger().debug("Ignoring entity move for local player entity");
			return;
		}
		EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
		if (entityInstance != null)
		{
			Vector3f newPos;
			if (pos != null)
			{
				newPos = relative ? entityInstance.getPos().add(pos) : pos;
			}
			else
			{
				newPos = entityInstance.getPos();
			}

			float newYaw;
			float newPitch;
			if (rot != null)
			{
				newYaw = rot.getY();
				newPitch = rot.getX();
			}
			else
			{
				newYaw = entityInstance.getYaw();
				newPitch = entityInstance.getPitch();
			}

			entityInstance.setPosition(newPos, onGround, newYaw, newPitch);
		}
	}

	public void onJavaEntitySetVelocity(int javaEntityInstanceId, @NotNull Vector3f velocity)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			LogManager.getLogger().debug("Ignoring entity set velocity for local player entity");
			return;
		}
		EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
		if (entityInstance != null)
		{
			entityInstance.setVelocity(velocity);
		}
	}

	public void onJavaEntityRotateHead(int javaEntityInstanceId, float headYaw)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			LogManager.getLogger().debug("Ignoring entity rotate head for local player entity");
			return;
		}
		EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
		if (entityInstance != null)
		{
			entityInstance.setHeadYaw(headYaw);
		}
	}

	public void onJavaEntitySetEquipment(int javaEntityInstanceId, Equipment[] equipments)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			LogManager.getLogger().debug("Ignoring entity set equipment for local player entity");
			return;
		}
		EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
		if (entityInstance != null)
		{
			ItemData handMain = entityInstance.getHandMain();
			ItemData handSecondary = entityInstance.getHandSecondary();
			ItemData armorHead = entityInstance.getArmorHead();
			ItemData armorChest = entityInstance.getArmorChest();
			ItemData armorLegs = entityInstance.getArmorLegs();
			ItemData armorFeet = entityInstance.getArmorFeet();
			for (Equipment equipment : equipments)
			{
				ItemData itemData = ItemTranslator.translateJavaToBedrock(equipment.getItem());
				switch (equipment.getSlot())
				{
					case MAIN_HAND -> handMain = itemData;
					case OFF_HAND -> handSecondary = itemData;
					case HELMET -> armorHead = itemData;
					case CHESTPLATE -> armorChest = itemData;
					case LEGGINGS -> armorLegs = itemData;
					case BOOTS -> armorFeet = itemData;
				}
			}
			entityInstance.setEquipment(handMain, handSecondary, armorHead, armorChest, armorLegs, armorFeet);
		}
	}

	public void onJavaEntityUpdateData(int javaEntityInstanceId, @NotNull EntityMetadata<?, ?>[] entityMetadata)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			Arrays.stream(entityMetadata).filter(metadata -> metadata.getId() == 9).findAny().ifPresent(metadata ->
			{
				if (metadata.getType() != MetadataType.FLOAT)
				{
					LogManager.getLogger().warn("Server sent bad player entity metadata");
					return;
				}
				this.javaPlayerHealth = (float) metadata.getValue();

				UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
				updateAttributesPacket.setRuntimeEntityId(this.bedrockPlayerEntityId);
				updateAttributesPacket.getAttributes().add(new AttributeData("minecraft:health", 0.0f, 20.0f, this.javaPlayerHealth > 0.0f && this.javaPlayerHealth < 1.0f ? 1.0f : this.javaPlayerHealth));
				this.sendBedrockPacket(updateAttributesPacket);

				if (this.javaPlayerHealth == 0.0f)
				{
					this.handlePlayerDead();
				}
			});
		}
		else
		{
			EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
			if (entityInstance != null)
			{
				entityInstance.metadataChanged(Arrays.copyOf(entityMetadata, entityMetadata.length));
			}
		}
	}

	public void onJavaEntityUpdateAttributes(int javaEntityInstanceId, @NotNull List<Attribute> attributes)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			LogManager.getLogger().debug("Ignoring entity update attributes for local player entity");
			return;
		}
		EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
		if (entityInstance != null)
		{
			entityInstance.attributesChanged(attributes.toArray(new Attribute[0]));
		}
	}

	public void onJavaEntityHurt(int javaEntityInstanceId)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			LogManager.getLogger().debug("Ignoring entity hurt for local player entity");
			return;
		}
		EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
		if (entityInstance != null)
		{
			entityInstance.hurt();
		}
	}

	public void onJavaEntityEvent(int javaEntityInstanceId, @NotNull EntityEvent entityEvent)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			LogManager.getLogger().debug("Ignoring entity event for local player entity");
			return;
		}
		EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
		if (entityInstance != null)
		{
			if (!entityInstance.handleEvent(entityEvent))
			{
				LogManager.getLogger().debug("Entity event with type {} ignored by entity ID {}", entityEvent.name(), javaEntityInstanceId);
			}
		}
	}

	public void onJavaEntityAnimation(int javaEntityInstanceId, @NotNull Animation animation)
	{
		if (javaEntityInstanceId == this.javaPlayerEntityId)
		{
			LogManager.getLogger().debug("Ignoring entity animation for local player entity");
			return;
		}
		EntityManager.JavaEntityInstance entityInstance = this.entityManager.getJavaEntity(javaEntityInstanceId);
		if (entityInstance != null)
		{
			if (!entityInstance.handleAnimation(animation))
			{
				LogManager.getLogger().debug("Entity animation with type {} ignored by entity ID {}", animation.name(), javaEntityInstanceId);
			}
		}
	}

	public void onJavaEntityTaken(@NotNull ClientboundTakeItemEntityPacket clientboundTakeItemEntityPacket)
	{
		int collectorJavaEntityId = clientboundTakeItemEntityPacket.getCollectorEntityId();
		int collectedJavaEntityId = clientboundTakeItemEntityPacket.getCollectedEntityId();
		EntityManager.JavaEntityInstance collectedJavaEntityInstance = this.entityManager.getJavaEntity(collectedJavaEntityId);
		if (collectedJavaEntityInstance == null)
		{
			LogManager.getLogger().warn("Server sent entity taken for entity that does not exist");
			return;
		}
		if (!(collectedJavaEntityInstance instanceof ItemJavaEntityInstance))
		{
			LogManager.getLogger().debug("Ignoring entity taken for non-item entity");
			return;
		}
		if (collectorJavaEntityId == this.javaPlayerEntityId)    // TODO: handle items taken by other players
		{
			ItemData item = ((ItemJavaEntityInstance) collectedJavaEntityInstance).getItem();
			if (item != null)
			{
				this.sendItemParticle(item.getDefinition().getRuntimeId(), item.getDamage(), collectedJavaEntityInstance.getPos(), this.bedrockPlayerEntityId);
			}
		}
		else
		{
			LogManager.getLogger().debug("Ignoring entity taken by non-player entity");
		}
	}

	public void onJavaLevelEvent(@NotNull Vector3i position, @NotNull LevelEvent event, @NotNull LevelEventData eventData)
	{
		if (!this.effectManager.handleLevelEvent(position, event, eventData))
		{
			LogManager.getLogger().debug("Unhandled level event {}", event instanceof LevelEventType ? ((LevelEventType) event).name() : event.getId());
		}
	}

	public void onJavaParticleEvent(@NotNull Particle particle, @NotNull Vector3f position, @NotNull Vector4f offset, int amount, boolean longDistance)
	{
		if (!this.effectManager.handleParticleEvent(particle, position, offset, amount, longDistance))
		{
			LogManager.getLogger().debug("Unhandled particle event {}", particle.getType().name());
		}
	}

	public void onJavaSoundEvent(@NotNull Sound sound, @NotNull Vector3f position, float pitch, float volume)
	{
		if (!this.effectManager.handleSoundEvent(sound, position, pitch, volume))
		{
			LogManager.getLogger().debug("Unhandled sound event {}", sound.getName());
		}
	}

	public void clientPlayerAnimation(@NotNull AnimatePacket animatePacket)
	{
		if (animatePacket.getRuntimeEntityId() != this.bedrockPlayerEntityId)
		{
			LogManager.getLogger().warn("Unrecognised client AnimatePacket (entity ID does not match player)");
			return;
		}

		if (animatePacket.getAction() == AnimatePacket.Action.SWING_ARM)
		{
			// TODO: breaks attack cooldown
			LogManager.getLogger().debug("Ignoring SWING_ARM AnimatePacket because it breaks the attack cooldown");
			//this.sendJavaPacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
		}
		else
		{
			LogManager.getLogger().debug("Ignoring AnimatePacket with action {}", animatePacket.getAction().name());
		}
	}

	private void handlePlayerDead()
	{
		// TODO: end session or respawn player as appropriate according to the Earth game settings (e.g. if in adventure or buildplate)

		ServerboundClientCommandPacket serverboundClientCommandPacket = new ServerboundClientCommandPacket(ClientCommand.RESPAWN);
		this.sendJavaPacket(serverboundClientCommandPacket);

		// TODO: immediately move the player back to the last client-sent position rather than waiting for the client to send the next position update
	}

	public void playerInteraction(@NotNull InventoryTransactionPacket packet)
	{
		if (packet.getTransactionType() == InventoryTransactionType.ITEM_USE)
		{
			if (packet.getActionType() == 0) // place/use item on block
			{
				ServerboundUseItemOnPacket serverboundUseItemOnPacket = new ServerboundUseItemOnPacket(packet.getBlockPosition(), Direction.VALUES[packet.getBlockFace()], Hand.MAIN_HAND, packet.getClickPosition().getX(), packet.getClickPosition().getY(), packet.getClickPosition().getZ(), false, 0);
				this.sendJavaPacket(serverboundUseItemOnPacket);
			}
			else if (packet.getActionType() == 1) // use item
			{
				ServerboundUseItemPacket serverboundUseItemPacket = new ServerboundUseItemPacket(Hand.MAIN_HAND, 0);
				this.sendJavaPacket(serverboundUseItemPacket);
			}
			else if (packet.getActionType() == 2) // dig
			{
				ServerboundPlayerActionPacket serverboundPlayerActionPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, packet.getBlockPosition(), Direction.VALUES[packet.getBlockFace()], 0);
				this.sendJavaPacket(serverboundPlayerActionPacket);
			}
		}
		else if (packet.getTransactionType() == InventoryTransactionType.ITEM_USE_ON_ENTITY)
		{
			EntityManager.BedrockEntityInstance entityInstance = this.entityManager.getBedrockEntity(packet.getRuntimeEntityId());
			if (entityInstance == null)
			{
				LogManager.getLogger().warn("Client tried to interact with entity ID {} that does not exist", packet.getRuntimeEntityId());
				return;
			}
			if (packet.getActionType() == 0) // use item
			{
				entityInstance.onInteract();
			}
			else if (packet.getActionType() == 1) // attack
			{
				entityInstance.onAttack();
			}
		}
	}

	public void playerBreakBlock(@NotNull Vector3i position, int face)
	{
		ServerboundPlayerActionPacket serverboundPlayerActionPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, position, Direction.VALUES[face], 0);
		this.sendJavaPacket(serverboundPlayerActionPacket);
	}

	public void playerItemPickup(long itemEntityId)
	{
		EntityManager.BedrockEntityInstance bedrockEntityInstance = this.entityManager.getBedrockEntity(itemEntityId);
		if (bedrockEntityInstance == null || !(bedrockEntityInstance instanceof ItemJavaEntityInstance.ItemBedrockEntityInstance))
		{
			LogManager.getLogger().warn("Client sent item pickup with entity ID {} that does not exist or is not an item entity", itemEntityId);
			return;
		}
		ItemJavaEntityInstance javaEntityInstance = ((ItemJavaEntityInstance.ItemBedrockEntityInstance) bedrockEntityInstance).getJavaEntityInstance();
		javaEntityInstance.sendPickup();
	}

	public void updateSelectedHotbarItem(@NotNull MobEquipmentPacket mobEquipmentPacket)
	{
		if (mobEquipmentPacket.getRuntimeEntityId() != this.bedrockPlayerEntityId)
		{
			LogManager.getLogger().warn("Unrecognised client MobEquipmentPacket (entity ID does not match player)");
			return;
		}
		else if (!(mobEquipmentPacket.getContainerId() == 0 || mobEquipmentPacket.getContainerId() == 125))
		{
			LogManager.getLogger().warn("Unrecognised MobEquipmentPacket (unrecognised container ID {})", mobEquipmentPacket.getContainerId());
			return;
		}

		if (mobEquipmentPacket.getContainerId() == 0)
		{
			this.bedrockSelectedHotbarSlot = mobEquipmentPacket.getHotbarSlot();

			ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket = new ServerboundSetCarriedItemPacket(this.bedrockSelectedHotbarSlot);
			this.sendJavaPacket(serverboundSetCarriedItemPacket);

			this.inventoryManager.sendClientHotbar();    // required because otherwise the client sometimes displays an old item in the selected slot
		}
		else if (mobEquipmentPacket.getContainerId() == 125)
		{
			// switch between interact and break/pickup mode
			// we use the last two hotbar slots to represent these on the Java server, as they should usually be empty

			int itemId = mobEquipmentPacket.getItem().getDefinition().getRuntimeId();
			if (itemId == 2258 || itemId == 2261) // pickup/punch
			{
				this.bedrockSelectedHotbarSlot = 7;
			}
			else if (itemId == 2259) // interact
			{
				this.bedrockSelectedHotbarSlot = 8;
			}

			ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket = new ServerboundSetCarriedItemPacket(this.bedrockSelectedHotbarSlot);
			this.sendJavaPacket(serverboundSetCarriedItemPacket);
		}
	}

	public void onGenoaInventoryOpen()
	{
		this.inventoryManager.sendClientGenoaInventory();
	}

	public void onGenoaInventoryChange(@NotNull GenoaInventoryDataPacket genoaInventoryDataPacket)
	{
		this.inventoryManager.onGenoaHotbarChange(genoaInventoryDataPacket.json);
	}

	public void onJavaItemPickupParticle(byte[] data)
	{
		try
		{
			ByteBuf buf = Unpooled.wrappedBuffer(data);
			MinecraftCodecHelper minecraftCodecHelper = (MinecraftCodecHelper) this.java.getCodecHelper();
			ItemStack itemStack = minecraftCodecHelper.readItemStack(buf);
			Vector3d pos = Vector3d.from(buf.readDouble(), buf.readDouble(), buf.readDouble());

			int javaId = itemStack.getId();
			JavaItems.BedrockMapping bedrockMapping = JavaItems.getBedrockMapping(javaId);
			if (bedrockMapping == null)
			{
				LogManager.getLogger().warn("Item pickup particle for item with no mapping {}", JavaItems.getName(javaId));
				return;
			}

			this.sendItemParticle(bedrockMapping.id, bedrockMapping.aux, pos.toFloat(), this.bedrockPlayerEntityId);
		}
		catch (IOException | NullPointerException exception)
		{
			LogManager.getLogger().warn("Server sent bad item pickup particle data", exception);
		}
	}

	public void onJavaSetCarriedItem(int slot)
	{
		if (slot != this.bedrockSelectedHotbarSlot)
		{
			LogManager.getLogger().warn("Server set selected hotbar slot to {}, client has requested {}", slot, this.bedrockSelectedHotbarSlot);

			ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket = new ServerboundSetCarriedItemPacket(this.bedrockSelectedHotbarSlot);
			this.sendJavaPacket(serverboundSetCarriedItemPacket);
		}
	}

	public void onJavaContainerSetContent(@NotNull ClientboundContainerSetContentPacket clientboundContainerSetContentPacket)
	{
		if (clientboundContainerSetContentPacket.getContainerId() == 0)
		{
			this.inventoryManager.syncInventory();
		}
	}

	public void onJavaContainerSetSlot(@NotNull ClientboundContainerSetSlotPacket clientboundContainerSetSlotPacket)
	{
		if (clientboundContainerSetSlotPacket.getContainerId() == 0)
		{
			this.inventoryManager.syncInventory();
		}
	}

	public void onJavaInventorySyncResponse(byte[] data)
	{
		try
		{
			ByteBuf buf = Unpooled.wrappedBuffer(data);
			MinecraftCodecHelper minecraftCodecHelper = (MinecraftCodecHelper) this.java.getCodecHelper();

			int count = buf.readInt();
			ItemStack[] itemStacks = new ItemStack[count];
			for (int index = 0; index < count; index++)
			{
				ItemStack itemStack = minecraftCodecHelper.readItemStack(buf);
				if (itemStack == null)
				{
					LogManager.getLogger().warn("Server sent bad inventory sync response data (null item stack)");
					return;
				}
				itemStacks[index] = itemStack;
			}

			ItemStack[] hotbar = new ItemStack[7];
			for (int index = 0; index < 7; index++)
			{
				ItemStack itemStack = minecraftCodecHelper.readItemStack(buf);
				hotbar[index] = itemStack;
			}

			this.inventoryManager.onInventorySyncResponse(itemStacks, hotbar);
		}
		catch (IOException exception)
		{
			LogManager.getLogger().warn("Server sent bad inventory sync response data", exception);
		}
	}

	public void onJavaSetHotbarResponse(byte[] data)
	{
		ByteBuf buf = Unpooled.wrappedBuffer(data);
		MinecraftCodecHelper minecraftCodecHelper = (MinecraftCodecHelper) this.java.getCodecHelper();
		boolean success = buf.readBoolean();
		this.inventoryManager.onSetHotbarResponse(success);
	}

	public void sendItemParticle(int bedrockItemId, int bedrockItemDataValue, @NotNull Vector3f fromPosition, long pickedUpByRuntimeEntityId)
	{
		GenoaItemParticlePacket genoaItemParticlePacket = new GenoaItemParticlePacket();
		genoaItemParticlePacket.setRuntimeEntityId(pickedUpByRuntimeEntityId);
		genoaItemParticlePacket.setItemId(bedrockItemId);
		genoaItemParticlePacket.setItemDataValue(bedrockItemDataValue);
		genoaItemParticlePacket.setPosition(fromPosition);
		this.sendBedrockPacket(genoaItemParticlePacket);
	}

	public void sendBedrockPacket(@NotNull BedrockPacket packet)
	{
		this.bedrock.sendPacket(packet);
	}

	public void sendJavaPacket(@NotNull MinecraftPacket packet)
	{
		this.java.send(packet);
	}
}