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

import micheal65536.fountain.palette.BedrockBlockPalette;
import micheal65536.fountain.palette.JavaBlockTranslator;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class LevelChunkUtils
{
	@NotNull
	public static LevelChunkPacket translateLevelChunk(@NotNull ClientboundLevelChunkWithLightPacket clientboundLevelChunkWithLightPacket, @NotNull HashMap<Integer, String> javaBiomesMap, @NotNull MinecraftCodecHelper minecraftCodecHelper)
	{
		try
		{
			BedrockChunk[] bedrockChunks = new BedrockChunk[16];
			ByteBuf byteBuf = Unpooled.wrappedBuffer(clientboundLevelChunkWithLightPacket.getChunkData());
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
					int javaBlockId;
					if (javaBlockPalette instanceof SingletonPalette)
					{
						javaBlockId = javaBlockPalette.idToState(0);
					}
					else
					{
						javaBlockId = javaBlockPalette.idToState(javaBlockData.get(yzx));
					}

					int bedrockId = JavaBlockTranslator.getBedrockBlockId(javaBlockId);
					if (bedrockId == -1)
					{
						LogManager.getLogger().warn("Chunk contained block with no mapping " + JavaBlockTranslator.getUnmappedBlockName(javaBlockId));
						bedrockId = BedrockBlockPalette.AIR;
					}
					bedrockChunk.set(YZXToXZY(yzx), bedrockId, JavaBlockTranslator.isWaterlogged(javaBlockId) ? BedrockBlockPalette.WATER : -1);

					// TODO: flower pots, pistons, cauldrons, lecterns
				}

				for (BlockEntityInfo blockEntityInfo : clientboundLevelChunkWithLightPacket.getBlockEntities())
				{
					// TODO: block entities
				}

				// TODO: biomes

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

				// TODO: biomes
				for (int i = 0; i < 256; i++)
				{
					byteBuf.writeByte(4);
				}

				byteBuf.writeByte(0);

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
			this.palettes[0].put(BedrockBlockPalette.AIR, 0);
			this.palettes[1].put(BedrockBlockPalette.AIR, 0);
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