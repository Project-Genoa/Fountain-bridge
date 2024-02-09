package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.DataFile;

import java.util.HashMap;

public class BedrockBiomes
{
	private static final HashMap<String, Integer> idMap = new HashMap<>();

	static
	{
		DataFile.load("registry/biomes_bedrock.json", root ->
		{
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
		});
	}

	public static int getId(@NotNull String name)
	{
		return idMap.getOrDefault(name, -1);
	}
}
