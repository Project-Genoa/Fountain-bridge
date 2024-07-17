package micheal65536.fountain.mappings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.DataFile;

import java.util.HashMap;

public class DirectSounds
{
	private static final HashMap<String, String> map = new HashMap<>();

	static
	{
		DataFile.load("mappings/direct_sounds.json", root ->
		{
			readSegment("", "", root.getAsJsonObject());
		});
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
				}
			}
		});
	}

	@Nullable
	public static String getDirectSoundMapping(@NotNull String javaName)
	{
		return map.getOrDefault(javaName, null);
	}
}