package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.DataFile;

import java.util.HashMap;

public class BedrockItems
{
	private static final HashMap<String, Integer> idMap = new HashMap<>();
	private static final HashMap<Integer, String> nameMap = new HashMap<>();

	static
	{
		DataFile.load("registry/items_bedrock.json", root ->
		{
			for (JsonElement element : root.getAsJsonArray())
			{
				int id = element.getAsJsonObject().get("id").getAsInt();
				String name = element.getAsJsonObject().get("name").getAsString();
				if (nameMap.put(id, name) != null)
				{
					LogManager.getLogger().warn("Duplicate Bedrock item ID {}", id);
				}
				if (idMap.put(name, id) != null)
				{
					LogManager.getLogger().warn("Duplicate Bedrock item name {}", name);
				}
			}
		});
	}

	public static int getId(@NotNull String name)
	{
		return idMap.getOrDefault(name, 0);
	}

	@Nullable
	public static String getName(int id)
	{
		return nameMap.getOrDefault(id, null);
	}
}