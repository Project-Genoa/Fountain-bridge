package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.connector.PlayerConnectorPluginWrapper;
import micheal65536.fountain.connector.plugin.ConnectorPlugin;
import micheal65536.fountain.connector.plugin.Inventory;
import micheal65536.fountain.registry.BedrockItems;
import micheal65536.fountain.registry.EarthItemCatalog;
import micheal65536.fountain.registry.JavaItems;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.IntStream;

public final class GenoaInventory
{
	private final HashMap<String, Integer> stackableItems = new HashMap<>();
	private final HashMap<String, HashMap<String, Integer>> nonStackableItems = new HashMap<>();
	private final Item[] hotbar;

	private final PlayerConnectorPluginWrapper playerConnectorPluginWrapper;

	public GenoaInventory(@NotNull PlayerConnectorPluginWrapper playerConnectorPluginWrapper)
	{
		this.hotbar = new Item[7];
		this.playerConnectorPluginWrapper = playerConnectorPluginWrapper;
	}

	public void loadInitialInventory(@NotNull Inventory inventory)
	{
		for (Inventory.StackableItem stackableItem : inventory.getStackableItems())
		{
			this.stackableItems.put(stackableItem.uuid, stackableItem.count);
		}
		for (Inventory.NonStackableItem nonStackableItem : inventory.getNonStackableItems())
		{
			this.nonStackableItems.computeIfAbsent(nonStackableItem.uuid, uuid -> new HashMap<>()).put(nonStackableItem.instanceId, nonStackableItem.wear);
		}
		Inventory.HotbarItem[] hotbar = inventory.getHotbar();
		for (int index = 0; index < 7; index++)
		{
			Inventory.HotbarItem hotbarItem = hotbar[index];
			if (hotbarItem == null)
			{
				this.hotbar[index] = null;
			}
			else
			{
				if (hotbarItem.instanceId != null)
				{
					this.hotbar[index] = new Item(hotbarItem.uuid, hotbarItem.instanceId, -1);
				}
				else
				{
					this.hotbar[index] = new Item(hotbarItem.uuid, hotbarItem.count);
				}
			}
		}
	}

	@NotNull
	public Inventory toConnectorPluginInventory()
	{
		return new Inventory(
				this.stackableItems.entrySet().stream().filter(entry -> entry.getValue() > 0).map(entry -> new Inventory.StackableItem(entry.getKey(), entry.getValue())).toArray(Inventory.StackableItem[]::new),
				this.nonStackableItems.entrySet().stream().flatMap(entry -> entry.getValue().entrySet().stream().map(entry1 -> new Inventory.NonStackableItem(entry.getKey(), entry1.getKey(), entry1.getValue()))).toArray(Inventory.NonStackableItem[]::new),
				Arrays.stream(this.hotbar).map(item -> item != null && item.count > 0 ? (item.instanceId != null ? new Inventory.HotbarItem(item.uuid, item.instanceId) : new Inventory.HotbarItem(item.uuid, item.count)) : null).toArray(Inventory.HotbarItem[]::new)
		);
	}

	public void addItemFromJavaServer(@NotNull ItemStack itemStack)
	{
		Item item = this.toGenoaItem(itemStack);
		if (item == null)
		{
			return;
		}
		this.addItem(item);
	}

	public void setHotbarFromJavaServer(ItemStack[] itemStacks)
	{
		Item[] newHotbar = Arrays.stream(itemStacks).map(itemStack -> itemStack != null ? this.toGenoaItem(itemStack) : null).toArray(Item[]::new);

		int[] changedIndexes = IntStream.range(0, 7).filter(index ->
		{
			Item currentItem = this.hotbar[index];
			Item newItem = newHotbar[index];
			if (currentItem == null && newItem == null)
			{
				return false;
			}
			else if (currentItem != null && newItem != null)
			{
				if (newItem.uuid.equals(currentItem.uuid))
				{
					if (currentItem.instanceId != null && newItem.instanceId != null && newItem.instanceId.equals(currentItem.instanceId) && newItem.wear == currentItem.wear)
					{
						return false;
					}
					else if (currentItem.instanceId == null && newItem.instanceId == null && newItem.count == currentItem.count)
					{
						return false;
					}
				}
			}
			return true;
		}).toArray();
		if (changedIndexes.length == 0)
		{
			return;
		}

		HashMap<String, Integer> stackableItemCountChanges = new HashMap<>();
		HashMap<String, HashMap<String, Integer>> addedNonStackableItems = new HashMap<>();
		HashMap<String, HashSet<String>> removedNonStackableItems = new HashMap<>();
		HashMap<String, HashMap<String, Integer>> updatedNonStackableItems = new HashMap<>();
		for (int index : changedIndexes)
		{
			Item item = this.hotbar[index];
			if (item != null)
			{
				if (item.instanceId == null)
				{
					stackableItemCountChanges.put(item.uuid, stackableItemCountChanges.getOrDefault(item.uuid, 0) - item.count);
				}
				else
				{
					if (!removedNonStackableItems.computeIfAbsent(item.uuid, uuid -> new HashSet<>()).add(item.instanceId))
					{
						throw new AssertionError();
					}
				}
			}
		}
		for (int index : changedIndexes)
		{
			Item item = newHotbar[index];
			if (item != null)
			{
				if (item.instanceId == null)
				{
					stackableItemCountChanges.put(item.uuid, stackableItemCountChanges.getOrDefault(item.uuid, 0) + item.count);
				}
				else
				{
					if (addedNonStackableItems.computeIfAbsent(item.uuid, uuid -> new HashMap<>()).put(item.instanceId, item.wear) != null)
					{
						throw new AssertionError();
					}
				}
			}
		}

		for (String uuid : removedNonStackableItems.keySet())
		{
			HashSet<String> removedInstances = removedNonStackableItems.get(uuid);
			HashMap<String, Integer> addedInstances = addedNonStackableItems.getOrDefault(uuid, null);
			if (addedInstances == null)
			{
				continue;
			}
			for (String instanceId : removedInstances.toArray(String[]::new))
			{
				if (addedInstances.containsKey(instanceId))
				{
					int wear = addedInstances.get(instanceId);
					updatedNonStackableItems.computeIfAbsent(uuid, uuid1 -> new HashMap<>()).put(instanceId, wear);
					removedInstances.remove(instanceId);
					addedInstances.remove(instanceId);
				}
			}
		}

		stackableItemCountChanges.forEach((uuid, count) ->
		{
			if (count > 0)
			{
				this.addItem(new Item(uuid, count));
			}
			else if (count < 0)
			{
				if (!this.takeItem(new Item(uuid, -count)))
				{
					throw new AssertionError();
				}
			}
		});
		addedNonStackableItems.forEach((uuid, instances) ->
		{
			instances.forEach((instanceId, wear) ->
			{
				this.addItem(new Item(uuid, instanceId, wear));
			});
		});
		removedNonStackableItems.forEach((uuid, instances) ->
		{
			instances.forEach(instanceId ->
			{
				if (!this.takeItem(new Item(uuid, instanceId, -1)))
				{
					throw new AssertionError();
				}
			});
		});
		updatedNonStackableItems.forEach((uuid, instances) ->
		{
			instances.forEach((instanceId, wear) ->
			{
				if (!this.updateItemWear(new Item(uuid, instanceId, wear)))
				{
					throw new AssertionError();
				}
			});
		});

		for (int index : changedIndexes)
		{
			this.hotbar[index] = newHotbar[index];
		}
		this.sendHotbarToConnectorPlugin();
	}

	public void clearHotbar()
	{
		Arrays.fill(this.hotbar, null);
	}

	public boolean updateHotbarFromClient(@NotNull String jsonString)
	{
		Item[] newHotbarItems = new Item[7];
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
						newHotbarItems[index] = new Item(uuid, instanceId, -1);
					}
					else
					{
						newHotbarItems[index] = new Item(uuid, count);
					}
				}
				index++;
			}
		}
		catch (Exception exception)
		{
			LogManager.getLogger().warn("Invalid JSON in updateHotbarFromClient");
			return false;
		}

		HashMap<String, Integer> hotbarItemCounts = new HashMap<>();
		HashMap<String, HashSet<String>> hotbarItemInstances = new HashMap<>();
		for (Item item : newHotbarItems)
		{
			if (item != null)
			{
				boolean have;
				if (item.instanceId == null)
				{
					int count = hotbarItemCounts.getOrDefault(item.uuid, 0) + item.count;
					hotbarItemCounts.put(item.uuid, count);
					have = this.stackableItems.getOrDefault(item.uuid, 0) >= count;
				}
				else
				{
					HashSet<String> hotbarInstances = hotbarItemInstances.computeIfAbsent(item.uuid, uuid -> new HashSet<>());
					if (!hotbarInstances.add(item.instanceId))
					{
						have = false;
					}
					else
					{
						HashMap<String, Integer> instances = this.nonStackableItems.getOrDefault(item.uuid, null);
						have = instances != null && instances.containsKey(item.instanceId);
					}
				}
				if (!have)
				{
					LogManager.getLogger().warn("Client tried to set hotbar with item that is not in inventory");
					return false;
				}
			}
		}

		for (int index = 0; index < 7; index++)
		{
			this.hotbar[index] = newHotbarItems[index];
		}

		this.sendHotbarToConnectorPlugin();

		return true;
	}

	public ItemStack[] getHotbarForJavaServer()
	{
		return Arrays.stream(this.hotbar).map(item ->
		{
			if (item == null || item.count == 0)
			{
				return null;
			}

			EarthItemCatalog.NameAndAux nameAndAux = EarthItemCatalog.getNameAndAux(item.uuid);
			if (nameAndAux == null)
			{
				LogManager.getLogger().warn("Cannot find item with UUID {}", item.uuid);
				return null;
			}
			EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(item.uuid);

			if (itemInfo.stackable)
			{
				return toItemStack(nameAndAux.name, nameAndAux.aux, item.count);
			}
			else
			{
				if (item.instanceId == null)
				{
					LogManager.getLogger().warn("Non-stackable item with no instance ID");
					return null;
				}
				int wear = this.nonStackableItems.get(item.uuid).get(item.instanceId);
				return toItemStack(nameAndAux.name, nameAndAux.aux, item.instanceId, wear);
			}
		}).toArray(ItemStack[]::new);
	}

	public String getGenoaInventoryResponseJSON()
	{
		try
		{
			StringWriter stringWriter = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(stringWriter);

			jsonWriter.beginObject();

			jsonWriter.name("hotbar").beginArray();
			for (Item item : this.hotbar)
			{
				EarthItemCatalog.ItemInfo itemInfo = item != null ? EarthItemCatalog.getItemInfo(item.uuid) : null;

				jsonWriter.beginObject();

				jsonWriter.name("guid").value(item != null ? item.uuid : "00000000-0000-0000-0000-000000000000");
				jsonWriter.name("count").value(item != null ? item.count : 0);
				jsonWriter.name("owned").value(item != null ? true : false);

				if (item != null && item.instanceId != null)
				{
					jsonWriter.name("instance_data").beginObject();
					jsonWriter.name("id").value(item.instanceId);
					jsonWriter.name("health").value(((float) (itemInfo.maxWear - item.wear) / (float) itemInfo.maxWear) * 100.0f);
					jsonWriter.endObject();
				}

				jsonWriter.name("category").beginObject();
				jsonWriter.name("loc").value(itemInfo != null ? itemInfo.category.loc : EarthItemCatalog.ItemInfo.Category.INVALID.loc);
				jsonWriter.name("value").value(itemInfo != null ? itemInfo.category.value : EarthItemCatalog.ItemInfo.Category.INVALID.value);
				jsonWriter.endObject();
				jsonWriter.name("rarity").beginObject();
				jsonWriter.name("loc").value(itemInfo != null ? itemInfo.rarity.loc : EarthItemCatalog.ItemInfo.Rarity.INVALID.loc);
				jsonWriter.name("value").value(itemInfo != null ? itemInfo.rarity.value : EarthItemCatalog.ItemInfo.Rarity.INVALID.value);
				jsonWriter.endObject();

				jsonWriter.endObject();
			}
			jsonWriter.endArray();

			HashSet<String> itemIds = new HashSet<>();
			HashMap<String, Integer> hotbarItemCounts = new HashMap<>();
			HashSet<String> hotbarItemInstances = new HashSet<>();
			itemIds.addAll(this.stackableItems.keySet());
			itemIds.addAll(this.nonStackableItems.keySet());
			for (Item item : this.hotbar)
			{
				if (item != null)
				{
					itemIds.add(item.uuid);
					hotbarItemCounts.put(item.uuid, hotbarItemCounts.getOrDefault(item.uuid, 0) + item.count);
					if (item.instanceId != null)
					{
						hotbarItemInstances.add(item.instanceId);
					}
				}
			}

			// TODO: item unlocked timestamp, and include unlocked items which aren't owned
			jsonWriter.name("inventory").beginArray();
			for (String uuid : itemIds)
			{
				EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(uuid);

				if (itemInfo.stackable)
				{
					int count = this.stackableItems.getOrDefault(uuid, 0) - hotbarItemCounts.getOrDefault(uuid, 0);
					if (count < 0)
					{
						throw new AssertionError();
					}

					jsonWriter.beginObject();

					jsonWriter.name("guid").value(uuid);
					jsonWriter.name("count").value(count);
					jsonWriter.name("owned").value(true);

					jsonWriter.name("category").beginObject();
					jsonWriter.name("loc").value(itemInfo.category.loc);
					jsonWriter.name("value").value(itemInfo.category.value);
					jsonWriter.endObject();
					jsonWriter.name("rarity").beginObject();
					jsonWriter.name("loc").value(itemInfo.rarity.loc);
					jsonWriter.name("value").value(itemInfo.rarity.value);
					jsonWriter.endObject();

					jsonWriter.endObject();
				}
				else
				{
					HashMap<String, Integer> instances = this.nonStackableItems.getOrDefault(uuid, new HashMap<>());
					HashSet<String> instanceIds = new HashSet<>(instances.keySet());
					instanceIds.removeAll(hotbarItemInstances);

					if (!instanceIds.isEmpty())
					{
						for (String instanceId : instanceIds)
						{
							int wear = instances.getOrDefault(instanceId, -1);
							if (wear < 0)
							{
								throw new AssertionError();
							}

							jsonWriter.beginObject();

							jsonWriter.name("guid").value(uuid);
							jsonWriter.name("count").value(1);
							jsonWriter.name("owned").value(true);

							jsonWriter.name("instance_data").beginObject();
							jsonWriter.name("id").value(instanceId);
							jsonWriter.name("health").value(((float) (itemInfo.maxWear - wear) / (float) itemInfo.maxWear) * 100.0f);
							jsonWriter.endObject();

							jsonWriter.name("category").beginObject();
							jsonWriter.name("loc").value(itemInfo.category.loc);
							jsonWriter.name("value").value(itemInfo.category.value);
							jsonWriter.endObject();
							jsonWriter.name("rarity").beginObject();
							jsonWriter.name("loc").value(itemInfo.rarity.loc);
							jsonWriter.name("value").value(itemInfo.rarity.value);
							jsonWriter.endObject();

							jsonWriter.endObject();
						}
					}
					else
					{
						jsonWriter.beginObject();

						jsonWriter.name("guid").value(uuid);
						jsonWriter.name("count").value(0);
						jsonWriter.name("owned").value(true);

						jsonWriter.name("category").beginObject();
						jsonWriter.name("loc").value(itemInfo.category.loc);
						jsonWriter.name("value").value(itemInfo.category.value);
						jsonWriter.endObject();
						jsonWriter.name("rarity").beginObject();
						jsonWriter.name("loc").value(itemInfo.rarity.loc);
						jsonWriter.name("value").value(itemInfo.rarity.value);
						jsonWriter.endObject();

						jsonWriter.endObject();
					}
				}
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

	private void addItem(@NotNull Item item)
	{
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(item.uuid);
		if (itemInfo.stackable)
		{
			this.stackableItems.put(item.uuid, this.stackableItems.getOrDefault(item.uuid, 0) + item.count);

			try
			{
				this.playerConnectorPluginWrapper.onPlayerInventoryAddItem(item.uuid, item.count);
			}
			catch (ConnectorPlugin.ConnectorPluginException exception)
			{
				LogManager.getLogger().error("Connector plugin threw exception when handling inventory item added {} {}", item.uuid, item.count, exception);
			}
		}
		else
		{

			if (item.instanceId == null)
			{
				LogManager.getLogger().warn("Non-stackable item with no instance ID");
				return;
			}

			this.nonStackableItems.computeIfAbsent(item.uuid, key -> new HashMap<>()).put(item.instanceId, item.wear);

			try
			{
				this.playerConnectorPluginWrapper.onPlayerInventoryAddItem(item.uuid, item.instanceId, item.wear);
			}
			catch (ConnectorPlugin.ConnectorPluginException exception)
			{
				LogManager.getLogger().error("Connector plugin threw exception when handling inventory item added {} {}", item.uuid, item.instanceId, exception);
			}
		}
	}

	private boolean takeItem(@NotNull Item item)
	{
		EarthItemCatalog.NameAndAux nameAndAux = EarthItemCatalog.getNameAndAux(item.uuid);
		if (nameAndAux == null)
		{
			LogManager.getLogger().warn("Cannot find item with UUID {}", item.uuid);
			return false;
		}
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(item.uuid);

		if (itemInfo.stackable)
		{
			if (this.stackableItems.getOrDefault(item.uuid, 0) < item.count)
			{
				return false;
			}

			this.stackableItems.put(item.uuid, this.stackableItems.getOrDefault(item.uuid, 0) - item.count);

			try
			{
				this.playerConnectorPluginWrapper.onPlayerInventoryRemoveItem(item.uuid, item.count);
			}
			catch (ConnectorPlugin.ConnectorPluginException exception)
			{
				LogManager.getLogger().error("Connector plugin threw exception when handling inventory item removed {} {}", item.uuid, item.count, exception);
			}

			return true;
		}
		else
		{
			if (item.instanceId == null)
			{
				LogManager.getLogger().warn("Non-stackable item with no instance ID");
				return false;
			}

			HashMap<String, Integer> instances = this.nonStackableItems.getOrDefault(item.uuid, null);
			if (instances == null)
			{
				return false;
			}

			if (!instances.containsKey(item.instanceId))
			{
				return false;
			}
			int wear = instances.remove(item.instanceId);

			try
			{
				this.playerConnectorPluginWrapper.onPlayerInventoryRemoveItem(item.uuid, item.instanceId);
			}
			catch (ConnectorPlugin.ConnectorPluginException exception)
			{
				LogManager.getLogger().error("Connector plugin threw exception when handling inventory item removed {} {}", item.uuid, item.instanceId, exception);
			}

			return true;
		}
	}

	private boolean updateItemWear(@NotNull Item item)
	{
		if (item.instanceId == null)
		{
			throw new IllegalArgumentException();
		}
		if (item.count != 1)
		{
			throw new AssertionError();
		}

		HashMap<String, Integer> instances = this.nonStackableItems.getOrDefault(item.uuid, null);
		if (instances == null)
		{
			return false;
		}
		if (!instances.containsKey(item.instanceId))
		{
			return false;
		}
		instances.put(item.instanceId, item.wear);

		try
		{
			this.playerConnectorPluginWrapper.onPlayerInventoryUpdateItemWear(item.uuid, item.instanceId, item.wear);
		}
		catch (ConnectorPlugin.ConnectorPluginException exception)
		{
			LogManager.getLogger().error("Connector plugin threw exception when handling inventory item wear updated {} {}", item.uuid, item.instanceId, exception);
		}

		return true;
	}

	private void sendHotbarToConnectorPlugin()
	{
		try
		{
			this.playerConnectorPluginWrapper.onPlayerInventorySetHotbar(Arrays.stream(this.hotbar).map(item -> item != null && item.count > 0 ? (item.instanceId != null ? new Inventory.HotbarItem(item.uuid, item.instanceId) : new Inventory.HotbarItem(item.uuid, item.count)) : null).toArray(Inventory.HotbarItem[]::new));
		}
		catch (ConnectorPlugin.ConnectorPluginException exception)
		{
			LogManager.getLogger().error("Connector plugin threw exception when handling hotbar change", exception);
		}
	}

	@Nullable
	private Item toGenoaItem(@NotNull ItemStack itemStack)
	{
		if (itemStack.getAmount() == 0)
		{
			return null;
		}

		int javaId = itemStack.getId();
		JavaItems.BedrockMapping bedrockMapping = JavaItems.getBedrockMapping(javaId);
		if (bedrockMapping == null)
		{
			LogManager.getLogger().warn("Attempt to translate item with no mapping {}", JavaItems.getName(javaId));
			return null;
		}
		String bedrockName = BedrockItems.getName(bedrockMapping.id);
		String uuid = EarthItemCatalog.getUUID(bedrockName, bedrockMapping.aux);
		if (uuid == null)
		{
			LogManager.getLogger().warn("Cannot find item UUID for {} {}", bedrockName, bedrockMapping.aux);
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

	@Nullable
	private static ItemStack toItemStack(@NotNull String name, int aux, int count)
	{
		return toItemStack(name, aux, null, 0, count);
	}

	@Nullable
	private static ItemStack toItemStack(@NotNull String name, int aux, @NotNull String instanceId, int wear)
	{
		return toItemStack(name, aux, instanceId, wear, 1);
	}

	@Nullable
	private static ItemStack toItemStack(@NotNull String name, int aux, @Nullable String instanceId, int wear, int count)
	{
		int bedrockId = BedrockItems.getId(name);
		if (bedrockId == 0)
		{
			LogManager.getLogger().warn("Cannot find Bedrock item for {}", name);
			return null;
		}

		int javaId = JavaItems.getJavaId(name, aux);
		if (javaId == -1)
		{
			LogManager.getLogger().warn("Cannot find Java item for {} {}", name, aux);
			return null;
		}

		if (instanceId != null)
		{
			CompoundTag nbt = new CompoundTag("");
			nbt.put(new IntTag("Damage", wear));
			nbt.put(new StringTag("GenoaInstanceId", instanceId));
			return new ItemStack(javaId, count, nbt);
		}
		else
		{
			return new ItemStack(javaId, count);
		}
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
			if (count <= 0)
			{
				throw new IllegalArgumentException();
			}
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