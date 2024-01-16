package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class BedrockItemPalette
{
	private static final HashMap<String, Integer> idMap = new HashMap<>();
	private static final HashMap<Integer, String> nameMap = new HashMap<>();

	static
	{
		try (FileReader fileReader = new FileReader("data/items_bedrock.json"))
		{
			JsonElement root = JsonParser.parseReader(fileReader);
			for (JsonElement element : root.getAsJsonArray())
			{
				String name = element.getAsJsonObject().get("name").getAsString();
				int id = element.getAsJsonObject().get("id").getAsInt();
				idMap.put(name, id);
				nameMap.put(id, name);
			}
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load Bedrock items", exception);
			System.exit(1);
		}
	}

	public static void init()
	{
		// empty, forces static initialiser to run if it hasn't already
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