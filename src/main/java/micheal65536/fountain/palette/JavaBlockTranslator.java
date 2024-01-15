package micheal65536.fountain.palette;

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

public class JavaBlockTranslator
{
	private static final HashMap<Integer, Integer> bedrockBlockIdMap = new HashMap<>();
	private static final HashMap<Integer, Boolean> isWaterloggedMap = new HashMap<>();
	private static final HashMap<Integer, String> unmappedBlocksMap = new HashMap<>();

	static
	{
		try (FileReader fileReader = new FileReader("data/blocks_java.json"))
		{
			JsonElement root = JsonParser.parseReader(fileReader);
			int javaId = 0;
			for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet())
			{
				JsonObject jsonObject = entry.getValue().getAsJsonObject();

				String javaNameAndState = entry.getKey();

				if (jsonObject.has("ignore") && jsonObject.get("ignore").getAsBoolean())
				{
					LogManager.getLogger().debug("Ignoring Java block " + javaNameAndState);
					unmappedBlocksMap.put(javaId, javaNameAndState);
					javaId++;
					continue;
				}

				String bedrockName = jsonObject.get("bedrock_name").getAsString();
				HashMap<String, Object> bedrockState = new HashMap<>();
				if (jsonObject.has("bedrock_states"))
				{
					JsonObject stateObject = jsonObject.get("bedrock_states").getAsJsonObject();
					for (Map.Entry<String, JsonElement> stateEntry : stateObject.entrySet())
					{
						JsonElement stateElement = stateEntry.getValue();
						if (stateElement.getAsJsonPrimitive().isString())
						{
							bedrockState.put(stateEntry.getKey(), stateElement.getAsString());
						}
						else if (stateElement.getAsJsonPrimitive().isBoolean())
						{
							bedrockState.put(stateEntry.getKey(), stateElement.getAsBoolean() ? 1 : 0);
						}
						else
						{
							bedrockState.put(stateEntry.getKey(), stateElement.getAsInt());
						}
					}
				}

				int bedrockId = BedrockBlockPalette.getId(bedrockName, bedrockState);
				if (bedrockId == -1)
				{
					unmappedBlocksMap.put(javaId, javaNameAndState);
					LogManager.getLogger().warn("Cannot find Bedrock block for Java block " + javaNameAndState);
				}
				else
				{
					bedrockBlockIdMap.put(javaId, bedrockId);

					boolean waterlogged = javaNameAndState.contains("waterlogged=true") || bedrockName.equals("minecraft:bubble_column") || bedrockName.equals("minecraft:kelp") || bedrockName.equals("minecraft:seagrass");
					isWaterloggedMap.put(javaId, waterlogged);
				}
				javaId++;
			}
		}
		catch (IOException | JsonParseException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load Java blocks", exception);
			System.exit(1);
		}
	}

	public static void init()
	{
		/// empty, forces static initialiser to run if it hasn't already
	}

	public static int getBedrockBlockId(int javaBlockId)
	{
		return bedrockBlockIdMap.getOrDefault(javaBlockId, -1);
	}

	public static boolean isWaterlogged(int javaBlockId)
	{
		return isWaterloggedMap.getOrDefault(javaBlockId, false);
	}

	@Nullable
	public static String getUnmappedBlockName(int javaBlockId)
	{
		return unmappedBlocksMap.getOrDefault(javaBlockId, null);
	}
}