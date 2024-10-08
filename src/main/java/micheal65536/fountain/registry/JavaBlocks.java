package micheal65536.fountain.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.DataFile;
import micheal65536.fountain.utils.FabricRegistryManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class JavaBlocks
{
	private static final HashMap<Integer, String> map = new HashMap<>();
	private static final HashMap<String, LinkedList<String>> nonVanillaStatesList = new HashMap<>();

	private static final HashMap<Integer, BedrockMapping> bedrockMap = new HashMap<>();
	private static final HashMap<String, BedrockMapping> bedrockMapByName = new HashMap<>();
	private static final HashMap<String, BedrockMapping> bedrockNonVanillaMap = new HashMap<>();

	static
	{
		DataFile.load("registry/blocks_java.json", root ->
		{
			for (JsonElement element : root.getAsJsonArray())
			{
				int id = element.getAsJsonObject().get("id").getAsInt();
				String name = element.getAsJsonObject().get("name").getAsString();
				if (map.put(id, name) != null)
				{
					LogManager.getLogger().warn("Duplicate Java block ID {}", id);
				}

				try
				{
					BedrockMapping bedrockMapping = readBedrockMapping(element.getAsJsonObject().get("bedrock").getAsJsonObject(), root.getAsJsonArray());
					if (bedrockMapping == null)
					{
						continue;
					}
					bedrockMap.put(id, bedrockMapping);
					bedrockMapByName.put(name, bedrockMapping);
				}
				catch (BedrockMappingFailException exception)
				{
					LogManager.getLogger().warn("Cannot find Bedrock block for Java block {}: {}", name, exception.getMessage());
				}
			}
		});

		DataFile.load("registry/blocks_java_nonvanilla.json", root ->
		{
			for (JsonElement element : root.getAsJsonArray())
			{
				String baseName = element.getAsJsonObject().get("name").getAsString();

				LinkedList<String> stateNames = new LinkedList<>();
				JsonArray statesArray = element.getAsJsonObject().get("states").getAsJsonArray();
				for (JsonElement stateElement : statesArray)
				{
					String stateName = stateElement.getAsJsonObject().get("name").getAsString();
					stateNames.add(stateName);

					String name = baseName + stateName;

					try
					{
						BedrockMapping bedrockMapping = readBedrockMapping(stateElement.getAsJsonObject().get("bedrock").getAsJsonObject(), null);
						if (bedrockMapping == null)
						{
							continue;
						}
						bedrockNonVanillaMap.put(name, bedrockMapping);
					}
					catch (BedrockMappingFailException exception)
					{
						LogManager.getLogger().warn("Cannot find Bedrock block for Java block {}: {}", name, exception.getMessage());
					}
				}

				if (nonVanillaStatesList.put(baseName, stateNames) != null)
				{
					LogManager.getLogger().warn("Duplicate Java non-vanilla block name {}", baseName);
				}
			}
		});
	}

	@Nullable
	private static BedrockMapping readBedrockMapping(@NotNull JsonObject bedrockMappingObject, @Nullable JsonArray javaBlocksArray) throws BedrockMappingFailException
	{
		if (bedrockMappingObject.has("ignore") && bedrockMappingObject.get("ignore").getAsBoolean())
		{
			return null;
		}

		String name = bedrockMappingObject.get("name").getAsString();

		HashMap<String, Object> state = new HashMap<>();
		if (bedrockMappingObject.has("state"))
		{
			JsonObject stateObject = bedrockMappingObject.get("state").getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : stateObject.entrySet())
			{
				JsonElement stateElement = entry.getValue();
				if (stateElement.getAsJsonPrimitive().isString())
				{
					state.put(entry.getKey(), stateElement.getAsString());
				}
				else if (stateElement.getAsJsonPrimitive().isBoolean())
				{
					state.put(entry.getKey(), stateElement.getAsBoolean() ? 1 : 0);
				}
				else
				{
					state.put(entry.getKey(), stateElement.getAsInt());
				}
			}
		}

		int id = BedrockBlocks.getId(name, state);
		if (id == -1)
		{
			throw new BedrockMappingFailException("Cannot find Bedrock block with provided name and state");
		}

		boolean waterlogged = bedrockMappingObject.has("waterlogged") ? bedrockMappingObject.get("waterlogged").getAsBoolean() : false;

		BedrockMapping.BlockEntity blockEntity = null;
		if (bedrockMappingObject.has("block_entity"))
		{
			JsonObject blockEntityObject = bedrockMappingObject.get("block_entity").getAsJsonObject();
			String type = blockEntityObject.get("type").getAsString();
			switch (type)
			{
				case "bed":
				{
					String color = blockEntityObject.get("color").getAsString();
					blockEntity = new BedrockMapping.BedBlockEntity(type, color);
				}
				break;
				case "flower_pot":
				{
					NbtMap contents = null;
					if (blockEntityObject.has("contents") && !blockEntityObject.get("contents").isJsonNull())
					{
						String contentsName = blockEntityObject.get("contents").getAsString();
						if (javaBlocksArray != null)
						{
							contents = javaBlocksArray.asList().stream()
									.filter(element -> element.getAsJsonObject().get("name").getAsString().equals(contentsName))
									.map(element -> element.getAsJsonObject().get("bedrock").getAsJsonObject())
									.filter(element -> !element.has("ignore") || !element.get("ignore").getAsBoolean())
									.findFirst().map(element ->
									{
										NbtMapBuilder builder = NbtMap.builder();
										builder.putString("name", element.get("name").getAsString());
										if (element.has("state"))
										{
											NbtMapBuilder stateBuilder = NbtMap.builder();
											element.get("state").getAsJsonObject().asMap().forEach((key, stateElement) ->
											{
												if (stateElement.getAsJsonPrimitive().isString())
												{
													stateBuilder.putString(key, stateElement.getAsString());
												}
												else if (stateElement.getAsJsonPrimitive().isBoolean())
												{
													stateBuilder.putInt(key, stateElement.getAsBoolean() ? 1 : 0);
												}
												else
												{
													stateBuilder.putInt(key, stateElement.getAsInt());
												}
											});
											builder.putCompound("states", stateBuilder.build());
										}
										return builder.build();
									}).orElse(null);
						}
						if (contents == null)
						{
							throw new BedrockMappingFailException("Could not find contents for flower pot");
						}
					}
					blockEntity = new BedrockMapping.FlowerPotBlockEntity(type, contents);
				}
				break;
				case "moving_block":
				{
					blockEntity = new BedrockMapping.BlockEntity(type);
				}
				break;
				case "piston":
				{
					boolean sticky = blockEntityObject.get("sticky").getAsBoolean();
					boolean extended = blockEntityObject.get("extended").getAsBoolean();
					blockEntity = new BedrockMapping.PistonBlockEntity(type, sticky, extended);
				}
				break;
			}
		}

		BedrockMapping.ExtraData extraData = null;
		if (bedrockMappingObject.has("extra_data"))
		{
			JsonObject extraDataObject = bedrockMappingObject.get("extra_data").getAsJsonObject();
			String type = extraDataObject.get("type").getAsString();
			switch (type)
			{
				case "note_block":
				{
					int pitch = extraDataObject.get("pitch").getAsInt();
					extraData = new BedrockMapping.NoteBlockExtraData(pitch);
				}
				break;
			}
		}

		return new BedrockMapping(id, waterlogged, blockEntity, extraData);
	}

	private static class BedrockMappingFailException extends Exception
	{
		public BedrockMappingFailException(String message)
		{
			super(message);
		}
	}

	public static int getMaxVanillaBlockId()
	{
		return map.keySet().stream().max(Integer::compareTo).orElse(-1);
	}

	public static String[] getStatesForNonVanillaBlock(@NotNull String name)
	{
		LinkedList<String> states = nonVanillaStatesList.getOrDefault(name, null);
		return states != null ? states.toArray(new String[0]) : null;
	}

	@Deprecated
	@Nullable
	public static String getName(int id)
	{
		return getName(id, null);
	}

	@Deprecated
	public static BedrockMapping getBedrockMapping(int javaId)
	{
		return getBedrockMapping(javaId, null);
	}

	@Nullable
	public static String getName(int id, @Nullable FabricRegistryManager fabricRegistryManager)
	{
		String name = map.getOrDefault(id, null);
		if (name == null && fabricRegistryManager != null)
		{
			name = fabricRegistryManager.getBlockName(id);
		}
		return name;
	}

	@Nullable
	public static BedrockMapping getBedrockMapping(int javaId, @Nullable FabricRegistryManager fabricRegistryManager)
	{
		BedrockMapping bedrockMapping = bedrockMap.getOrDefault(javaId, null);
		if (bedrockMapping == null && fabricRegistryManager != null)
		{
			String fabricName = fabricRegistryManager.getBlockName(javaId);
			if (fabricName != null)
			{
				bedrockMapping = bedrockNonVanillaMap.getOrDefault(fabricName, null);
			}
		}
		return bedrockMapping;
	}

	@Nullable
	public static BedrockMapping getBedrockMapping(@NotNull String javaName)
	{
		BedrockMapping bedrockMapping = bedrockMapByName.getOrDefault(javaName, null);
		if (bedrockMapping == null)
		{
			bedrockMapping = bedrockNonVanillaMap.getOrDefault(javaName, null);
		}
		return bedrockMapping;
	}

	public static final class BedrockMapping
	{
		public final int id;
		public final boolean waterlogged;
		@Nullable
		public final BlockEntity blockEntity;
		@Nullable
		public final ExtraData extraData;

		private BedrockMapping(int id, boolean waterlogged, @Nullable BlockEntity blockEntity, @Nullable ExtraData extraData)
		{
			this.id = id;
			this.waterlogged = waterlogged;
			this.blockEntity = blockEntity;
			this.extraData = extraData;
		}

		public static class BlockEntity
		{
			@NotNull
			public final String type;

			private BlockEntity(@NotNull String type)
			{
				this.type = type;
			}
		}

		public static class BedBlockEntity extends BlockEntity
		{
			@NotNull
			public final String color;

			private BedBlockEntity(@NotNull String type, @NotNull String color)
			{
				super(type);
				this.color = color;
			}
		}

		public static class FlowerPotBlockEntity extends BlockEntity
		{
			@Nullable
			public final NbtMap contents;

			private FlowerPotBlockEntity(@NotNull String type, @Nullable NbtMap contents)
			{
				super(type);
				this.contents = contents;
			}
		}

		public static class PistonBlockEntity extends BlockEntity
		{
			public final boolean sticky;
			public final boolean extended;

			private PistonBlockEntity(@NotNull String type, boolean sticky, boolean extended)
			{
				super(type);
				this.sticky = sticky;
				this.extended = extended;
			}
		}

		public static abstract class ExtraData
		{
			private ExtraData()
			{
				// empty
			}
		}

		public static class NoteBlockExtraData extends ExtraData
		{
			public final int pitch;

			private NoteBlockExtraData(int pitch)
			{
				this.pitch = pitch;
			}
		}
	}
}