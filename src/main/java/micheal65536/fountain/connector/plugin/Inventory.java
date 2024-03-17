package micheal65536.fountain.connector.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public final class Inventory
{
	@NotNull
	private final StackableItem[] stackableItems;
	@NotNull
	private final NonStackableItem[] nonStackableItems;
	@Nullable
	private final HotbarItem[] hotbar;

	public Inventory(@NotNull StackableItem[] stackableItems, @NotNull NonStackableItem[] nonStackableItems, @Nullable HotbarItem[] hotbar)
	{
		// check that arrays are valid
		if (stackableItems == null || nonStackableItems == null || hotbar == null)
		{
			throw new IllegalArgumentException();
		}
		if (hotbar.length != 7)
		{
			throw new IllegalArgumentException();
		}
		if (Arrays.stream(stackableItems).anyMatch(stackableItem -> stackableItem == null) || Arrays.stream(nonStackableItems).anyMatch(nonStackableItem -> nonStackableItem == null))
		{
			throw new IllegalArgumentException();
		}

		// check that items aren't duplicated
		if (Arrays.stream(stackableItems).map(stackableItem -> stackableItem.uuid).anyMatch(uuid -> Arrays.stream(stackableItems).filter(stackableItem -> stackableItem.uuid.equals(uuid)).count() != 1))
		{
			throw new IllegalArgumentException();
		}
		if (Arrays.stream(nonStackableItems).map(nonStackableItem -> nonStackableItem.instanceId).anyMatch(instanceId -> Arrays.stream(nonStackableItems).filter(nonStackableItem -> nonStackableItem.instanceId.equals(instanceId)).count() != 1))
		{
			throw new IllegalArgumentException();
		}

		// check that the same item UUID doesn't appear in both the stackable and the non-stackable lists
		if (Arrays.stream(stackableItems).map(stackableItem -> stackableItem.uuid).anyMatch(uuid -> Arrays.stream(nonStackableItems).anyMatch(nonStackableItem -> nonStackableItem.uuid.equals(uuid))))
		{
			throw new IllegalArgumentException();
		}
		if (Arrays.stream(nonStackableItems).map(nonStackableItem -> nonStackableItem.uuid).anyMatch(uuid -> Arrays.stream(stackableItems).anyMatch(stackableItem -> stackableItem.uuid.equals(uuid))))
		{
			throw new IllegalArgumentException();
		}

		// check that the hotbar doesn't contain items that aren't in the main lists
		HashMap<String, Integer> hotbarItemCounts = new HashMap<>();
		HashMap<String, HashSet<String>> hotbarItemInstances = new HashMap<>();
		for (HotbarItem hotbarItem : hotbar)
		{
			if (hotbarItem == null)
			{
				continue;
			}
			if (hotbarItem.instanceId != null)
			{
				if (!hotbarItemInstances.computeIfAbsent(hotbarItem.uuid, uuid -> new HashSet<>()).add(hotbarItem.instanceId))
				{
					throw new IllegalArgumentException();
				}
			}
			else
			{
				hotbarItemCounts.put(hotbarItem.uuid, hotbarItemCounts.getOrDefault(hotbarItem.uuid, 0) + hotbarItem.count);
			}
		}
		if (hotbarItemCounts.entrySet().stream().anyMatch(entry -> entry.getValue() > Arrays.stream(stackableItems).filter(stackableItem -> stackableItem.uuid.equals(entry.getKey())).findFirst().map(stackableItem -> stackableItem.count).orElse(0)))
		{
			throw new IllegalArgumentException();
		}
		if (hotbarItemInstances.entrySet().stream().anyMatch(entry -> entry.getValue().stream().anyMatch(instanceId -> Arrays.stream(nonStackableItems).filter(nonStackableItem -> nonStackableItem.uuid.equals(entry.getKey()) && nonStackableItem.instanceId.equals(instanceId)).count() != 1)))
		{
			throw new IllegalArgumentException();
		}

		this.stackableItems = Arrays.copyOf(stackableItems, stackableItems.length);
		this.nonStackableItems = Arrays.copyOf(nonStackableItems, nonStackableItems.length);
		this.hotbar = Arrays.copyOf(hotbar, hotbar.length);
	}

	public StackableItem[] getStackableItems()
	{
		return Arrays.copyOf(this.stackableItems, this.stackableItems.length);
	}

	public NonStackableItem[] getNonStackableItems()
	{
		return Arrays.copyOf(this.nonStackableItems, this.nonStackableItems.length);
	}

	public HotbarItem[] getHotbar()
	{
		return Arrays.copyOf(this.hotbar, this.hotbar.length);
	}

	public static final class StackableItem
	{
		@NotNull
		public final String uuid;
		public final int count;

		public StackableItem(@NotNull String uuid, int count)
		{
			if (count < 1)
			{
				throw new IllegalArgumentException();
			}
			this.uuid = uuid;
			this.count = count;
		}
	}

	public static final class NonStackableItem
	{
		@NotNull
		public final String uuid;
		@NotNull
		public final String instanceId;
		public final int wear;

		public NonStackableItem(@NotNull String uuid, @NotNull String instanceId, int wear)
		{
			if (wear < 0)
			{
				throw new IllegalArgumentException();
			}
			this.uuid = uuid;
			this.instanceId = instanceId;
			this.wear = wear;
		}
	}

	public static final class HotbarItem
	{
		@NotNull
		public final String uuid;
		public final int count;
		@Nullable
		public final String instanceId;

		public HotbarItem(@NotNull String uuid, int count)
		{
			if (count < 1)
			{
				throw new IllegalArgumentException();
			}
			this.uuid = uuid;
			this.count = count;
			this.instanceId = null;
		}

		public HotbarItem(@NotNull String uuid, @NotNull String instanceId)
		{
			this.uuid = uuid;
			this.count = 1;
			this.instanceId = instanceId;
		}
	}
}