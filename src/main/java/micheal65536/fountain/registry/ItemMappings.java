package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ItemMappings
{
	private static final HashMap<String, String> javaToBedrockMap = new HashMap<>();
	private static final HashMap<String, String> bedrockToJavaMap = new HashMap<>();

	static
	{
		try (FileReader fileReader = new FileReader("data/items_mapping.json"))
		{
			JsonElement root = JsonParser.parseReader(fileReader);
			for (JsonElement element : root.getAsJsonArray())
			{
				String javaName = element.getAsJsonObject().get("java").getAsString();
				String bedrockName = element.getAsJsonObject().get("bedrock").getAsString();
				javaToBedrockMap.put(javaName, bedrockName);
				bedrockToJavaMap.put(bedrockName, javaName);
			}
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load item mappings", exception);
			System.exit(1);
		}
	}

	public static void init()
	{
		// empty, forces static initialiser to run if it hasn't already
	}

	@NotNull
	public static String getBedrockName(@NotNull String javaName)
	{
		return javaToBedrockMap.getOrDefault(javaName, javaName);
	}

	@NotNull
	public static String getJavaName(@NotNull String bedrockName)
	{
		return bedrockToJavaMap.getOrDefault(bedrockName, bedrockName);
	}
}