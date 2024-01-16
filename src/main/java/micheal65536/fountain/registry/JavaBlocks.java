package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JavaBlocks
{
	private static final HashMap<Integer, String> map = new HashMap<>();

	private static final HashMap<Integer, Integer> bedrockIdMap = new HashMap<>();
	private static final HashMap<Integer, Boolean> bedrockWaterloggedMap = new HashMap<>();

	static
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
					JsonObject stateObject = bedrockMapping.get("state").getAsJsonObject();
					for (Map.Entry<String, JsonElement> entry : stateObject.entrySet())
					{
						JsonElement stateElement = entry.getValue();
						if (stateElement.getAsJsonPrimitive().isString())
						{
							bedrockState.put(entry.getKey(), stateElement.getAsString());
						}
						else if (stateElement.getAsJsonPrimitive().isBoolean())
						{
							bedrockState.put(entry.getKey(), stateElement.getAsBoolean() ? 1 : 0);
						}
						else
						{
							bedrockState.put(entry.getKey(), stateElement.getAsInt());
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

	@Nullable
	public static String getName(int id)
	{
		return map.getOrDefault(id, null);
	}

	public static int getBedrockId(int javaId)
	{
		return bedrockIdMap.getOrDefault(javaId, -1);
	}

	public static boolean isWaterlogged(int javaId)
	{
		return bedrockWaterloggedMap.getOrDefault(javaId, false);
	}
}