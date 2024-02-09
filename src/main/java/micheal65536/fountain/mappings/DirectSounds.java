package micheal65536.fountain.mappings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class DirectSounds
{
	private static final HashMap<String, String> map = new HashMap<>();

	static
	{
		try (FileReader fileReader = new FileReader("data/direct_sounds.json"))
		{
			JsonElement root = JsonParser.parseReader(fileReader);

			readSegment("", "", root.getAsJsonObject());
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load direct sounds mapping data", exception);
			System.exit(1);
		}
	}

	private static void readSegment(@NotNull String javaPath, @NotNull String bedrockPath, @NotNull JsonObject segment)
	{
		segment.asMap().forEach((javaName, element) ->
		{
			if (javaName.startsWith("_"))
			{
				return;
			}
			if (element.isJsonObject())
			{
				String bedrockName;
				if (element.getAsJsonObject().has("_bedrock_name"))
				{
					bedrockName = element.getAsJsonObject().get("_bedrock_name").getAsString();
				}
				else
				{
					bedrockName = javaName;
				}
				readSegment(javaPath + javaName + ".", bedrockPath + bedrockName + ".", element.getAsJsonObject());

				if (segment.has("_all"))
				{
					for (JsonElement allElement : segment.get("_all").getAsJsonArray())
					{
						if (allElement.getAsJsonObject().has("_include") && allElement.getAsJsonObject().get("_include").getAsJsonArray().asList().stream().map(JsonElement::getAsString).noneMatch(name -> name.equals(javaName)))
						{
							continue;
						}
						if (allElement.getAsJsonObject().has("_exclude") && allElement.getAsJsonObject().get("_exclude").getAsJsonArray().asList().stream().map(JsonElement::getAsString).anyMatch(name -> name.equals(javaName)))
						{
							continue;
						}
						readSegment(javaPath + javaName + ".", bedrockPath + bedrockName + ".", allElement.getAsJsonObject());
					}
				}
			}
			else
			{
				if (element.isJsonNull())
				{
					map.putIfAbsent(javaPath + javaName, null);
				}
				else
				{
					String bedrockName = element.getAsString();
					map.putIfAbsent(javaPath + javaName, bedrockName.contains(".") || bedrockName.equals("_ignore") ? bedrockName : (bedrockPath + bedrockName));
					if (bedrockName.equals("_ignore"))
					{
						LogManager.getLogger().debug("Ignoring sound {}", javaPath + javaName);
					}
				}
			}
		});
	}

	public static void init()
	{
		// empty, forces static initialiser to run if it hasn't already
	}

	@Nullable
	public static String getDirectSoundMapping(@NotNull String javaName)
	{
		return map.getOrDefault(javaName, null);
	}
}