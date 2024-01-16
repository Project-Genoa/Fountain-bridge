package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.registry.EarthItemCatalog;
import micheal65536.fountain.registry.ItemMappings;
import micheal65536.fountain.registry.JavaItemPalette;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GenoaInventory
{
	private final HashMap<String, Integer> stackableItems = new HashMap<>();
	private final HashMap<String, HashMap<String, Integer>> nonStackableItems = new HashMap<>();

	public void addItem(@NotNull ItemStack itemStack)
	{
		Item item = this.toGenoaItem(itemStack);
		if (item == null)
		{
			return;
		}
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(item.uuid);

		if (itemInfo.stackable)
		{
			this.stackableItems.put(item.uuid, this.stackableItems.getOrDefault(item.uuid, 0) + item.count);
		}
		else
		{
			this.nonStackableItems.computeIfAbsent(item.uuid, key -> new HashMap<>()).put(item.instanceId, item.wear);
		}
	}

	public String[] updateHotbar(ItemStack[] oldHotbar, @NotNull String jsonString)
	{
		Item[] newItems = new Item[7];
		try
		{
			JsonElement root = JsonParser.parseReader(new StringReader(jsonString));
			int index = 0;
			for (JsonElement element : root.getAsJsonObject().get("hotbar").getAsJsonArray())
			{
				int count = element.getAsJsonObject().get("count").getAsInt();
				if (count > 0)
				{
					String uuid = element.getAsJsonObject().get("guid").getAsString();
					String instanceId = element.getAsJsonObject().has("instance_data") ? element.getAsJsonObject().get("instance_data").getAsJsonObject().get("id").getAsString() : null;
					if (instanceId != null)
					{
						newItems[index] = new Item(uuid, instanceId, -1);
					}
					else
					{
						newItems[index] = new Item(uuid, count);
					}
				}
				index++;
			}
		}
		catch (JsonParseException | UnsupportedOperationException | NullPointerException | ArrayIndexOutOfBoundsException exception)
		{
			LogManager.getLogger().warn("Invalid JSON in updateHotbar");
			return null;
		}

		for (ItemStack itemStack : oldHotbar)
		{
			if (itemStack != null)
			{
				this.addItem(itemStack);
			}
		}

		return Arrays.stream(newItems).map(item -> item != null ? this.takeItem(item) : null).toArray(String[]::new);
	}

	public String getJSONString(ItemStack[] hotbar)
	{
		Item[] genoaHotbar = Arrays.stream(hotbar).map(itemStack -> itemStack != null ? this.toGenoaItem(itemStack) : null).toArray(Item[]::new);

		try
		{
			StringWriter stringWriter = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(stringWriter);

			jsonWriter.beginObject();

			jsonWriter.name("hotbar").beginArray();
			for (Item item : genoaHotbar)
			{
				jsonWriter.beginObject();

				jsonWriter.name("guid").value(item != null ? item.uuid : "00000000-0000-0000-0000-000000000000");
				jsonWriter.name("count").value(item != null ? item.count : 0);
				jsonWriter.name("owned").value(item != null ? true : false);

				if (item != null && item.instanceId != null)
				{
					EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(item.uuid);
					jsonWriter.name("instance_data").beginObject();
					jsonWriter.name("id").value(item.instanceId);
					jsonWriter.name("health").value(Math.round(((float) (itemInfo.maxWear - item.wear) / (float) itemInfo.maxWear) * 100.0f));
					jsonWriter.endObject();
				}

				// TODO: category and rarity

				jsonWriter.endObject();
			}
			jsonWriter.endArray();

			// TODO: item unlocked timestamp, and include unlocked items which aren't owned
			jsonWriter.name("inventory").beginArray();
			for (Map.Entry<String, Integer> entry : this.stackableItems.entrySet())
			{
				String uuid = entry.getKey();
				int count = entry.getValue();
				if (count == 0)
				{
					continue;
				}

				jsonWriter.beginObject();

				jsonWriter.name("guid").value(uuid);
				jsonWriter.name("count").value(count);
				jsonWriter.name("owned").value(true);

				// TODO: category and rarity

				jsonWriter.endObject();
			}
			for (Map.Entry<String, HashMap<String, Integer>> entry : this.nonStackableItems.entrySet())
			{
				String uuid = entry.getKey();
				HashMap<String, Integer> instances = entry.getValue();
				EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(uuid);

				for (Map.Entry<String, Integer> instance : instances.entrySet())
				{
					String instanceId = instance.getKey();
					int wear = instance.getValue();

					jsonWriter.beginObject();

					jsonWriter.name("guid").value(uuid);
					jsonWriter.name("count").value(1);
					jsonWriter.name("owned").value(true);

					jsonWriter.name("instance_data").beginObject();
					jsonWriter.name("id").value(instanceId);
					jsonWriter.name("health").value(Math.round(((float) (itemInfo.maxWear - wear) / (float) itemInfo.maxWear) * 100.0f));
					jsonWriter.endObject();

					// TODO: category and rarity

					jsonWriter.endObject();
				}
			}
			for (Item item : Arrays.stream(genoaHotbar).filter(item -> item != null && item.count > 0 && this.stackableItems.getOrDefault(item.uuid, 0) == 0 && this.nonStackableItems.getOrDefault(item.uuid, new HashMap<>()).isEmpty()).toArray(Item[]::new))
			{
				jsonWriter.beginObject();

				jsonWriter.name("guid").value(item.uuid);
				jsonWriter.name("count").value(0);
				jsonWriter.name("owned").value(true);

				// TODO: category and rarity

				jsonWriter.endObject();
			}
			jsonWriter.endArray();

			jsonWriter.endObject();

			return stringWriter.toString();
		}
		catch (IOException exception)
		{
			throw new AssertionError(exception);
		}
	}

	@Nullable
	private String takeItem(@NotNull Item item)
	{
		if (item.count == 0)
		{
			return null;
		}

		EarthItemCatalog.NameAndAux nameAndAux = EarthItemCatalog.getNameAndAux(item.uuid);
		if (nameAndAux == null)
		{
			LogManager.getLogger().warn("Cannot find item " + item.uuid);
			return null;
		}
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(item.uuid);

		if (itemInfo.stackable)
		{
			int takeCount = Math.min(item.count, this.stackableItems.getOrDefault(item.uuid, 0));
			if (takeCount == 0)
			{
				return null;
			}

			if (takeCount > 64)    // TODO: determine the actual maximum count for the item (e.g. eggs can only stack up to 16 items)
			{
				takeCount = 64;
			}

			this.stackableItems.put(item.uuid, this.stackableItems.getOrDefault(item.uuid, 0) - takeCount);

			return earthToItemString(nameAndAux.name, nameAndAux.aux) + " " + takeCount;
		}
		else
		{
			if (item.instanceId == null)
			{
				LogManager.getLogger().warn("Non-stackable item with no instance ID");
				return null;
			}

			HashMap<String, Integer> instances = this.nonStackableItems.getOrDefault(item.uuid, null);
			if (instances == null)
			{
				return null;
			}

			if (!instances.containsKey(item.instanceId))
			{
				return null;
			}
			int wear = instances.remove(item.instanceId);

			String toolData = "{Damage:" + wear + ", GenoaInstanceId:\"" + item.instanceId + "\"}";

			return ItemMappings.getJavaName("minecraft:" + nameAndAux.name) + toolData + " 1";
		}
	}

	@Nullable
	private Item toGenoaItem(@NotNull ItemStack itemStack)
	{
		if (itemStack.getAmount() == 0)
		{
			return null;
		}

		String javaName = JavaItemPalette.getName(itemStack.getId());
		String bedrockName = ItemMappings.getBedrockName(javaName);
		String earthName = bedrockName.split(":", 2)[1];
		int aux = getAux(javaName, itemStack.getNbt());
		String uuid = EarthItemCatalog.getUUID(earthName, aux);
		if (uuid == null)
		{
			LogManager.getLogger().warn("Cannot find item UUID for " + earthName);
			return null;
		}
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(uuid);

		if (itemInfo.stackable)
		{
			return new Item(uuid, itemStack.getAmount());
		}
		else
		{
			CompoundTag nbt = itemStack.getNbt();
			String instanceId = nbt != null && nbt.contains("GenoaInstanceId") ? (String) nbt.get("GenoaInstanceId").getValue() : UUID.randomUUID().toString();
			int wear = nbt != null && nbt.contains("Damage") ? (int) nbt.get("Damage").getValue() : 0;
			return new Item(uuid, instanceId, wear);
		}
	}

	private static int getAux(@NotNull String javaName, @Nullable CompoundTag nbt)
	{
		if (nbt == null)
		{
			return 0;
		}

		// TODO
		return 0;
	}

	@Nullable
	private static String earthToItemString(@NotNull String earthName, int aux)
	{
		// TODO
		return ItemMappings.getJavaName("minecraft:" + earthName);
	}

	public static final class Item
	{
		@NotNull
		public final String uuid;
		@Nullable
		public final String instanceId;
		public final int wear;
		public final int count;

		private Item(@NotNull String uuid, int count)
		{
			this.uuid = uuid;
			this.instanceId = null;
			this.wear = 0;
			this.count = count;
		}

		private Item(@NotNull String uuid, @NotNull String instanceId, int wear)
		{
			this.uuid = uuid;
			this.instanceId = instanceId;
			this.wear = wear;
			this.count = 1;
		}
	}
}