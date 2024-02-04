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

	private static final HashMap<Integer, BedrockMapping> bedrockMap = new HashMap<>();
	private static final HashMap<String, BedrockMapping> bedrockNonVanillaMap = new HashMap<>();

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
						LogManager.getLogger().warn("Duplicate Java block ID {}", id);
					}

					BedrockMapping bedrockMapping = readBedrockMapping(element.getAsJsonObject().get("bedrock").getAsJsonObject());
					if (bedrockMapping == null)
					{
						LogManager.getLogger().debug("Ignoring Java block {}", name);
						continue;
					}
					if (bedrockMapping.id == -1)
					{
						LogManager.getLogger().warn("Cannot find Bedrock block for Java block {}", name);
					}
					else
					{
						bedrockMap.put(id, bedrockMapping);
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

						BedrockMapping bedrockMapping = readBedrockMapping(stateElement.getAsJsonObject().get("bedrock").getAsJsonObject());
						if (bedrockMapping == null)
						{
							LogManager.getLogger().debug("Ignoring Java block {}", name);
							continue;
						}
						if (bedrockMapping.id == -1)
						{
							LogManager.getLogger().warn("Cannot find Bedrock block for Java block {}", name);
						}
						else
						{
							bedrockNonVanillaMap.put(name, bedrockMapping);
						}
					}

					if (nonVanillaStatesList.put(baseName, stateNames) != null)
					{
						LogManager.getLogger().warn("Duplicate Java non-vanilla block name {}", baseName);
					}
				}
			}
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load Java blocks data", exception);
			System.exit(1);
		}
	}

	@Nullable
	private static BedrockMapping readBedrockMapping(JsonObject bedrockMappingObject)
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

		boolean waterlogged = bedrockMappingObject.has("waterlogged") ? bedrockMappingObject.get("waterlogged").getAsBoolean() : false;

		int id = BedrockBlocks.getId(name, state);
		return new BedrockMapping(id, waterlogged);
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

	public static final class BedrockMapping
	{
		public final int id;
		public final boolean waterlogged;

		private BedrockMapping(int id, boolean waterlogged)
		{
			this.id = id;
			this.waterlogged = waterlogged;
		}
	}
}