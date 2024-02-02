package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class BedrockBiomes
{
	private static final HashMap<String, Integer> idMap = new HashMap<>();

	static
	{
		try (FileReader fileReader = new FileReader("data/biomes_bedrock.json"))
		{
			JsonElement root = JsonParser.parseReader(fileReader);
			for (JsonElement element : root.getAsJsonArray())
			{
				int id = element.getAsJsonObject().get("id").getAsInt();
				String name = element.getAsJsonObject().get("name").getAsString();
				if (idMap.containsValue(id))
				{
					LogManager.getLogger().warn("Duplicate biome ID {}", id);
				}
				if (idMap.put(name, id) != null)
				{
					LogManager.getLogger().warn("Duplicate biome name {}", name);
				}
			}
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load biomes data", exception);
			System.exit(1);
		}
	}

	public static void init()
	{
		// empty, forces static initialiser to run if it hasn't already
	}

	public static int getId(@NotNull String name)
	{
		return idMap.getOrDefault(name, -1);
	}
}
