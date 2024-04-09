package micheal65536.fountain.preview;

import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.LongArrayTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.nbt.NbtMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.registry.BedrockBlocks;
import micheal65536.fountain.registry.JavaBlocks;
import micheal65536.fountain.utils.BlockEntityTranslator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

class Chunk
{
	@Nullable
	public static Chunk read(@NotNull CompoundTag chunkTag)
	{
		try
		{
			return new Chunk(chunkTag);
		}
		catch (Exception exception)
		{
			LogManager.getLogger().error("Could not read chunk", exception);
			return null;
		}
	}

	public final int chunkX;
	public final int chunkZ;

	public final int[] blocks = new int[16 * 256 * 16];
	public final NbtMap[] blockEntities = new NbtMap[16 * 256 * 16];

	private Chunk(@NotNull CompoundTag chunkTag) throws Exception
	{
		this.chunkX = (int) chunkTag.get("xPos").getValue();
		this.chunkZ = (int) chunkTag.get("zPos").getValue();

		JavaBlocks.BedrockMapping.BlockEntity[] blockEntityMappings = new JavaBlocks.BedrockMapping.BlockEntity[16 * 256 * 16];
		JavaBlocks.BedrockMapping.ExtraData[] extraDatas = new JavaBlocks.BedrockMapping.ExtraData[16 * 256 * 16];

		Arrays.fill(this.blocks, BedrockBlocks.AIR);
		Arrays.fill(this.blockEntities, null);
		Arrays.fill(blockEntityMappings, null);
		Arrays.fill(extraDatas, null);

		HashSet<String> alreadyNotifiedMissingBlocks = new HashSet<>();
		for (int subchunkY = 0; subchunkY < 16; subchunkY++)
		{
			int sectionIndex = subchunkY + 4 + 1; // Java world height starts at -64, plus one section for bottommost lighting
			CompoundTag sectionTag = ((ListTag) chunkTag.get("sections")).get(sectionIndex);

			CompoundTag blockStatesTag = sectionTag.get("block_states");

			ListTag paletteTag = blockStatesTag.get("palette");
			ArrayList<String> javaPalette = new ArrayList<>(paletteTag.size());
			for (Tag paletteEntryTag : paletteTag)
			{
				javaPalette.add(readPaletteEntry((CompoundTag) paletteEntryTag));
			}

			int[] javaBlocks;
			if (javaPalette.size() == 0)
			{
				throw new IOException("Chunk section has empty palette");
			}
			if (!blockStatesTag.contains("data"))
			{
				if (javaPalette.size() > 1)
				{
					throw new IOException("Chunk section has palette with more than one entry and no data");
				}

				javaBlocks = new int[4096];
				Arrays.fill(javaBlocks, 0);
			}
			else
			{
				javaBlocks = readBitArray(blockStatesTag.get("data"), javaPalette.size());
			}

			for (int x = 0; x < 16; x++)
			{
				for (int y = 0; y < 16; y++)
				{
					for (int z = 0; z < 16; z++)
					{
						String javaName = javaPalette.get(javaBlocks[(y * 16 + z) * 16 + x]);

						JavaBlocks.BedrockMapping bedrockMapping = JavaBlocks.getBedrockMapping(javaName);
						if (bedrockMapping == null)
						{
							if (alreadyNotifiedMissingBlocks.add(javaName))
							{
								LogManager.getLogger().warn("Chunk contained block with no mapping {}", javaName);
							}
						}

						// TODO: how to handle waterlogged blocks???
						int bedrockId = bedrockMapping != null ? bedrockMapping.id : BedrockBlocks.AIR;
						this.blocks[(x * 256 + (y + subchunkY * 16)) * 16 + z] = bedrockId;

						JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping = bedrockMapping != null && bedrockMapping.blockEntity != null ? bedrockMapping.blockEntity : null;
						NbtMap bedrockBlockEntityData = blockEntityMapping != null ? BlockEntityTranslator.translateBlockEntity(blockEntityMapping, null) : null;
						if (bedrockBlockEntityData != null)
						{
							bedrockBlockEntityData = bedrockBlockEntityData.toBuilder().putInt("x", x + this.chunkX * 16).putInt("y", y + subchunkY * 16).putInt("z", z + this.chunkZ * 16).putBoolean("isMovable", false).build();
						}
						this.blockEntities[(x * 256 + (y + subchunkY * 16)) * 16 + z] = bedrockBlockEntityData;
						blockEntityMappings[(x * 256 + (y + subchunkY * 16)) * 16 + z] = blockEntityMapping;

						extraDatas[(x * 256 + (y + subchunkY * 16)) * 16 + z] = bedrockMapping != null ? bedrockMapping.extraData : null;
					}
				}
			}
		}

		for (Tag blockEntityTag : (ListTag) chunkTag.get("block_entities"))
		{
			CompoundTag blockEntityCompoundTag = (CompoundTag) blockEntityTag;
			int x = getChunkBlockOffset(((IntTag) blockEntityCompoundTag.get("x")).getValue());
			int y = ((IntTag) blockEntityCompoundTag.get("y")).getValue();
			int z = getChunkBlockOffset(((IntTag) blockEntityCompoundTag.get("z")).getValue());
			String type = ((StringTag) blockEntityCompoundTag.get("id")).getValue();
			BlockEntityInfo blockEntityInfo = new BlockEntityInfo(x, y, z, BlockEntityType.FURNACE, blockEntityCompoundTag);    // TODO: use proper type (currently this doesn't matter for any of our translator implementations)

			JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping = blockEntityMappings[(x * 256 + y) * 16 + z];
			if (blockEntityMapping == null)
			{
				LogManager.getLogger().debug("Ignoring block entity of type {}", type);
			}
			NbtMap bedrockBlockEntityData = blockEntityMapping != null ? BlockEntityTranslator.translateBlockEntity(blockEntityMapping, blockEntityInfo) : null;
			if (bedrockBlockEntityData != null)
			{
				bedrockBlockEntityData = bedrockBlockEntityData.toBuilder().putInt("x", x + this.chunkX * 16).putInt("y", y).putInt("z", z + this.chunkZ * 16).putBoolean("isMovable", false).build();
			}
			this.blockEntities[(x * 256 + y) * 16 + z] = bedrockBlockEntityData;
		}
	}

	// TODO: this relies on the state tags in the block names in the Java blocks registry matching the actual server names/values and to be sorted in alphabetical order, should verify/ensure that this is the case
	@NotNull
	private static String readPaletteEntry(@NotNull CompoundTag paletteEntryTag) throws Exception
	{
		String name = ((StringTag) paletteEntryTag.get("Name")).getValue();

		LinkedList<String> properties = new LinkedList<>();
		if (paletteEntryTag.contains("Properties"))
		{
			for (Tag propertyTag : (CompoundTag) paletteEntryTag.get("Properties"))
			{
				properties.add(propertyTag.getName() + "=" + propertyTag.getValue());
			}
		}

		if (properties.size() > 0)
		{
			name = name + "[" + String.join(",", properties.stream().sorted(String::compareTo).toArray(String[]::new)) + "]";
		}

		return name;
	}

	private static int[] readBitArray(@NotNull LongArrayTag longArrayTag, int maxValue) throws Exception
	{
		int[] out = new int[4096];
		int outIndex = 0;

		long[] in = longArrayTag.getValue();
		int inIndex = 0;
		int inSubIndex = 0;

		int bits = 64;
		for (int bits1 = 4; bits1 <= 64; bits1++)
		{
			if (maxValue <= (1 << bits1))
			{
				bits = bits1;
				break;
			}
		}
		int valuesPerLong = 64 / bits;

		long currentIn = in[inIndex++];
		inSubIndex = 0;
		while (outIndex < out.length)
		{
			if (inSubIndex >= valuesPerLong)
			{
				currentIn = in[inIndex++];
				inSubIndex = 0;
			}
			long value = (currentIn >> ((inSubIndex++) * bits)) & ((1 << bits) - 1);
			out[outIndex++] = (int) value;
		}

		return out;
	}

	private static int getChunkBlockOffset(int pos)
	{
		return pos >= 0 ? pos % 16 : 15 - ((-pos - 1) % 16);
	}
}