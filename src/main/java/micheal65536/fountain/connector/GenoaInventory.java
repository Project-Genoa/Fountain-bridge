package micheal65536.fountain.connector;

import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.connector.plugin.Inventory;
import micheal65536.fountain.registry.EarthItemCatalog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public final class GenoaInventory
{
	public final HashMap<String, Integer> stackableItems = new HashMap<>();
	public final HashMap<String, HashMap<String, Integer>> nonStackableItems = new HashMap<>();
	public final Item[] hotbar;

	public GenoaInventory(@NotNull Inventory inventory)
	{
		this.hotbar = new Item[7];

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

	public void addItem(@NotNull String uuid, int count)
	{
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(uuid);
		if (itemInfo.stackable)
		{
			this.stackableItems.put(uuid, this.stackableItems.getOrDefault(uuid, 0) + count);
		}
		else
		{
			LogManager.getLogger().warn("Attempting to add non-stackable item as stackable");
		}
	}

	public void addItem(@NotNull String uuid, @NotNull String instanceId, int wear)
	{
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(uuid);
		if (!itemInfo.stackable)
		{
			this.nonStackableItems.computeIfAbsent(uuid, key -> new HashMap<>()).put(instanceId, wear);
		}
		else
		{
			LogManager.getLogger().warn("Attempting to add stackable item as non-stackable");
		}
	}

	public int takeItem(@NotNull String uuid, int count)
	{
		EarthItemCatalog.NameAndAux nameAndAux = EarthItemCatalog.getNameAndAux(uuid);
		if (nameAndAux == null)
		{
			LogManager.getLogger().warn("Cannot find item with UUID {}", uuid);
			return 0;
		}
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(uuid);

		if (itemInfo.stackable)
		{
			count = Math.min(this.stackableItems.getOrDefault(uuid, 0), count);
			this.stackableItems.put(uuid, this.stackableItems.getOrDefault(uuid, 0) - count);
			this.limitHotbarToInventory();
			return count;
		}
		else
		{
			LogManager.getLogger().warn("Attempting to take non-stackable item as stackable");
			return 0;
		}
	}

	public boolean takeItem(@NotNull String uuid, @NotNull String instanceId)
	{
		EarthItemCatalog.NameAndAux nameAndAux = EarthItemCatalog.getNameAndAux(uuid);
		if (nameAndAux == null)
		{
			LogManager.getLogger().warn("Cannot find item with UUID {}", uuid);
			return false;
		}
		EarthItemCatalog.ItemInfo itemInfo = EarthItemCatalog.getItemInfo(uuid);

		if (!itemInfo.stackable)
		{
			HashMap<String, Integer> instances = this.nonStackableItems.getOrDefault(uuid, null);
			if (instances == null)
			{
				return false;
			}
			if (!instances.containsKey(instanceId))
			{
				return false;
			}
			instances.remove(instanceId);
			this.limitHotbarToInventory();
			return true;
		}
		else
		{
			LogManager.getLogger().warn("Attempting to take stackable item as non-stackable");
			return false;
		}
	}

	public void updateItemWear(@NotNull String uuid, @NotNull String instanceId, int wear)
	{
		HashMap<String, Integer> instances = this.nonStackableItems.getOrDefault(uuid, null);
		if (instances == null)
		{
			LogManager.getLogger().warn("Attempting to update wear for item instance that does not exist");
			return;
		}
		if (!instances.containsKey(instanceId))
		{
			LogManager.getLogger().warn("Attempting to update wear for item instance that does not exist");
			return;
		}
		instances.put(instanceId, wear);
	}

	public void setHotbar(Inventory.HotbarItem[] hotbar)
	{
		Item[] newHotbarItems = Arrays.stream(hotbar).map(hotbarItem ->
		{
			if (hotbarItem == null)
			{
				return null;
			}
			else
			{
				if (hotbarItem.instanceId != null)
				{
					return new Item(hotbarItem.uuid, hotbarItem.instanceId, -1);
				}
				else
				{
					return new Item(hotbarItem.uuid, hotbarItem.count);
				}
			}
		}).toArray(Item[]::new);

		for (int index = 0; index < 7; index++)
		{
			this.hotbar[index] = newHotbarItems[index];
		}

		this.limitHotbarToInventory();
	}

	private void limitHotbarToInventory()
	{
		HashMap<String, Integer> hotbarItemCounts = new HashMap<>();
		HashMap<String, HashSet<String>> hotbarItemInstances = new HashMap<>();
		for (int index = 0; index < 7; index++)
		{
			Item item = this.hotbar[index];
			if (item != null)
			{
				if (item.instanceId == null)
				{
					int availableCount = this.stackableItems.getOrDefault(item.uuid, 0) - hotbarItemCounts.getOrDefault(item.uuid, 0);
					hotbarItemCounts.put(item.uuid, hotbarItemCounts.getOrDefault(item.uuid, 0) + item.count);
					if (item.count > availableCount)
					{
						if (availableCount <= 0)
						{
							item = null;
						}
						else
						{
							item = new Item(item.uuid, availableCount);
						}
					}
				}
				else
				{
					HashSet<String> hotbarInstances = hotbarItemInstances.computeIfAbsent(item.uuid, uuid -> new HashSet<>());
					if (!hotbarInstances.add(item.instanceId))
					{
						item = null;
					}
					else
					{
						HashMap<String, Integer> instances = this.nonStackableItems.getOrDefault(item.uuid, null);
						if (instances == null)
						{
							item = null;
						}
						else if (!instances.containsKey(item.instanceId))
						{
							item = null;
						}
					}
				}
				this.hotbar[index] = item;
			}
		}
	}

	private static final class Item
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
			this.uuid = uuid;
			this.instanceId = instanceId;
			this.wear = wear;
			this.count = 1;
		}
	}
}