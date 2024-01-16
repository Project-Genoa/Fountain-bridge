package micheal65536.fountain.registry;

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

public class EarthItemCatalog
{
	private static final HashMap<String, ItemInfo> itemInfoMap = new HashMap<>();
	private static final HashMap<NameAndAux, String> itemUUIDMap = new HashMap<>();
	private static final HashMap<String, NameAndAux> itemNameMap = new HashMap<>();

	static
	{
		try (FileReader fileReader = new FileReader("data/items_catalog.json"))
		{
			JsonElement root = JsonParser.parseReader(fileReader);
			for (JsonElement element : root.getAsJsonArray())
			{
				String uuid = element.getAsJsonObject().get("id").getAsString();
				JsonObject item = element.getAsJsonObject().get("item").getAsJsonObject();
				String name = item.get("name").getAsString();
				int aux = item.get("aux").getAsInt();
				boolean stackable = element.getAsJsonObject().get("stacks").getAsBoolean();
				int maxWear = item.has("health") && !item.get("health").isJsonNull() ? item.get("health").getAsInt() : 0;
				// TODO: category and rarity

				itemInfoMap.put(uuid, new ItemInfo(stackable, maxWear));
				itemUUIDMap.put(new NameAndAux(name, aux), uuid);
				itemNameMap.put(uuid, new NameAndAux(name, aux));
			}
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot load Earth item catalog", exception);
			System.exit(1);
		}
	}

	public static void init()
	{
		// empty, forces static initialiser to run if it hasn't already
	}

	@Nullable
	public static String getUUID(@NotNull String name, int aux)
	{
		return itemUUIDMap.getOrDefault(new NameAndAux(name, aux), null);
	}

	@Nullable
	public static NameAndAux getNameAndAux(@NotNull String uuid)
	{
		return itemNameMap.getOrDefault(uuid, null);
	}

	@NotNull
	public static ItemInfo getItemInfo(@NotNull String uuid)
	{
		ItemInfo itemInfo = itemInfoMap.getOrDefault(uuid, null);
		if (itemInfo == null)
		{
			throw new IllegalArgumentException();
		}
		return itemInfo;
	}

	public static final class NameAndAux
	{
		@NotNull
		public final String name;
		public final int aux;

		private NameAndAux(@NotNull String name, int aux)
		{
			this.name = name;
			this.aux = aux;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof NameAndAux)
			{
				return this.name.equals(((NameAndAux) obj).name) && this.aux == ((NameAndAux) obj).aux;
			}
			else
			{
				return false;
			}
		}

		@Override
		public int hashCode()
		{
			return this.name.hashCode() + this.aux;
		}
	}

	public static final class ItemInfo
	{
		public final boolean stackable; // in Earth, stackable appears to directly correlate with whether the item has wear or not (stackable = no wear, non-stackable = requires wear)
		public final int maxWear;
		// TODO: category
		// TODO: rarity

		private ItemInfo(boolean stackable, int maxWear)
		{
			this.stackable = stackable;
			this.maxWear = maxWear;
		}
	}
}