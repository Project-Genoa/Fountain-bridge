package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.chunk.BitStorage;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.Palette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.SingletonPalette;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.mc.protocol.data.game.level.block.value.BlockValue;
import com.github.steveice10.mc.protocol.data.game.level.block.value.BlockValueType;
import com.github.steveice10.mc.protocol.data.game.level.block.value.NoteBlockValue;
import com.github.steveice10.mc.protocol.data.game.level.block.value.PistonValue;
import com.github.steveice10.mc.protocol.data.game.level.block.value.PistonValueType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.BlockEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.Main;
import micheal65536.fountain.PlayerSession;
import micheal65536.fountain.registry.BedrockBiomes;
import micheal65536.fountain.registry.BedrockBlocks;
import micheal65536.fountain.registry.JavaBlocks;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class ChunkManager
{
	private static final int REPLACEMENT_BLOCK = BedrockBlocks.AIR;
	private static final int DEFAULT_BIOME = 4;    // forest biome

	private final PlayerSession playerSession;
	private final FabricRegistryManager fabricRegistryManager;

	private final int chunkRadius;
	private final Chunk[][] chunks;

	public ChunkManager(int chunkRadius, @NotNull PlayerSession playerSession, @NotNull FabricRegistryManager fabricRegistryManager)
	{
		this.playerSession = playerSession;
		this.fabricRegistryManager = fabricRegistryManager;

		this.chunkRadius = chunkRadius;
		this.chunks = new Chunk[this.chunkRadius * 2][this.chunkRadius * 2];
		for (int chunkX = -this.chunkRadius; chunkX < this.chunkRadius; chunkX++)
		{
			for (int chunkZ = -this.chunkRadius; chunkZ < this.chunkRadius; chunkZ++)
			{
				this.chunks[chunkX + this.chunkRadius][chunkZ + this.chunkRadius] = new Chunk(chunkX, chunkZ);
			}
		}
	}

	public void sendFullChunk(int chunkX, int chunkZ)
	{
		if (chunkX < -this.chunkRadius || chunkX >= this.chunkRadius || chunkZ < -this.chunkRadius || chunkZ >= this.chunkRadius)
		{
			return;
		}
		Chunk chunk = this.getChunk(chunkX, chunkZ);

		byte[] data;
		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
		try
		{
			byteBuf.writeByte(16);
			for (int subchunkY = 0; subchunkY < 16; subchunkY++)
			{
				LinkedHashMap<Integer, Integer>[] palettes = new LinkedHashMap[]{new LinkedHashMap<>(), new LinkedHashMap<>()};
				int[][] blocks = new int[2][4096];
				for (int x = 0; x < 16; x++)
				{
					for (int z = 0; z < 16; z++)
					{
						for (int y = 0; y < 16; y++)
						{
							int level0BlockId = chunk.getBlock(x, y + subchunkY * 16, z, 0);
							int level1BlockId = chunk.getBlock(x, y + subchunkY * 16, z, 1);
							blocks[0][(x * 16 + z) * 16 + y] = palettes[0].computeIfAbsent(level0BlockId, blockId -> palettes[0].size());
							blocks[1][(x * 16 + z) * 16 + y] = palettes[1].computeIfAbsent(level1BlockId, blockId -> palettes[1].size());
						}
					}
				}

				byteBuf.writeByte(8);
				int maxLayer = !(palettes[1].size() == 1 && palettes[1].containsKey(BedrockBlocks.AIR)) ? 2 : 1;
				byteBuf.writeByte(maxLayer);
				for (int layer = 0; layer < maxLayer; layer++)
				{
					int bits = 16;
					int paletteSize = palettes[layer].size();
					for (int bits1 : new int[]{1, 2, 3, 4, 5, 6, 8, 16})
					{
						if (paletteSize <= (1 << bits1))
						{
							bits = bits1;
							break;
						}
					}

					byteBuf.writeByte((bits << 1) | 1);
					int blocksPerInt = 32 / bits;
					for (int index = 0; index < 4096; index += blocksPerInt)
					{
						int value = 0;
						for (int index1 = 0; index1 < blocksPerInt && index + index1 < 4096; index1++)
						{
							value = value | (blocks[layer][index + index1] << (bits * index1));
						}
						byteBuf.writeIntLE(value);
					}

					VarInts.writeInt(byteBuf, palettes[layer].size());
					for (int id : palettes[layer].keySet())
					{
						VarInts.writeInt(byteBuf, id);
					}
				}
			}

			for (int x = 0; x < 16; x++)
			{
				for (int z = 0; z < 16; z++)
				{
					byteBuf.writeShortLE(chunk.getHeightmap(x, z));
				}
			}

			for (int x = 0; x < 16; x++)
			{
				for (int z = 0; z < 16; z++)
				{
					byteBuf.writeByte(chunk.getBiome(x, z));
				}
			}

			VarInts.writeUnsignedInt(byteBuf, 0);

			NbtMap[] blockEntities = chunk.getBlockEntities();
			if (blockEntities.length > 0)
			{
				NBTOutputStream nbtOutputStream = NbtUtils.createNetworkWriter(new ByteBufOutputStream(byteBuf));
				for (NbtMap nbtMap : blockEntities)
				{
					nbtOutputStream.writeTag(nbtMap);
				}
				nbtOutputStream.close();
			}

			data = new byte[byteBuf.readableBytes()];
			byteBuf.readBytes(data);
		}
		catch (IOException exception)
		{
			throw new AssertionError(exception);
		}
		finally
		{
			byteBuf.release();
		}

		LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
		levelChunkPacket.setChunkX(chunkX);
		levelChunkPacket.setChunkZ(chunkZ);
		levelChunkPacket.setData(Unpooled.wrappedBuffer(data));
		this.playerSession.sendBedrockPacket(levelChunkPacket);
	}

	public void onJavaLevelChunk(@NotNull ClientboundLevelChunkWithLightPacket clientboundLevelChunkWithLightPacket, @NotNull HashMap<Integer, String> javaBiomesMap, @NotNull MinecraftCodecHelper minecraftCodecHelper) // TODO: put biome map and codec helper in class fields rather than taking them as parameters every time
	{
		int chunkX = clientboundLevelChunkWithLightPacket.getX();
		int chunkZ = clientboundLevelChunkWithLightPacket.getZ();
		if (chunkX < -this.chunkRadius || chunkX >= this.chunkRadius || chunkZ < -this.chunkRadius || chunkZ >= this.chunkRadius)
		{
			return;
		}
		Chunk bedrockChunk = this.getChunk(clientboundLevelChunkWithLightPacket.getX(), clientboundLevelChunkWithLightPacket.getZ());

		try
		{
			HashSet<Integer> alreadyNotifiedMissingBlocks = new HashSet<>();
			HashSet<String> alreadyNotifiedMissingBiomes = new HashSet<>();
			ByteBuf byteBuf = Unpooled.wrappedBuffer(clientboundLevelChunkWithLightPacket.getChunkData());
			int[][][] javaBiomes = new int[4][4][64];
			for (int[][] x : javaBiomes)
			{
				for (int[] z : x)
				{
					Arrays.fill(z, -1);
				}
			}
			for (int chunkY = -4; chunkY < 20; chunkY++) // Java world height goes from -64 to 320
			{
				ChunkSection chunkSection = minecraftCodecHelper.readChunkSection(byteBuf, 32);
				if (chunkY < 0 || chunkY >= 16)
				{
					continue;
				}

				Palette javaBlockPalette = chunkSection.getChunkData().getPalette();
				BitStorage javaBlockData = chunkSection.getChunkData().getStorage();
				for (int yzx = 0; yzx < 4096; yzx++)
				{
					int x = yzx & 0x00F;
					int z = (yzx & 0x0F0) >> 4;
					int y = ((yzx & 0xF00) >> 8) + chunkY * 16;

					int javaId;
					if (javaBlockPalette instanceof SingletonPalette)
					{
						javaId = javaBlockPalette.idToState(0);
					}
					else
					{
						javaId = javaBlockPalette.idToState(javaBlockData.get(yzx));
					}

					JavaBlocks.BedrockMapping bedrockMapping = JavaBlocks.getBedrockMapping(javaId, this.fabricRegistryManager);
					if (bedrockMapping == null)
					{
						if (alreadyNotifiedMissingBlocks.add(javaId))
						{
							LogManager.getLogger().warn("Chunk contained block with no mapping {}", JavaBlocks.getName(javaId, this.fabricRegistryManager));
						}
					}

					bedrockChunk.setBlock(x, y, z, 0, bedrockMapping != null ? bedrockMapping.id : REPLACEMENT_BLOCK);
					bedrockChunk.setBlock(x, y, z, 1, bedrockMapping != null && bedrockMapping.waterlogged ? BedrockBlocks.WATER : BedrockBlocks.AIR);

					JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping = bedrockMapping != null && bedrockMapping.blockEntity != null ? bedrockMapping.blockEntity : null;
					NbtMap bedrockBlockEntityData = blockEntityMapping != null ? BlockEntityTranslator.translateBlockEntity(blockEntityMapping, null) : null;
					bedrockChunk.setBlockEntity(x, y, z, blockEntityMapping, bedrockBlockEntityData);

					bedrockChunk.setExtraData(x, y, z, bedrockMapping != null ? bedrockMapping.extraData : null);
				}

				Palette javaBiomePalette = chunkSection.getBiomeData().getPalette();
				BitStorage javaBiomeData = chunkSection.getBiomeData().getStorage();
				for (int yzx = 0; yzx < 64; yzx++)
				{
					int javaId;
					if (javaBiomePalette instanceof SingletonPalette)
					{
						javaId = javaBiomePalette.idToState(0);
					}
					else
					{
						javaId = javaBiomePalette.idToState(javaBiomeData.get(yzx));
					}

					int bedrockId;
					String biomeName = javaBiomesMap.getOrDefault(javaId, null);
					if (biomeName == null)
					{
						LogManager.getLogger().warn("Server sent bad biome data");
						bedrockId = -1;
					}
					else
					{
						bedrockId = BedrockBiomes.getId(biomeName);
						if (bedrockId == -1)
						{
							if (alreadyNotifiedMissingBiomes.add(biomeName))
							{
								LogManager.getLogger().warn("Chunk contained biome with no mapping {}", biomeName);
							}
						}
					}

					int x = yzx & 0b000011;
					int z = (yzx & 0b001100) >> 2;
					int y = ((yzx & 0b110000) >> 4) + chunkY * 4;
					javaBiomes[x][z][y] = bedrockId;
				}
			}

			HashMap<Integer, Integer> biomeCounts = new HashMap<>();
			for (int x = 0; x < 16; x++)
			{
				for (int z = 0; z < 16; z++)
				{
					biomeCounts.clear();
					for (int y = 0; y < 256; y++)
					{
						int biomeId = javaBiomes[x / 4][z / 4][y / 4];
						if (biomeId != -1)
						{
							biomeCounts.put(biomeId, biomeCounts.getOrDefault(biomeId, 0) + 1);
						}
					}

					int mostCommonBiomeId = -1;
					int mostCommonBiomeCount = 0;
					for (Map.Entry<Integer, Integer> entry : biomeCounts.entrySet())
					{
						if (entry.getValue() > mostCommonBiomeCount)
						{
							mostCommonBiomeId = entry.getKey();
						}
					}

					if (mostCommonBiomeId == -1)
					{
						LogManager.getLogger().warn("Could not determine biome for {}, {} ({} candidates)", x, z, biomeCounts.size());
						mostCommonBiomeId = DEFAULT_BIOME;
					}

					bedrockChunk.setBiome(x, z, mostCommonBiomeId);
				}
			}

			for (BlockEntityInfo blockEntityInfo : clientboundLevelChunkWithLightPacket.getBlockEntities())
			{
				int x = blockEntityInfo.getX();
				int y = blockEntityInfo.getY();
				int z = blockEntityInfo.getZ();
				JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping = bedrockChunk.getBlockEntityMapping(x, y, z);
				if (blockEntityMapping == null)
				{
					LogManager.getLogger().debug("Ignoring block entity of type {}", blockEntityInfo.getType());
				}
				NbtMap bedrockBlockEntityData = blockEntityMapping != null ? BlockEntityTranslator.translateBlockEntity(blockEntityMapping, blockEntityInfo) : null;
				bedrockChunk.setBlockEntity(x, y, z, blockEntityMapping, bedrockBlockEntityData);
			}
		}
		catch (IOException exception)
		{
			LogManager.getLogger().error("Error reading level chunk packet", exception);
			bedrockChunk.clear();
		}

		this.sendFullChunk(chunkX, chunkZ);
	}

	public void onJavaBlockUpdate(@NotNull BlockChangeEntry blockChangeEntry)
	{
		Vector3i position = blockChangeEntry.getPosition();
		if (this.isOutOfBounds(position.getX(), position.getY(), position.getZ()))
		{
			return;
		}
		Chunk chunk = this.getChunkForBlock(position.getX(), position.getZ());
		int blockX = getChunkBlockOffset(position.getX());
		int blockY = position.getY();
		int blockZ = getChunkBlockOffset(position.getZ());

		// TODO: Geyser said we need to special-case doors here, but they seem to work fine?

		JavaBlocks.BedrockMapping bedrockMapping = JavaBlocks.getBedrockMapping(blockChangeEntry.getBlock(), this.fabricRegistryManager);
		if (bedrockMapping == null)
		{
			LogManager.getLogger().warn("Block update contained block with no mapping {}", JavaBlocks.getName(blockChangeEntry.getBlock(), this.fabricRegistryManager));
		}

		boolean bedrockChanged = false;
		if (chunk.setBlock(blockX, blockY, blockZ, 0, bedrockMapping != null ? bedrockMapping.id : REPLACEMENT_BLOCK))
		{
			bedrockChanged = true;
			this.sendBlockUpdate(position, 0, chunk.getBlock(blockX, blockY, blockZ, 0));
		}

		if (chunk.setBlock(blockX, blockY, blockZ, 1, bedrockMapping != null && bedrockMapping.waterlogged ? BedrockBlocks.WATER : BedrockBlocks.AIR))
		{
			this.sendBlockUpdate(position, 1, chunk.getBlock(blockX, blockY, blockZ, 1));
		}

		chunk.setExtraData(blockX, blockY, blockZ, bedrockMapping != null ? bedrockMapping.extraData : null);

		if (bedrockChanged)    // required for pistons
		{
			JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping = bedrockMapping != null && bedrockMapping.blockEntity != null ? bedrockMapping.blockEntity : null;
			NbtMap bedrockBlockEntityData = blockEntityMapping != null ? BlockEntityTranslator.translateBlockEntity(blockEntityMapping, null) : null;
			chunk.setBlockEntity(blockX, blockY, blockZ, blockEntityMapping, bedrockBlockEntityData);
			if (bedrockBlockEntityData != null)
			{
				this.sendBlockEntity(position, chunk.getBlockEntity(blockX, blockY, blockZ));
			}
		}
	}

	public void onJavaBlockEntityUpdate(@NotNull ClientboundBlockEntityDataPacket clientboundBlockEntityDataPacket)
	{
		Vector3i position = clientboundBlockEntityDataPacket.getPosition();
		if (this.isOutOfBounds(position.getX(), position.getY(), position.getZ()))
		{
			return;
		}
		Chunk chunk = this.getChunkForBlock(position.getX(), position.getZ());
		int blockX = getChunkBlockOffset(position.getX());
		int blockY = position.getY();
		int blockZ = getChunkBlockOffset(position.getZ());

		JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping = chunk.getBlockEntityMapping(blockX, blockY, blockZ);
		if (blockEntityMapping == null)
		{
			LogManager.getLogger().debug("Ignoring block entity of type {}", clientboundBlockEntityDataPacket.getType());
		}
		NbtMap bedrockBlockEntityData = blockEntityMapping != null ? BlockEntityTranslator.translateBlockEntity(blockEntityMapping, new BlockEntityInfo(blockX, blockY, blockZ, clientboundBlockEntityDataPacket.getType(), clientboundBlockEntityDataPacket.getNbt())) : null;
		chunk.setBlockEntity(blockX, blockY, blockZ, blockEntityMapping, bedrockBlockEntityData);
		if (bedrockBlockEntityData != null)
		{
			this.sendBlockEntity(position, chunk.getBlockEntity(blockX, blockY, blockZ));
		}
	}

	public void onJavaBlockEvent(@NotNull ClientboundBlockEventPacket clientboundBlockEventPacket)
	{
		Vector3i position = clientboundBlockEventPacket.getPosition();
		if (this.isOutOfBounds(position.getX(), position.getY(), position.getZ()))
		{
			return;
		}
		Chunk chunk = this.getChunkForBlock(position.getX(), position.getZ());
		int blockX = getChunkBlockOffset(position.getX());
		int blockY = position.getY();
		int blockZ = getChunkBlockOffset(position.getZ());

		BlockValue blockValue = clientboundBlockEventPacket.getValue();
		BlockValueType blockValueType = clientboundBlockEventPacket.getType();

		if (blockValue instanceof NoteBlockValue)
		{
			JavaBlocks.BedrockMapping.NoteBlockExtraData noteBlockExtraData = (JavaBlocks.BedrockMapping.NoteBlockExtraData) chunk.getExtraData(blockX, blockY, blockZ);
			this.sendBlockEvent(position, 1, noteBlockExtraData.pitch);
		}
		else if (blockValue instanceof PistonValue)
		{
			NbtMap bedrockBlockEntityData = chunk.getBlockEntity(blockX, blockY, blockZ);
			if (bedrockBlockEntityData != null)
			{
				bedrockBlockEntityData = switch ((PistonValueType) blockValueType)
				{
					case PUSHING -> bedrockBlockEntityData.toBuilder()
							.putByte("State", (byte) 0)
							.putByte("NewState", (byte) 1)
							.putFloat("Progress", 0.0f)
							.putFloat("LastProgress", 0.0f)
							.build();
					case PULLING -> bedrockBlockEntityData.toBuilder()
							.putByte("State", (byte) 2)
							.putByte("NewState", (byte) 3)
							.putFloat("Progress", 1.0f)
							.putFloat("LastProgress", 1.0f)
							.build();
					case CANCELLED_MID_PUSH -> bedrockBlockEntityData.toBuilder()
							.putByte("State", (byte) 1)
							.putByte("NewState", (byte) 3)
							.putFloat("Progress", bedrockBlockEntityData.getFloat("Progress"))
							.putFloat("LastProgress", bedrockBlockEntityData.getFloat("Progress"))
							.build();
				};
				chunk.setBlockEntity(blockX, blockY, blockZ, chunk.getBlockEntityMapping(blockX, blockY, blockZ), bedrockBlockEntityData);
				this.sendBlockEntity(position, chunk.getBlockEntity(blockX, blockY, blockZ));

				if ((PistonValueType) blockValueType == PistonValueType.PUSHING)
				{
					// remove the block in front of the piston (Java edition replaces this with a minecraft:moving_piston block for the piston head, Bedrock edition does not use a separate minecraft:moving_block for the piston head block and expects that space to be air)
					Vector3i headPosition = switch ((int) BedrockBlocks.getState(chunk.getBlock(blockX, blockY, blockZ, 0)).get("facing_direction"))
					{
						case 0 -> position.add(0, -1, 0);
						case 1 -> position.add(0, 1, 0);
						case 2 -> position.add(0, 0, 1);
						case 3 -> position.add(0, 0, -1);
						case 4 -> position.add(1, 0, 0);
						case 5 -> position.add(-1, 0, 0);
						default -> position;
					};
					if (!this.isOutOfBounds(headPosition.getX(), headPosition.getY(), headPosition.getZ()))
					{
						Chunk headChunk = this.getChunkForBlock(headPosition.getX(), headPosition.getZ());
						if (headChunk.setBlock(getChunkBlockOffset(headPosition.getX()), headPosition.getY(), getChunkBlockOffset(headPosition.getZ()), 0, BedrockBlocks.AIR))
						{
							this.sendBlockUpdate(headPosition, 0, BedrockBlocks.AIR);
						}
						chunk.setExtraData(blockX, blockY, blockZ, null);
					}
				}
			}
		}
		else
		{
			LogManager.getLogger().debug("Ignoring block event of type {}", clientboundBlockEventPacket.getValue().getClass().getSimpleName());
		}
	}

	public void tick()
	{
		Arrays.stream(this.chunks).flatMap(Arrays::stream).forEach(chunk ->
		{
			Arrays.stream(chunk.getBlockEntities()).forEach(nbtMap ->
			{
				Vector3i position = Vector3i.from(nbtMap.getInt("x"), nbtMap.getInt("y"), nbtMap.getInt("z"));
				int blockX = getChunkBlockOffset(position.getX());
				int blockY = position.getY();
				int blockZ = getChunkBlockOffset(position.getZ());

				if (!chunk.getBlockEntityMapping(blockX, blockY, blockZ).type.equals("piston"))
				{
					return;
				}

				byte state = nbtMap.getByte("State");
				byte newState = nbtMap.getByte("NewState");
				if ((state & 1) == 0 && (newState & 1) == 0)
				{
					return;
				}
				nbtMap = switch (newState)
				{
					case 0 -> nbtMap.toBuilder()
							.putByte("State", newState)
							.putFloat("Progress", 0.0f)
							.putFloat("LastProgress", 0.0f)
							.build();
					case 2 -> nbtMap.toBuilder()
							.putFloat("State", newState)
							.putFloat("Progress", 1.0f)
							.putFloat("LastProgress", 1.0f)
							.build();
					case 1 -> nbtMap.toBuilder()
							.putByte("State", newState)
							.putByte("NewState", nbtMap.getFloat("Progress") == 0.5f ? (byte) 2 : newState)
							.putFloat("Progress", nbtMap.getFloat("Progress") == 0.5f ? 1.0f : 0.5f)
							.putFloat("LastProgress", nbtMap.getFloat("Progress"))
							.build();
					case 3 -> nbtMap.toBuilder()
							.putByte("State", newState)
							.putByte("NewState", nbtMap.getFloat("Progress") == 0.5f ? (byte) 0 : newState)
							.putFloat("Progress", nbtMap.getFloat("Progress") == 0.5f ? 0.0f : 0.5f)
							.putFloat("LastProgress", nbtMap.getFloat("Progress"))
							.build();
					default -> nbtMap;
				};

				chunk.setBlockEntity(blockX, blockY, blockZ, chunk.getBlockEntityMapping(blockX, blockY, blockZ), nbtMap);
				this.sendBlockEntity(position, chunk.getBlockEntity(blockX, blockY, blockZ));
			});
		});
	}

	private void sendBlockUpdate(Vector3i position, int layer, int blockId)
	{
		UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
		updateBlockPacket.setBlockPosition(position);
		updateBlockPacket.setDataLayer(layer);
		updateBlockPacket.setDefinition(Main.BLOCK_DEFINITION_REGISTRY.getDefinition(blockId));
		updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
		this.playerSession.sendBedrockPacket(updateBlockPacket);
	}

	private void sendBlockEntity(Vector3i position, NbtMap data)
	{
		BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
		blockEntityDataPacket.setBlockPosition(position);
		blockEntityDataPacket.setData(data);
		this.playerSession.sendBedrockPacket(blockEntityDataPacket);
	}

	private void sendBlockEvent(Vector3i position, int eventType, int eventData)
	{
		BlockEventPacket blockEventPacket = new BlockEventPacket();
		blockEventPacket.setBlockPosition(position);
		blockEventPacket.setEventType(eventType);
		blockEventPacket.setEventData(eventData);
		this.playerSession.sendBedrockPacket(blockEventPacket);
	}

	private boolean isOutOfBounds(int x, int y, int z)
	{
		return y < 0 || y >= 256 || x < -this.chunkRadius * 16 || x >= this.chunkRadius * 16 || z < -this.chunkRadius * 16 || z >= this.chunkRadius * 16;
	}

	private Chunk getChunk(int x, int z)
	{
		return this.chunks[x + this.chunkRadius][z + this.chunkRadius];
	}

	private Chunk getChunkForBlock(int x, int z)
	{
		return this.getChunk(x >= 0 ? (x / 16) : (-((-x - 1) / 16) - 1), z >= 0 ? (z / 16) : (-((-z - 1) / 16) - 1));
	}

	private static int getChunkBlockOffset(int pos)
	{
		return pos >= 0 ? pos % 16 : 15 - ((-pos - 1) % 16);
	}

	private static final class Chunk
	{
		private final int chunkX;
		private final int chunkZ;

		private final int[][] blocks = new int[2][16 * 16 * 256];
		private final int[] heightmap = new int[16 * 16];
		private final int[] biomes = new int[16 * 16];
		private final NbtMap[] blockEntities = new NbtMap[16 * 16 * 256];
		private final JavaBlocks.BedrockMapping.BlockEntity[] blockEntityMappings = new JavaBlocks.BedrockMapping.BlockEntity[16 * 16 * 256];
		private final JavaBlocks.BedrockMapping.ExtraData[] extraDatas = new JavaBlocks.BedrockMapping.ExtraData[16 * 16 * 256];

		public Chunk(int chunkX, int chunkZ)
		{
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;

			this.clear();
		}

		public void clear()
		{
			Arrays.fill(this.blocks[0], BedrockBlocks.AIR);
			Arrays.fill(this.blocks[1], BedrockBlocks.AIR);
			Arrays.fill(this.heightmap, 0);
			Arrays.fill(this.biomes, DEFAULT_BIOME);
			Arrays.fill(this.blockEntities, null);
			Arrays.fill(this.blockEntityMappings, null);
		}

		public int getBlock(int x, int y, int z, int layer)
		{
			return this.blocks[layer][(x * 16 + z) * 256 + y];
		}

		public boolean setBlock(int x, int y, int z, int layer, int block)
		{
			if (this.blocks[layer][(x * 16 + z) * 256 + y] != block)
			{
				this.blocks[layer][(x * 16 + z) * 256 + y] = block;

				if (layer == 0)
				{
					if (block != BedrockBlocks.AIR)
					{
						if (this.heightmap[x * 16 + z] < y)
						{
							this.heightmap[x * 16 + z] = y;
						}
					}
					else
					{
						if (this.heightmap[x * 16 + z] == y)
						{
							this.heightmap[x * 16 + z] = IntStream.range(0, y).filter(height -> this.blocks[layer][(x * 16 + z) * 256 + height] != BedrockBlocks.AIR).max().orElse(0);
						}
					}
				}

				return true;
			}
			else
			{
				return false;
			}
		}

		public int getHeightmap(int x, int z)
		{
			return this.heightmap[x * 16 + z];
		}

		@Nullable
		public NbtMap getBlockEntity(int x, int y, int z)
		{
			return this.blockEntities[(x * 16 + z) * 256 + y];
		}

		@Nullable
		public JavaBlocks.BedrockMapping.BlockEntity getBlockEntityMapping(int x, int y, int z)
		{
			return this.blockEntityMappings[(x * 16 + z) * 256 + y];
		}

		public void setBlockEntity(int x, int y, int z, @Nullable JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping, @Nullable NbtMap blockEntity)
		{
			if (blockEntity != null && blockEntityMapping == null)
			{
				throw new IllegalArgumentException();
			}
			this.blockEntityMappings[(x * 16 + z) * 256 + y] = blockEntityMapping;
			if (blockEntity != null)
			{
				this.blockEntities[(x * 16 + z) * 256 + y] = blockEntity.toBuilder().putInt("x", x + this.chunkX * 16).putInt("y", y).putInt("z", z + this.chunkZ * 16).putBoolean("isMovable", false).build();
			}
			else
			{
				this.blockEntities[(x * 16 + z) * 256 + y] = null;
			}
		}

		@NotNull
		public NbtMap[] getBlockEntities()
		{
			return Arrays.stream(this.blockEntities).filter(blockEntity -> blockEntity != null).toArray(NbtMap[]::new);
		}

		@Nullable
		public JavaBlocks.BedrockMapping.ExtraData getExtraData(int x, int y, int z)
		{
			return this.extraDatas[(x * 16 + z) * 256 + y];
		}

		public void setExtraData(int x, int y, int z, @Nullable JavaBlocks.BedrockMapping.ExtraData extraData)
		{
			this.extraDatas[(x * 16 + z) * 256 + y] = extraData;
		}

		public int getBiome(int x, int z)
		{
			return this.biomes[x * 16 + z];
		}

		public void setBiome(int x, int z, int biome)
		{
			this.biomes[x * 16 + z] = biome;
		}
	}
}