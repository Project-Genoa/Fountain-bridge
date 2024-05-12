package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.connector.PlayerConnectorPluginWrapper;
import micheal65536.fountain.connector.plugin.ConnectorPlugin;
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

public final class GenoaInventoryHelper
{
	private final FabricRegistryManager fabricRegistryManager;

	private final PlayerConnectorPluginWrapper playerConnectorPluginWrapper;

	public GenoaInventoryHelper(@NotNull FabricRegistryManager fabricRegistryManager, @NotNull PlayerConnectorPluginWrapper playerConnectorPluginWrapper)
	{
		this.fabricRegistryManager = fabricRegistryManager;

		this.playerConnectorPluginWrapper = playerConnectorPluginWrapper;
	}

	@NotNull
	public Inventory getInventoryFromConnectorPlugin()
	{
		micheal65536.fountain.connector.plugin.Inventory connectorPluginInventory;
		try
		{
			connectorPluginInventory = this.playerConnectorPluginWrapper.onPlayerGetInventory();
		}
		catch (ConnectorPlugin.ConnectorPluginException exception)
		{
			LogManager.getLogger().warn("Connector plugin threw exception when getting player inventory", exception);
			return new Inventory();
		}

		Inventory inventory = new Inventory();
		for (micheal65536.fountain.connector.plugin.Inventory.StackableItem stackableItem : connectorPluginInventory.getStackableItems())
		{
			inventory.stackableItems.put(stackableItem.uuid, stackableItem.count);
		}
		for (micheal65536.fountain.connector.plugin.Inventory.NonStackableItem nonStackableItem : connectorPluginInventory.getNonStackableItems())
		{
			inventory.nonStackableItems.computeIfAbsent(nonStackableItem.uuid, uuid -> new HashMap<>()).put(nonStackableItem.instanceId, nonStackableItem.wear);
		}
		micheal65536.fountain.connector.plugin.Inventory.HotbarItem[] hotbar = connectorPluginInventory.getHotbar();
		for (int index = 0; index < 7; index++)
		{
			micheal65536.fountain.connector.plugin.Inventory.HotbarItem hotbarItem = hotbar[index];
			if (hotbarItem == null)
			{
				inventory.hotbar[index] = null;
			}
			else
			{
				if (hotbarItem.instanceId != null)
				{
					inventory.hotbar[index] = new Item(hotbarItem.uuid, hotbarItem.instanceId, inventory.nonStackableItems.get(hotbarItem.uuid).get(hotbarItem.instanceId));
				}
				else
				{
					inventory.hotbar[index] = new Item(hotbarItem.uuid, hotbarItem.count);
				}
			}
		}
		return inventory;
	}

	public Item[] readHotbarFromClient(@NotNull String jsonString)
	{
		try
		{
			Item[] hotbar = new Item[7];
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
						hotbar[index] = new Item(uuid, instanceId, 0);
					}
					else
					{
						hotbar[index] = new Item(uuid, count);
					}
				}
				index++;
			}
			return hotbar;
		}
		catch (Exception exception)
		{
			LogManager.getLogger().warn("Invalid JSON in readHotbarFromClient");
			return null;
		}
	}

	@NotNull
	public String makeGenoaInventoryDataJSON(@NotNull Inventory inventory)
	{
		try
		{
			StringWriter stringWriter = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(stringWriter);

			jsonWriter.beginObject();

			jsonWriter.name("hotbar").beginArray();
			for (Item item : inventory.hotbar)
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
			itemIds.addAll(inventory.stackableItems.keySet());
			itemIds.addAll(inventory.nonStackableItems.keySet());
			for (Item item : inventory.hotbar)
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

			jsonWriter.name("inventory").beginArray();
			for (String uuid : itemIds)
			{
				EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(uuid);

				if (itemInfo.stackable)
				{
					int count = inventory.stackableItems.getOrDefault(uuid, 0) - hotbarItemCounts.getOrDefault(uuid, 0);
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
					HashMap<String, Integer> instances = inventory.nonStackableItems.getOrDefault(uuid, new HashMap<>());
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

	public void addItem(@NotNull Item item)
	{
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(item.uuid);
		if (itemInfo.stackable)
		{
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

	public int takeItem(@NotNull Item item)
	{
		EarthItemCatalog.NameAndAux nameAndAux = EarthItemCatalog.getNameAndAux(item.uuid);
		if (nameAndAux == null)
		{
			LogManager.getLogger().warn("Cannot find item with UUID {}", item.uuid);
			return 0;
		}
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(item.uuid);

		if (itemInfo.stackable)
		{
			try
			{
				return this.playerConnectorPluginWrapper.onPlayerInventoryRemoveItem(item.uuid, item.count);
			}
			catch (ConnectorPlugin.ConnectorPluginException exception)
			{
				LogManager.getLogger().error("Connector plugin threw exception when handling inventory item removed {} {}", item.uuid, item.count, exception);
				return 0;
			}
		}
		else
		{
			if (item.instanceId == null)
			{
				LogManager.getLogger().warn("Non-stackable item with no instance ID");
				return 0;
			}

			try
			{
				return this.playerConnectorPluginWrapper.onPlayerInventoryRemoveItem(item.uuid, item.instanceId) ? 1 : 0;
			}
			catch (ConnectorPlugin.ConnectorPluginException exception)
			{
				LogManager.getLogger().error("Connector plugin threw exception when handling inventory item removed {} {}", item.uuid, item.instanceId, exception);
				return 0;
			}
		}
	}

	public void updateItemWear(@NotNull Item item)
	{
		if (item.instanceId == null)
		{
			throw new IllegalArgumentException();
		}
		if (item.count != 1)
		{
			throw new AssertionError();
		}

		try
		{
			this.playerConnectorPluginWrapper.onPlayerInventoryUpdateItemWear(item.uuid, item.instanceId, item.wear);
		}
		catch (ConnectorPlugin.ConnectorPluginException exception)
		{
			LogManager.getLogger().error("Connector plugin threw exception when handling inventory item wear updated {} {}", item.uuid, item.instanceId, exception);
		}
	}

	public void setHotbar(Item[] hotbar)
	{
		if (hotbar.length != 7)
		{
			throw new IllegalArgumentException();
		}
		try
		{
			this.playerConnectorPluginWrapper.onPlayerInventorySetHotbar(Arrays.stream(hotbar).map(item -> item != null && item.count > 0 ? (item.instanceId != null ? new micheal65536.fountain.connector.plugin.Inventory.HotbarItem(item.uuid, item.instanceId) : new micheal65536.fountain.connector.plugin.Inventory.HotbarItem(item.uuid, item.count)) : null).toArray(micheal65536.fountain.connector.plugin.Inventory.HotbarItem[]::new));
		}
		catch (ConnectorPlugin.ConnectorPluginException exception)
		{
			LogManager.getLogger().error("Connector plugin threw exception when handling hotbar change", exception);
		}
	}

	@Nullable
	public Item toGenoaItem(@NotNull ItemStack itemStack)
	{
		if (itemStack.getAmount() == 0)
		{
			return null;
		}

		int javaId = itemStack.getId();
		JavaItems.BedrockMapping bedrockMapping = JavaItems.getBedrockMapping(javaId, this.fabricRegistryManager);
		if (bedrockMapping == null)
		{
			LogManager.getLogger().warn("Attempt to translate item with no mapping {}", JavaItems.getName(javaId, this.fabricRegistryManager));
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
	public ItemStack toItemStack(@Nullable Item item)
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
			return this.toItemStack(nameAndAux.name, nameAndAux.aux, item.count);
		}
		else
		{
			if (item.instanceId == null)
			{
				LogManager.getLogger().warn("Non-stackable item with no instance ID");
				return null;
			}
			return this.toItemStack(nameAndAux.name, nameAndAux.aux, item.instanceId, item.wear);
		}
	}

	@Nullable
	private ItemStack toItemStack(@NotNull String name, int aux, int count)
	{
		return this.toItemStack(name, aux, null, 0, count);
	}

	@Nullable
	private ItemStack toItemStack(@NotNull String name, int aux, @NotNull String instanceId, int wear)
	{
		return this.toItemStack(name, aux, instanceId, wear, 1);
	}

	@Nullable
	private ItemStack toItemStack(@NotNull String name, int aux, @Nullable String instanceId, int wear, int count)
	{
		int bedrockId = BedrockItems.getId(name);
		if (bedrockId == 0)
		{
			LogManager.getLogger().warn("Cannot find Bedrock item for {}", name);
			return null;
		}

		int javaId = JavaItems.getJavaId(name, aux, this.fabricRegistryManager);
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

	@NotNull
	public ItemData toItemData(@Nullable Item item)
	{
		ItemStack itemStack = this.toItemStack(item);
		if (itemStack == null || itemStack.getAmount() == 0)
		{
			return ItemData.builder().build();
		}
		else
		{
			return ItemTranslator.translateJavaToBedrock(itemStack, this.fabricRegistryManager);
		}
	}

	public static final class Inventory
	{
		public final HashMap<String, Integer> stackableItems = new HashMap<>();
		public final HashMap<String, HashMap<String, Integer>> nonStackableItems = new HashMap<>();
		public final Item[] hotbar;

		private Inventory()
		{
			this.hotbar = new Item[7];
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

		public Item(@NotNull String uuid, int count)
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

		public Item(@NotNull String uuid, @NotNull String instanceId, int wear)
		{
			if (wear < 0)
			{
				throw new IllegalArgumentException();
			}
			this.uuid = uuid;
			this.instanceId = instanceId;
			this.wear = wear;
			this.count = 1;
		}
	}
}