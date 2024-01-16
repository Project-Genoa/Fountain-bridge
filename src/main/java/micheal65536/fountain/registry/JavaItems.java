package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JavaItems
{
	private static final HashMap<Integer, String> map = new HashMap<>();

	private static final HashMap<Integer, Integer> bedrockIdMap = new HashMap<>();
	private static final HashMap<Integer, NbtMap> bedrockNBTMap = new HashMap<>();
	private static final HashMap<ItemNameAndNBT, Integer> javaIdMap = new HashMap<>();

	static
	{
		try (FileReader fileReader = new FileReader("data/items_java.json"))
		{
			JsonElement root = JsonParser.parseReader(fileReader);
			for (JsonElement element : root.getAsJsonArray())
			{
				int id = element.getAsJsonObject().get("id").getAsInt();
				String name = element.getAsJsonObject().get("name").getAsString();
				if (map.put(id, name) != null)
				{
					LogManager.getLogger().warn("Duplicate Java item ID " + id);
				}

				JsonObject bedrockMapping = element.getAsJsonObject().get("bedrock").getAsJsonObject();
				if (bedrockMapping.has("ignore") && bedrockMapping.get("ignore").getAsBoolean())
				{
					LogManager.getLogger().debug("Ignoring Java item " + name);
					continue;
				}
				String bedrockName = bedrockMapping.get("name").getAsString();
				NbtMap bedrockNBT;
				if (bedrockMapping.has("nbt"))
				{
					NbtMapBuilder builder = NbtMap.builder();
					JsonObject nbtObject = bedrockMapping.get("nbt").getAsJsonObject();
					for (Map.Entry<String, JsonElement> entry : nbtObject.entrySet())
					{
						JsonElement stateElement = entry.getValue();
						if (stateElement.getAsJsonPrimitive().isString())
						{
							builder.putString(entry.getKey(), stateElement.getAsString());
						}
						else if (stateElement.getAsJsonPrimitive().isBoolean())
						{
							builder.putBoolean(entry.getKey(), stateElement.getAsBoolean());
						}
						else
						{
							// TODO: NBT requires type information
							//builder.putInt(entry.getKey(), stateElement.getAsInt());
						}
					}
					bedrockNBT = builder.build();
				}
				else
				{
					bedrockNBT = null;
				}
				int bedrockId = BedrockItems.getId(bedrockName);
				if (bedrockId == 0)
				{
					LogManager.getLogger().warn("Cannot find Bedrock item for Java item " + name);
				}
				else
				{
					bedrockIdMap.put(id, bedrockId);
					bedrockNBTMap.put(id, bedrockNBT);
					if (javaIdMap.put(new ItemNameAndNBT(bedrockName, bedrockNBT), id) != null)
					{
						LogManager.getLogger().warn("Duplicate Bedrock item mapping " + bedrockName);
					}
				}
			}
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load Java items", exception);
			System.exit(1);
		}
	}

	public static void init()
	{
		// empty, forces static initialiser to run if it hasn't already
	}

	@Nullable
	public static String getName(int id)
	{
		return map.getOrDefault(id, null);
	}

	public static int getBedrockId(int javaId)
	{
		return bedrockIdMap.getOrDefault(javaId, 0);
	}

	@Nullable
	public static NbtMap getBedrockNBT(int javaId)
	{
		return bedrockNBTMap.getOrDefault(javaId, null);
	}

	public static int getJavaId(@NotNull String bedrockName, @Nullable NbtMap bedrockNBT)
	{
		return javaIdMap.getOrDefault(new ItemNameAndNBT(bedrockName, bedrockNBT), -1);
	}

	private static final class ItemNameAndNBT
	{
		@NotNull
		public final String name;
		@Nullable
		public final NbtMap nbt;

		public ItemNameAndNBT(@NotNull String name, @Nullable NbtMap nbt)
		{
			this.name = name;
			this.nbt = nbt;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof ItemNameAndNBT)
			{
				return this.name.equals(((ItemNameAndNBT) obj).name) && ((this.nbt == null && ((ItemNameAndNBT) obj).nbt == null) || (this.nbt != null && ((ItemNameAndNBT) obj).nbt != null && this.nbt.equals(((ItemNameAndNBT) obj).nbt)));
			}
			else
			{
				return false;
			}
		}

		@Override
		public int hashCode()
		{
			return this.name.hashCode() ^ (this.nbt != null ? this.nbt.hashCode() : 0);
		}
	}
}