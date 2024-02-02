package micheal65536.fountain.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.utils.FabricRegistryManager;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class JavaBlocks
{
	private static final HashMap<Integer, String> map = new HashMap<>();
	private static final HashMap<String, LinkedList<String>> nonVanillaStatesList = new HashMap<>();

	private static final HashMap<Integer, Integer> bedrockIdMap = new HashMap<>();
	private static final HashMap<Integer, Boolean> bedrockWaterloggedMap = new HashMap<>();
	private static final HashMap<String, Integer> bedrockNonVanillaIdMap = new HashMap<>();
	private static final HashMap<String, Boolean> bedrockNonVanillaWaterloggedMap = new HashMap<>();

	static
	{
		try
		{
			try (FileReader fileReader = new FileReader("data/blocks_java.json"))
			{
				JsonElement root = JsonParser.parseReader(fileReader);
				for (JsonElement element : root.getAsJsonArray())
				{
					int id = element.getAsJsonObject().get("id").getAsInt();
					String name = element.getAsJsonObject().get("name").getAsString();
					if (map.put(id, name) != null)
					{
						LogManager.getLogger().warn("Duplicate Java block ID " + id);
					}

					JsonObject bedrockMapping = element.getAsJsonObject().get("bedrock").getAsJsonObject();
					if (bedrockMapping.has("ignore") && bedrockMapping.get("ignore").getAsBoolean())
					{
						LogManager.getLogger().debug("Ignoring Java block " + name);
						continue;
					}
					String bedrockName = bedrockMapping.get("name").getAsString();
					HashMap<String, Object> bedrockState = new HashMap<>();
					if (bedrockMapping.has("state"))
					{
						JsonObject bedrockStateObject = bedrockMapping.get("state").getAsJsonObject();
						for (Map.Entry<String, JsonElement> entry : bedrockStateObject.entrySet())
						{
							JsonElement bedrockStateElement = entry.getValue();
							if (bedrockStateElement.getAsJsonPrimitive().isString())
							{
								bedrockState.put(entry.getKey(), bedrockStateElement.getAsString());
							}
							else if (bedrockStateElement.getAsJsonPrimitive().isBoolean())
							{
								bedrockState.put(entry.getKey(), bedrockStateElement.getAsBoolean() ? 1 : 0);
							}
							else
							{
								bedrockState.put(entry.getKey(), bedrockStateElement.getAsInt());
							}
						}
					}
					int bedrockId = BedrockBlocks.getId(bedrockName, bedrockState);
					if (bedrockId == -1)
					{
						LogManager.getLogger().warn("Cannot find Bedrock block for Java block " + name);
					}
					else
					{
						bedrockIdMap.put(id, bedrockId);

						boolean waterlogged = name.contains("waterlogged=true") || bedrockName.equals("minecraft:bubble_column") || bedrockName.equals("minecraft:kelp") || bedrockName.equals("minecraft:seagrass");    // TODO: consider putting this in data file
						bedrockWaterloggedMap.put(id, waterlogged);
					}
				}
			}

			try (FileReader fileReader = new FileReader("data/blocks_java_nonvanilla.json"))
			{
				JsonElement root = JsonParser.parseReader(fileReader);
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

						JsonObject bedrockMapping = stateElement.getAsJsonObject().get("bedrock").getAsJsonObject();
						if (bedrockMapping.has("ignore") && bedrockMapping.get("ignore").getAsBoolean())
						{
							LogManager.getLogger().debug("Ignoring Java block " + name);
							continue;
						}
						String bedrockName = bedrockMapping.get("name").getAsString();
						HashMap<String, Object> bedrockState = new HashMap<>();
						if (bedrockMapping.has("state"))
						{
							JsonObject bedrockStateObject = bedrockMapping.get("state").getAsJsonObject();
							for (Map.Entry<String, JsonElement> entry : bedrockStateObject.entrySet())
							{
								JsonElement bedrockStateElement = entry.getValue();
								if (bedrockStateElement.getAsJsonPrimitive().isString())
								{
									bedrockState.put(entry.getKey(), bedrockStateElement.getAsString());
								}
								else if (bedrockStateElement.getAsJsonPrimitive().isBoolean())
								{
									bedrockState.put(entry.getKey(), bedrockStateElement.getAsBoolean() ? 1 : 0);
								}
								else
								{
									bedrockState.put(entry.getKey(), bedrockStateElement.getAsInt());
								}
							}
						}
						int bedrockId = BedrockBlocks.getId(bedrockName, bedrockState);
						if (bedrockId == -1)
						{
							LogManager.getLogger().warn("Cannot find Bedrock block for Java block " + name);
						}
						else
						{
							bedrockNonVanillaIdMap.put(name, bedrockId);

							boolean waterlogged = name.contains("waterlogged=true");    // TODO: consider putting this in data file
							bedrockNonVanillaWaterloggedMap.put(name, waterlogged);
						}
					}

					if (nonVanillaStatesList.put(baseName, stateNames) != null)
					{
						LogManager.getLogger().warn("Duplicate Java non-vanilla block name " + baseName);
					}
				}
			}
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load Java blocks", exception);
			System.exit(1);
		}
	}

	public static void init()
	{
		/// empty, forces static initialiser to run if it hasn't already
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
	public static int getBedrockId(int javaId)
	{
		return getBedrockId(javaId, null);
	}

	@Deprecated
	public static boolean isWaterlogged(int javaId)
	{
		return isWaterlogged(javaId, null);
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

	public static int getBedrockId(int javaId, @Nullable FabricRegistryManager fabricRegistryManager)
	{
		int bedrockId = bedrockIdMap.getOrDefault(javaId, -1);
		if (bedrockId == -1 && fabricRegistryManager != null)
		{
			String fabricName = fabricRegistryManager.getBlockName(javaId);
			if (fabricName != null)
			{
				bedrockId = bedrockNonVanillaIdMap.getOrDefault(fabricName, -1);
			}
		}
		return bedrockId;
	}

	public static boolean isWaterlogged(int javaId, @Nullable FabricRegistryManager fabricRegistryManager)
	{
		if (bedrockWaterloggedMap.containsKey(javaId))
		{
			return bedrockWaterloggedMap.get(javaId);
		}
		if (fabricRegistryManager != null)
		{
			String fabricName = fabricRegistryManager.getBlockName(javaId);
			if (fabricName != null)
			{
				return bedrockNonVanillaWaterloggedMap.getOrDefault(fabricName, false);
			}
		}
		return false;
	}
}