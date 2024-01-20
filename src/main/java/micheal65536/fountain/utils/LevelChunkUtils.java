package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.chunk.BitStorage;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.Palette;
import com.github.steveice10.mc.protocol.data.game.chunk.palette.SingletonPalette;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.protocol.bedrock.packet.LevelChunkPacket;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.registry.BedrockBiomes;
import micheal65536.fountain.registry.BedrockBlocks;
import micheal65536.fountain.registry.JavaBlocks;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class LevelChunkUtils
{
	@NotNull
	public static LevelChunkPacket translateLevelChunk(@NotNull ClientboundLevelChunkWithLightPacket clientboundLevelChunkWithLightPacket, @NotNull HashMap<Integer, String> javaBiomesMap, @NotNull MinecraftCodecHelper minecraftCodecHelper)
	{
		try
		{
			BedrockChunk[] bedrockChunks = new BedrockChunk[16];
			HashSet<Integer> alreadyNotifiedMissingBlocks = new HashSet<>();
			HashSet<String> alreadyNotifiedMissingBiomes = new HashSet<>();
			ByteBuf byteBuf = Unpooled.wrappedBuffer(clientboundLevelChunkWithLightPacket.getChunkData());
			int[][] heightmap = new int[16][16];
			int[][][] biomes = new int[4][4][64];
			for (int[][] x : biomes)
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

				BedrockChunk bedrockChunk = new BedrockChunk();

				Palette javaBlockPalette = chunkSection.getChunkData().getPalette();
				BitStorage javaBlockData = chunkSection.getChunkData().getStorage();
				for (int yzx = 0; yzx < 4096; yzx++)
				{
					int javaId;
					if (javaBlockPalette instanceof SingletonPalette)
					{
						javaId = javaBlockPalette.idToState(0);
					}
					else
					{
						javaId = javaBlockPalette.idToState(javaBlockData.get(yzx));
					}

					int bedrockId = JavaBlocks.getBedrockId(javaId);
					if (bedrockId == -1)
					{
						if (alreadyNotifiedMissingBlocks.add(javaId))
						{
							LogManager.getLogger().warn("Chunk contained block with no mapping " + JavaBlocks.getName(javaId));
						}
						bedrockId = BedrockBlocks.AIR;
					}
					bedrockChunk.set(YZXToXZY(yzx), bedrockId, JavaBlocks.isWaterlogged(javaId) ? BedrockBlocks.WATER : -1);

					if (bedrockId != BedrockBlocks.AIR)
					{
						int x = yzx & 0x00F;
						int z = (yzx & 0x0F0) >> 4;
						int y = (yzx & 0xF00) >> 8;
						y = y + chunkY * 16;
						if (y > heightmap[x][z])
						{
							heightmap[x][z] = y;
						}
					}

					// TODO: flower pots, pistons, cauldrons, lecterns
				}

				for (BlockEntityInfo blockEntityInfo : clientboundLevelChunkWithLightPacket.getBlockEntities())
				{
					// TODO: block entities
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
						LogManager.getLogger().warn("Java server sent bad biome data");
						bedrockId = -1;
					}
					else
					{
						bedrockId = BedrockBiomes.getId(biomeName);
						if (bedrockId == -1)
						{
							if (alreadyNotifiedMissingBiomes.add(biomeName))
							{
								LogManager.getLogger().warn("Chunk contained biome with no mapping " + biomeName);
							}
						}
					}

					int x = yzx & 0b000011;
					int z = (yzx & 0b001100) >> 2;
					int y = (yzx & 0b110000) >> 4;
					y = y + chunkY * 4;
					biomes[x][z][y] = bedrockId;
				}

				bedrockChunks[chunkY] = bedrockChunk;
			}

			byte[] data;
			byteBuf = ByteBufAllocator.DEFAULT.buffer();
			try
			{
				byteBuf.writeByte(bedrockChunks.length);
				for (BedrockChunk chunk : bedrockChunks)
				{
					chunk.write(byteBuf);
				}

				for (int x = 0; x < 16; x++)
				{
					for (int z = 0; z < 16; z++)
					{
						byteBuf.writeShortLE(heightmap[x][z]);
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
							int biomeId = biomes[x / 4][z / 4][y / 4];
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
							LogManager.getLogger().warn("Could not determine biome for " + x + ", " + z + " (" + biomeCounts.size() + " candidates)");
							mostCommonBiomeId = 4;
						}

						byteBuf.writeByte(mostCommonBiomeId);
					}
				}

				VarInts.writeUnsignedInt(byteBuf, 0);

				// TODO: block entities

				data = new byte[byteBuf.readableBytes()];
				byteBuf.readBytes(data);
			}
			finally
			{
				byteBuf.release();
			}

			LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
			levelChunkPacket.setChunkX(clientboundLevelChunkWithLightPacket.getX());
			levelChunkPacket.setChunkZ(clientboundLevelChunkWithLightPacket.getZ());
			levelChunkPacket.setData(Unpooled.wrappedBuffer(data));
			return levelChunkPacket;
		}
		catch (IOException exception)
		{
			exception.printStackTrace();
			return makeEmpty(clientboundLevelChunkWithLightPacket.getX(), clientboundLevelChunkWithLightPacket.getZ());
		}
	}

	@NotNull
	public static LevelChunkPacket makeEmpty(int chunkX, int chunkZ)
	{
		LevelChunkPacket levelChunkPacket = new LevelChunkPacket();
		levelChunkPacket.setChunkX(chunkX);
		levelChunkPacket.setChunkZ(chunkZ);
		levelChunkPacket.setData(Unpooled.wrappedBuffer(new byte[0]));
		return levelChunkPacket;
	}

	private static int YZXToXZY(int yzx)
	{
		return ((yzx & 0xF00) >> 8) | ((yzx & 0x0F0) >> 0) | ((yzx & 0x00F) << 8);
	}

	private static final class BedrockChunk
	{
		public final LinkedHashMap<Integer, Integer>[] palettes = new LinkedHashMap[]{new LinkedHashMap<>(), new LinkedHashMap<>()};
		public final int[][] blocks = new int[2][4096];

		public BedrockChunk()
		{
			this.palettes[0].put(BedrockBlocks.AIR, 0);
			this.palettes[1].put(BedrockBlocks.AIR, 0);
		}

		public void set(int xzy, int level0BlockId, int level1BlockId)
		{
			int index = this.palettes[0].getOrDefault(level0BlockId, -1);
			if (index == -1)
			{
				index = this.palettes[0].size();
				this.palettes[0].put(level0BlockId, index);
			}
			this.blocks[0][xzy] = index;

			if (level1BlockId != -1)
			{
				index = this.palettes[1].getOrDefault(level1BlockId, -1);
				if (index == -1)
				{
					index = this.palettes[1].size();
					this.palettes[1].put(level1BlockId, index);
				}
				this.blocks[1][xzy] = index;
			}
		}

		public void write(@NotNull ByteBuf byteBuf)
		{
			byteBuf.writeByte(8);

			int maxLayer = this.palettes[1].size() > 1 ? 2 : 1;
			byteBuf.writeByte(maxLayer);
			for (int layer = 0; layer < maxLayer; layer++)
			{
				int bits = 16;
				int paletteSize = this.palettes[layer].size();
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
						value = value | (this.blocks[layer][index + index1] << (bits * index1));
					}
					byteBuf.writeIntLE(value);
				}

				VarInts.writeInt(byteBuf, this.palettes[layer].size());
				for (int id : this.palettes[layer].keySet())
				{
					VarInts.writeInt(byteBuf, id);
				}
			}
		}
	}
}