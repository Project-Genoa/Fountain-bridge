package micheal65536.fountain;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.data.GamePublishSetting;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.cloudburstmc.protocol.bedrock.packet.GenoaGameplaySettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.GenoaInventoryDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDifficultyPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.registry.BedrockBlocks;
import micheal65536.fountain.registry.JavaBlocks;
import micheal65536.fountain.utils.GenoaInventory;
import micheal65536.fountain.utils.ItemTranslator;
import micheal65536.fountain.utils.LevelChunkUtils;
import micheal65536.fountain.utils.LoginUtils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;

public final class PlayerSession
{
	private static final int MAX_NONEMPTY_CHUNK_RADIUS = 2;
	private static final int HARDCODED_CHUNK_RADIUS = 20;
	private static final Vector3i HARDCODED_CHUNK_CENTER = Vector3i.from(0, 128, 0);

	private static final int JAVA_MAIN_INVENTORY_OFFSET = 9;
	private static final int JAVA_MAIN_INVENTORY_EXTRA_OFFSET = JAVA_MAIN_INVENTORY_OFFSET + 7;
	private static final int JAVA_HOTBAR_OFFSET = JAVA_MAIN_INVENTORY_OFFSET + 27;

	private final BedrockSession bedrock;
	private boolean bedrockDoDaylightCycle = true;
	private int bedrockSelectedHotbarSlot = 0;

	private final TcpClientSession java;
	private int javaPlayerEntityId;
	private final HashMap<Integer, String> javaBiomes = new HashMap<>();
	private final ItemStack[] javaPlayerHotbar = new ItemStack[7];

	private final GenoaInventory genoaInventory;

	public PlayerSession(@NotNull BedrockSession bedrockSession, @NotNull LoginPacket loginPacket)
	{
		this.genoaInventory = new GenoaInventory(); // TODO: initialise from API server, sync initial hotbar to Java server at some point

		this.bedrock = bedrockSession;

		MinecraftProtocol javaProtocol = new MinecraftProtocol(LoginUtils.getUsername(loginPacket));
		this.java = new TcpClientSession("127.0.0.1", 25565, javaProtocol);
		this.java.addListener(new ServerPacketHandler(this));
		this.java.connect(true);
	}

	public void disconnectForced()
	{
		// TODO

		try
		{
			this.bedrock.disconnect();
		}
		catch (IllegalStateException exception)
		{
			LogManager.getLogger().debug("Disconnect with Bedrock session already closed (this is usually normal)");
		}
		this.java.disconnect("");
	}

	public void onJavaLogin(@NotNull ClientboundLoginPacket clientboundLoginPacket)
	{
		this.javaPlayerEntityId = clientboundLoginPacket.getEntityId();

		PlayStatusPacket playStatusPacket = new PlayStatusPacket();
		playStatusPacket.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
		this.sendBedrockPacket(playStatusPacket);

		StartGamePacket startGamePacket = new StartGamePacket();
		//startGamePacket.getGamerules().add(new GameRuleData<>("dodaylightcycle", false));
		startGamePacket.setUniqueEntityId(this.javaPlayerEntityId);
		startGamePacket.setRuntimeEntityId(this.javaPlayerEntityId);
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
		genoaGameplaySettingsPacket.setOwnerRuntimeId(this.javaPlayerEntityId);
		this.sendBedrockPacket(genoaGameplaySettingsPacket);

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
				this.sendBedrockPacket(LevelChunkUtils.makeEmpty(chunkX, chunkZ));
			}
		}

		ServerboundClientInformationPacket serverboundClientInformationPacket = new ServerboundClientInformationPacket("en_GB", HARDCODED_CHUNK_RADIUS, ChatVisibility.FULL, true, Arrays.asList(SkinPart.values()), HandPreference.RIGHT_HAND, false, true);
		this.sendJavaPacket(serverboundClientInformationPacket);

		ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket = new ServerboundSetCarriedItemPacket(this.bedrockSelectedHotbarSlot);
		this.sendJavaPacket(serverboundSetCarriedItemPacket);
	}

	public void onJavaDifficultyChanged(@NotNull Difficulty difficulty)
	{
		SetDifficultyPacket setDifficultyPacket = new SetDifficultyPacket();
		setDifficultyPacket.setDifficulty(difficulty.ordinal());
		this.sendBedrockPacket(setDifficultyPacket);
	}

	public void updateTime(long javaTime)
	{
		LogManager.getLogger().trace("Server set time to " + javaTime);

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

	public void onJavaLevelChunk(@NotNull ClientboundLevelChunkWithLightPacket clientboundLevelChunkWithLightPacket)
	{
		if (clientboundLevelChunkWithLightPacket.getX() >= -MAX_NONEMPTY_CHUNK_RADIUS && clientboundLevelChunkWithLightPacket.getX() < MAX_NONEMPTY_CHUNK_RADIUS && clientboundLevelChunkWithLightPacket.getZ() >= -MAX_NONEMPTY_CHUNK_RADIUS && clientboundLevelChunkWithLightPacket.getZ() < MAX_NONEMPTY_CHUNK_RADIUS)
		{
			LogManager.getLogger().trace("Sending chunk " + clientboundLevelChunkWithLightPacket.getX() + ", " + clientboundLevelChunkWithLightPacket.getZ());

			LevelChunkPacket levelChunkPacket = LevelChunkUtils.translateLevelChunk(clientboundLevelChunkWithLightPacket, this.javaBiomes, (MinecraftCodecHelper) this.java.getCodecHelper());
			this.sendBedrockPacket(levelChunkPacket);
		}
	}

	public void onJavaBlockUpdate(@NotNull BlockChangeEntry blockChangeEntry)
	{
		Vector3i position = blockChangeEntry.getPosition();
		if (position.getY() > 0 && position.getY() < 256)
		{
			if (position.getX() >= -MAX_NONEMPTY_CHUNK_RADIUS * 16 && position.getX() < MAX_NONEMPTY_CHUNK_RADIUS * 16 && position.getZ() >= -MAX_NONEMPTY_CHUNK_RADIUS * 16 && position.getZ() < MAX_NONEMPTY_CHUNK_RADIUS * 16)
			{
				LogManager.getLogger().trace("Sending block update " + position.getX() + ", " + position.getY() + ", " + position.getZ());

				// TODO: flower pots, pistons, cauldrons, lecterns

				// TODO: doors

				int bedrockId = JavaBlocks.getBedrockId(blockChangeEntry.getBlock());
				if (bedrockId == -1)
				{
					LogManager.getLogger().warn("Block update contained block with no mapping " + JavaBlocks.getName(blockChangeEntry.getBlock()));
					bedrockId = BedrockBlocks.AIR;
				}

				UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
				updateBlockPacket.setBlockPosition(position);
				updateBlockPacket.setDataLayer(0);
				updateBlockPacket.setDefinition(this.bedrock.getPeer().getCodecHelper().getBlockDefinitions().getDefinition(bedrockId));
				updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
				this.sendBedrockPacket(updateBlockPacket);

				updateBlockPacket = new UpdateBlockPacket();
				updateBlockPacket.setBlockPosition(position);
				updateBlockPacket.setDataLayer(1);
				updateBlockPacket.setDefinition(this.bedrock.getPeer().getCodecHelper().getBlockDefinitions().getDefinition(JavaBlocks.isWaterlogged(blockChangeEntry.getBlock()) ? BedrockBlocks.WATER : BedrockBlocks.AIR));
				updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
				this.sendBedrockPacket(updateBlockPacket);
			}
		}
	}

	public void updateSelectedHotbarItem(@NotNull MobEquipmentPacket mobEquipmentPacket)
	{
		if (mobEquipmentPacket.getRuntimeEntityId() != this.javaPlayerEntityId || !(mobEquipmentPacket.getContainerId() == 0 || mobEquipmentPacket.getContainerId() == 125))
		{
			LogManager.getLogger().warn("Unrecognised MobEquipmentPacket (" + mobEquipmentPacket.getRuntimeEntityId() + ", " + mobEquipmentPacket.getContainerId() + ")");
			return;
		}

		if (mobEquipmentPacket.getContainerId() == 0)
		{
			this.bedrockSelectedHotbarSlot = mobEquipmentPacket.getHotbarSlot();

			ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket = new ServerboundSetCarriedItemPacket(this.bedrockSelectedHotbarSlot);
			this.sendJavaPacket(serverboundSetCarriedItemPacket);

			this.sendHotbar();    // required because otherwise the client sometimes displays an old item in the selected slot
		}
		else if (mobEquipmentPacket.getContainerId() == 125)
		{
			// TODO: pickup/interact mode
		}
	}

	public void onGenoaInventoryChange(@NotNull GenoaInventoryDataPacket genoaInventoryDataPacket)
	{
		String[] newHotbar = this.genoaInventory.updateHotbar(this.javaPlayerHotbar, genoaInventoryDataPacket.json);
		if (newHotbar != null)
		{
			this.sendCommand("clear @s");
			int index = 0;
			for (String item : newHotbar)
			{
				this.sendCommand("item replace entity @s hotbar." + index + " with " + item);
				index++;
			}
		}
	}

	public void onJavaSetCarriedItem(int slot)
	{
		if (slot != this.bedrockSelectedHotbarSlot)
		{
			LogManager.getLogger().warn("Server set selected hotbar slot to " + slot + ", client has requested " + (this.bedrockSelectedHotbarSlot));

			ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket = new ServerboundSetCarriedItemPacket(this.bedrockSelectedHotbarSlot);
			this.sendJavaPacket(serverboundSetCarriedItemPacket);
		}
		else
		{
			LogManager.getLogger().debug("Server set selected hotbar slot to " + slot);
		}
	}

	public void onJavaContainerSetContent(@NotNull ClientboundContainerSetContentPacket clientboundContainerSetContentPacket)
	{
		if (clientboundContainerSetContentPacket.getContainerId() == 0)
		{
			ItemStack[] items = clientboundContainerSetContentPacket.getItems();
			for (int slotIndex = 0; slotIndex < items.length; slotIndex++)
			{
				if (slotIndex >= JAVA_HOTBAR_OFFSET && slotIndex < JAVA_HOTBAR_OFFSET + this.javaPlayerHotbar.length)
				{
					this.javaPlayerHotbar[slotIndex - JAVA_HOTBAR_OFFSET] = cloneItemStack(items[slotIndex]);
				}
				else
				{
					if (items[slotIndex] != null)
					{
						this.transferJavaInventorySlotToGenoa(items[slotIndex], slotIndex);
					}
				}
			}
			this.sendHotbar();
		}
	}

	public void onJavaContainerSetSlot(@NotNull ClientboundContainerSetSlotPacket clientboundContainerSetSlotPacket)
	{
		if (clientboundContainerSetSlotPacket.getContainerId() == 0)
		{
			int slotIndex = clientboundContainerSetSlotPacket.getSlot();
			if (slotIndex >= JAVA_HOTBAR_OFFSET && slotIndex < JAVA_HOTBAR_OFFSET + this.javaPlayerHotbar.length)
			{
				this.javaPlayerHotbar[slotIndex - JAVA_HOTBAR_OFFSET] = cloneItemStack(clientboundContainerSetSlotPacket.getItem());
				this.sendHotbar();
			}
			else
			{
				ItemStack itemStack = clientboundContainerSetSlotPacket.getItem();
				if (itemStack != null)
				{
					this.transferJavaInventorySlotToGenoa(itemStack, slotIndex);
				}
			}
		}
	}

	private static ItemStack cloneItemStack(@Nullable ItemStack itemStack)
	{
		return itemStack != null ? new ItemStack(itemStack.getId(), itemStack.getAmount(), itemStack.getNbt() != null ? itemStack.getNbt().clone() : null) : null;
	}

	private void transferJavaInventorySlotToGenoa(@NotNull ItemStack itemStack, int slotIndex)
	{
		if (slotIndex < JAVA_MAIN_INVENTORY_OFFSET || slotIndex >= JAVA_MAIN_INVENTORY_OFFSET + 36 || (slotIndex >= JAVA_HOTBAR_OFFSET && slotIndex < JAVA_HOTBAR_OFFSET + this.javaPlayerHotbar.length))
		{
			throw new IllegalArgumentException();
		}

		slotIndex = slotIndex - JAVA_MAIN_INVENTORY_OFFSET;
		if (slotIndex >= 27)
		{
			this.sendCommand("item replace entity @s hotbar." + (slotIndex - 27) + " with minecraft:air");
		}
		else
		{
			this.sendCommand("item replace entity @s inventory." + slotIndex + " with minecraft:air");
		}

		this.genoaInventory.addItem(itemStack);
	}

	public void sendHotbar()
	{
		InventoryContentPacket inventoryContentPacket = new InventoryContentPacket();
		inventoryContentPacket.setContainerId(0);
		inventoryContentPacket.setContents(Arrays.stream(this.javaPlayerHotbar).map(itemStack ->
		{
			if (itemStack == null)
			{
				return ItemData.builder().build();
			}
			else
			{
				return ItemTranslator.translateJavaToBedrock(itemStack, this.bedrock.getPeer().getCodecHelper());
			}
		}).toList());
		this.sendBedrockPacket(inventoryContentPacket);
	}

	public void sendGenoaInventory()
	{
		GenoaInventoryDataPacket genoaInventoryDataPacket = new GenoaInventoryDataPacket();
		genoaInventoryDataPacket.setJson(this.genoaInventory.getJSONString(this.javaPlayerHotbar));
		this.sendBedrockPacket(genoaInventoryDataPacket);
	}

	public void sendBedrockPacket(@NotNull BedrockPacket packet)
	{
		this.bedrock.sendPacket(packet);
	}

	public void sendJavaPacket(@NotNull MinecraftPacket packet)
	{
		this.java.send(packet);
	}

	public void sendCommand(@NotNull String command)
	{
		ServerboundChatCommandPacket serverboundChatCommandPacket = new ServerboundChatCommandPacket(command, System.currentTimeMillis(), 0, Collections.emptyList(), 0, new BitSet());
		this.sendJavaPacket(serverboundChatCommandPacket);
	}
}