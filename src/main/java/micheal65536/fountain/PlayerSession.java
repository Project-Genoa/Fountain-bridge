package micheal65536.fountain;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.data.GamePublishSetting;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.GenoaGameplaySettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkChunkPublisherUpdatePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDifficultyPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.utils.LevelChunkUtils;
import micheal65536.fountain.utils.LoginUtils;

import java.util.Arrays;
import java.util.HashMap;

public final class PlayerSession
{
	private static final int MAX_NONEMPTY_CHUNK_RADIUS = 2;
	private static final int HARDCODED_CHUNK_RADIUS = 20;
	private static final Vector3i HARDCODED_CHUNK_CENTER = Vector3i.from(0, 128, 0);

	private final BedrockSession bedrock;

	private final TcpClientSession java;
	private int javaPlayerEntityId;
	private final HashMap<Integer, String> javaBiomes = new HashMap<>();

	public PlayerSession(@NotNull BedrockSession bedrockSession, @NotNull LoginPacket loginPacket)
	{
		this.bedrock = bedrockSession;

		MinecraftProtocol javaProtocol = new MinecraftProtocol(LoginUtils.getUsername(loginPacket));
		this.java = new TcpClientSession("127.0.0.1", 25565, javaProtocol);
		this.java.addListener(new ServerPacketHandler(this));
		this.java.connect(true);
	}

	public void disconnectForced()
	{
		// TODO
	}

	public void onJavaLogin(@NotNull ClientboundLoginPacket clientboundLoginPacket)
	{
		this.javaPlayerEntityId = clientboundLoginPacket.getEntityId();

		PlayStatusPacket playStatusPacket = new PlayStatusPacket();
		playStatusPacket.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
		this.sendBedrockPacket(playStatusPacket);

		StartGamePacket startGamePacket = new StartGamePacket();
		startGamePacket.getGamerules().add(new GameRuleData<>("dodaylightcycle", false));
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
		startGamePacket.setDayCycleStopTime(6000);    // TODO: set according to buildplate day/night mode
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
	}

	public void onJavaDifficultyChanged(@NotNull Difficulty difficulty)
	{
		SetDifficultyPacket setDifficultyPacket = new SetDifficultyPacket();
		setDifficultyPacket.setDifficulty(difficulty.ordinal());
		this.sendBedrockPacket(setDifficultyPacket);
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
		if (clientboundLevelChunkWithLightPacket.getX() >= -MAX_NONEMPTY_CHUNK_RADIUS && clientboundLevelChunkWithLightPacket.getX() <= MAX_NONEMPTY_CHUNK_RADIUS && clientboundLevelChunkWithLightPacket.getZ() >= -MAX_NONEMPTY_CHUNK_RADIUS && clientboundLevelChunkWithLightPacket.getZ() <= MAX_NONEMPTY_CHUNK_RADIUS)
		{
			LevelChunkPacket levelChunkPacket = LevelChunkUtils.translateLevelChunk(clientboundLevelChunkWithLightPacket, this.javaBiomes, (MinecraftCodecHelper) this.java.getCodecHelper());
			this.sendBedrockPacket(levelChunkPacket);
		}
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