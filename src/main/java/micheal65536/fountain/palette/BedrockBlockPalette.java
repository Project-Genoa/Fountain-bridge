package micheal65536.fountain.palette;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BedrockBlockPalette
{
	private static final HashMap<BlockNameAndState, Integer> map = new HashMap<>();

	public static final int AIR;
	public static final int WATER;

	static
	{
		try (FileReader fileReader = new FileReader("data/blocks_bedrock.json"))
		{
			JsonElement root = JsonParser.parseReader(fileReader);
			int id = 0;
			for (JsonElement element : root.getAsJsonArray())
			{
				String name = element.getAsJsonObject().get("name").getAsString();
				HashMap<String, Object> state = new HashMap<>();
				JsonObject stateObject = element.getAsJsonObject().get("states").getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : stateObject.entrySet())
				{
					JsonElement stateElement = entry.getValue();
					if (stateElement.getAsJsonPrimitive().isString())
					{
						state.put(entry.getKey(), stateElement.getAsString());
					}
					else
					{
						state.put(entry.getKey(), stateElement.getAsInt());
					}
				}
				map.put(new BlockNameAndState(name, state), id);
				id++;
			}
		}
		catch (IOException | JsonParseException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load Bedrock blocks", exception);
			System.exit(1);
		}

		AIR = BedrockBlockPalette.getId("minecraft:air", new HashMap<>());
		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put("liquid_depth", 0);
		WATER = BedrockBlockPalette.getId("minecraft:water", hashMap);
	}

	public static void init()
	{
		// empty, forces static initialiser to run if it hasn't already
	}

	public static int getId(@NotNull String name, @NotNull HashMap<String, Object> state)
	{
		BlockNameAndState blockNameAndState = new BlockNameAndState(name, state);
		return map.getOrDefault(blockNameAndState, -1);
	}

	private static final class BlockNameAndState
	{
		@NotNull
		public final String name;
		@NotNull
		public final HashMap<String, Object> state;

		public BlockNameAndState(@NotNull String name, @NotNull HashMap<String, Object> state)
		{
			this.name = name;
			this.state = state;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof BlockNameAndState)
			{
				return this.name.equals(((BlockNameAndState) obj).name) && this.state.equals(((BlockNameAndState) obj).state);
			}
			else
			{
				return false;
			}
		}

		@Override
		public int hashCode()
		{
			return this.name.hashCode() ^ this.state.hashCode();
		}
	}
}