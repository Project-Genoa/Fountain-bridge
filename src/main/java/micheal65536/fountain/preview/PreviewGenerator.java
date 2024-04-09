package micheal65536.fountain.preview;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;

import micheal65536.fountain.registry.BedrockBlocks;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.stream.IntStream;

public class PreviewGenerator
{
	private static final int CHUNK_RADIUS = 2;

	public static void main(String[] args)
	{
		try
		{
			ServerDataZip serverDataZip = ServerDataZip.read(System.in);

			LinkedList<Chunk> chunks = new LinkedList<>();
			for (int chunkX = -CHUNK_RADIUS; chunkX < CHUNK_RADIUS; chunkX++)
			{
				for (int chunkZ = -CHUNK_RADIUS; chunkZ < CHUNK_RADIUS; chunkZ++)
				{
					Chunk chunk = Chunk.read(serverDataZip.getChunkNBT(chunkX, chunkZ));
					if (chunk == null)
					{
						LogManager.getLogger().error("Could not convert chunk {}, {}", chunkX, chunkZ);
					}
					else
					{
						chunks.add(chunk);
					}
				}
			}

			PreviewModel.SubChunk[] subChunks = chunks.stream()
					.flatMap(chunk ->
					{
						return IntStream.range(0, 16)
								.mapToObj(subchunkY ->
								{
									LinkedHashMap<Integer, Integer> palette = new LinkedHashMap<>();
									int[] blocks = new int[4096];
									for (int x = 0; x < 16; x++)
									{
										for (int y = 0; y < 16; y++)
										{
											for (int z = 0; z < 16; z++)
											{
												int blockId = chunk.blocks[(x * 256 + (y + subchunkY * 16)) * 16 + z];
												blocks[(x * 16 + y) * 16 + z] = palette.computeIfAbsent(blockId, blockId1 -> palette.size());
											}
										}
									}

									if (palette.size() == 1 && palette.containsKey(BedrockBlocks.AIR))
									{
										return null;
									}
									else
									{
										return new PreviewModel.SubChunk(
												new PreviewModel.Position(chunk.chunkX, subchunkY, chunk.chunkZ),
												palette.keySet().stream()
														.map(blockId ->
														{
															String name = BedrockBlocks.getName(blockId);
															if (name == null)
															{
																throw new AssertionError();
															}
															int data = 0;
															while (blockId - data - 1 >= 0 && name.equals(BedrockBlocks.getName(blockId - data - 1)))
															{
																data++;
															}
															return new PreviewModel.SubChunk.PaletteEntry(name, data);
														})
														.toArray(PreviewModel.SubChunk.PaletteEntry[]::new),
												blocks
										);
									}
								})
								.filter(subChunk -> subChunk != null);
					})
					.toArray(PreviewModel.SubChunk[]::new);

			// block entities seem to not be used by the client when rendering the preview anyway?
			PreviewModel.BlockEntity[] blockEntities = chunks.stream()
					.flatMap(chunk -> Arrays.stream(chunk.blockEntities))
					.filter(blockEntity -> blockEntity != null)
					.map(blockEntity -> new PreviewModel.BlockEntity(
							switch (blockEntity.getString("id"))
							{
								case "Bed" -> 27;
								case "PistonArm" -> 18;
								default ->
								{
									LogManager.getLogger().warn("No block entity type code mapping for {}", blockEntity.getString("id"));
									yield -1;
								}
							},
							new PreviewModel.Position(blockEntity.getInt("x"), blockEntity.getInt("y"), blockEntity.getInt("z")),
							JsonNbtConverter.convert(blockEntity)
					))
					.filter(blockEntity -> blockEntity.type() != -1)
					.toArray(PreviewModel.BlockEntity[]::new);

			// TODO: entities

			PreviewModel previewModel = new PreviewModel(
					1,
					false,
					subChunks,
					blockEntities,
					new PreviewModel.Entity[0]
			);

			System.out.print(new Gson().newBuilder().serializeNulls().create().toJson(previewModel));
		}
		catch (IOException exception)
		{
			// TODO
		}
	}
}