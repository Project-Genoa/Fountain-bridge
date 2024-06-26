package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.DataFile;

import java.util.HashMap;
import java.util.Locale;

public class EarthItemCatalog
{
	private static final HashMap<String, ItemInfo> itemInfoMap = new HashMap<>();
	private static final HashMap<NameAndAux, String> itemUUIDMap = new HashMap<>();
	private static final HashMap<String, NameAndAux> itemNameMap = new HashMap<>();

	static
	{
		DataFile.load("registry/items_catalog.json", root ->
		{
			for (JsonElement element : root.getAsJsonArray())
			{
				JsonObject object = element.getAsJsonObject();
				String uuid = object.get("uuid").getAsString();
				String name = object.get("name").getAsString();
				int aux = object.get("aux").getAsInt();
				boolean stackable = object.get("stackable").getAsBoolean();
				int maxWear = !stackable ? object.get("maxWear").getAsInt() : 0;
				ItemInfo.Category category = ItemInfo.Category.valueOf(object.get("category").getAsString().toUpperCase(Locale.ROOT));
				ItemInfo.Rarity rarity = ItemInfo.Rarity.valueOf(object.get("rarity").getAsString().toUpperCase(Locale.ROOT));

				itemInfoMap.put(uuid, new ItemInfo(stackable, maxWear, category, rarity));
				itemUUIDMap.put(new NameAndAux(name, aux), uuid);
				itemNameMap.put(uuid, new NameAndAux(name, aux));
			}
		});
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
		@NotNull
		public final Category category;
		@NotNull
		public final Rarity rarity;

		private ItemInfo(boolean stackable, int maxWear, @NotNull Category category, @NotNull Rarity rarity)
		{
			this.stackable = stackable;
			this.maxWear = maxWear;
			this.category = category;
			this.rarity = rarity;
		}

		public enum Category
		{
			MOBS("inventory.category.mobs", 1),
			CONSTRUCTION("inventory.category.construction", 2),
			NATURE("inventory.category.nature", 3),
			EQUIPMENT("inventory.category.equipment", 4),
			ITEMS("inventory.category.items", 5),
			INVALID("inventory.category.invalid", 6);

			@NotNull
			public final String loc;
			public final int value;

			Category(@NotNull String loc, int value)
			{
				this.loc = loc;
				this.value = value;
			}
		}

		public enum Rarity
		{
			COMMON("inventory.rarity.common", 0),
			UNCOMMON("inventory.rarity.uncommon", 1),
			RARE("inventory.rarity.rare", 2),
			EPIC("inventory.rarity.epic", 3),
			LEGENDARY("inventory.rarity.legendary", 4),
			OOBE("inventory.rarity.oobe", 5),
			INVALID("inventory.rarity.invalid", 6);

			@NotNull
			public final String loc;
			public final int value;

			Rarity(@NotNull String loc, int value)
			{
				this.loc = loc;
				this.value = value;
			}
		}
	}
}