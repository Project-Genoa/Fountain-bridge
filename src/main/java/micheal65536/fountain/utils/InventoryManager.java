package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.protocol.bedrock.packet.GenoaInventoryDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.PlayerSession;
import micheal65536.fountain.connector.PlayerConnectorPluginWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.IntStream;

public class InventoryManager
{
	private final PlayerSession playerSession;

	private final MinecraftCodecHelper minecraftCodecHelper;

	private final FabricRegistryManager fabricRegistryManager;
	private final PlayerConnectorPluginWrapper playerConnectorPluginWrapper;
	private final GenoaInventoryHelper genoaInventoryHelper;

	private boolean initialiseSent = false;

	private boolean inventorySyncRequestSent = false;
	private boolean inventorySyncRequestQueued;
	private boolean sentInventorySyncRequestClearHotbar;
	private boolean queuedInventorySyncRequestClearHotbar;
	private boolean setHotbarRequestSent = false;

	private GenoaInventoryHelper.Item[] sentServerHotbar;
	private GenoaInventoryHelper.Item[] currentServerHotbar;

	public InventoryManager(@NotNull PlayerSession playerSession, @NotNull FabricRegistryManager fabricRegistryManager, @NotNull MinecraftCodecHelper minecraftCodecHelper, @NotNull PlayerConnectorPluginWrapper playerConnectorPluginWrapper)
	{
		this.playerSession = playerSession;

		this.minecraftCodecHelper = minecraftCodecHelper;

		this.fabricRegistryManager = fabricRegistryManager;
		this.playerConnectorPluginWrapper = playerConnectorPluginWrapper;
		this.genoaInventoryHelper = new GenoaInventoryHelper(this.fabricRegistryManager, this.playerConnectorPluginWrapper);

		this.currentServerHotbar = new GenoaInventoryHelper.Item[7];
	}

	public void initialiseServerInventory()
	{
		if (!this.initialiseSent)
		{
			GenoaInventoryHelper.Inventory inventory = this.genoaInventoryHelper.getInventoryFromConnectorPlugin();
			this.sendInitialSetHotbarRequest(inventory.hotbar);
		}
	}

	public void syncInventoryFromServer()
	{
		this.queueInventorySyncRequest(false);
	}

	public void onGenoaHotbarChange(@NotNull String json)
	{
		GenoaInventoryHelper.Item[] hotbar = this.genoaInventoryHelper.readHotbarFromClient(json);
		if (hotbar == null)
		{
			return;
		}
		this.genoaInventoryHelper.setHotbar(hotbar);
		this.queueInventorySyncRequest(true);
	}

	public void sendClientHotbar()
	{
		InventoryContentPacket inventoryContentPacket = new InventoryContentPacket();
		inventoryContentPacket.setContainerId(0);
		inventoryContentPacket.setContents(Arrays.stream(this.genoaInventoryHelper.getInventoryFromConnectorPlugin().hotbar).map(this.genoaInventoryHelper::toItemData).toList());
		this.playerSession.sendBedrockPacket(inventoryContentPacket);
	}

	public void sendClientGenoaInventory()
	{
		GenoaInventoryDataPacket genoaInventoryDataPacket = new GenoaInventoryDataPacket();
		genoaInventoryDataPacket.setJson(this.genoaInventoryHelper.makeGenoaInventoryDataJSON(this.genoaInventoryHelper.getInventoryFromConnectorPlugin()));
		this.playerSession.sendBedrockPacket(genoaInventoryDataPacket);
	}

	public void onInventorySyncResponse(ItemStack[] itemStacks, ItemStack[] hotbar)
	{
		if (!this.inventorySyncRequestSent)
		{
			LogManager.getLogger().warn("Server sent unexpected inventory sync response");
			return;
		}

		for (ItemStack itemStack : itemStacks)
		{
			GenoaInventoryHelper.Item item = this.genoaInventoryHelper.toGenoaItem(itemStack);
			if (item != null)
			{
				this.genoaInventoryHelper.addItem(item);
			}
		}

		GenoaInventoryHelper.Item[] genoaInventoryHelperHotbar = Arrays.stream(hotbar).map(itemStack -> itemStack != null ? this.genoaInventoryHelper.toGenoaItem(itemStack) : null).toArray(GenoaInventoryHelper.Item[]::new);
		if (this.sentInventorySyncRequestClearHotbar)
		{
			this.handleServerHotbarChange(genoaInventoryHelperHotbar);
			this.currentServerHotbar = new GenoaInventoryHelper.Item[7];

			this.sendSetHotbarRequest(this.genoaInventoryHelper.getInventoryFromConnectorPlugin().hotbar);
		}
		else
		{
			this.handleServerHotbarChange(genoaInventoryHelperHotbar);

			this.genoaInventoryHelper.setHotbar(genoaInventoryHelperHotbar);
			this.sendClientHotbar();

			this.inventorySyncRequestSent = false;
			this.sendQueuedInventorySyncRequest();
		}
	}

	public void onSetHotbarResponse(boolean success)
	{
		if (!this.setHotbarRequestSent)
		{
			LogManager.getLogger().warn("Server sent unexpected set hotbar response");
			return;
		}

		if (!this.inventorySyncRequestSent)
		{
			throw new AssertionError();
		}

		if (success)
		{
			this.currentServerHotbar = this.sentServerHotbar;
			this.queueInventorySyncRequest(false);
			this.sendQueuedInventorySyncRequest();
		}
		else
		{
			this.queueInventorySyncRequest(true);
			this.sendQueuedInventorySyncRequest();
		}
	}

	private void handleServerHotbarChange(GenoaInventoryHelper.Item[] newHotbar)
	{
		int[] changedIndexes = IntStream.range(0, 7).filter(index ->
		{
			GenoaInventoryHelper.Item currentItem = this.currentServerHotbar[index];
			GenoaInventoryHelper.Item newItem = newHotbar[index];
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
			GenoaInventoryHelper.Item item = this.currentServerHotbar[index];
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
			GenoaInventoryHelper.Item item = newHotbar[index];
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
				this.genoaInventoryHelper.addItem(new GenoaInventoryHelper.Item(uuid, count));
			}
			else if (count < 0)
			{
				int removedCount = this.genoaInventoryHelper.takeItem(new GenoaInventoryHelper.Item(uuid, -count));
				if (removedCount < -count)
				{
					LogManager.getLogger().warn("Attempted to remove item {} {} that is not in inventory", uuid, (-count) - removedCount);
				}
			}
		});
		addedNonStackableItems.forEach((uuid, instances) ->
		{
			instances.forEach((instanceId, wear) ->
			{
				this.genoaInventoryHelper.addItem(new GenoaInventoryHelper.Item(uuid, instanceId, wear));
			});
		});
		removedNonStackableItems.forEach((uuid, instances) ->
		{
			instances.forEach(instanceId ->
			{
				int removedCount = this.genoaInventoryHelper.takeItem(new GenoaInventoryHelper.Item(uuid, instanceId, 0));
				if (removedCount == 0)
				{
					LogManager.getLogger().warn("Attempted to remove item {} {} that is not in inventory", uuid, instanceId);
				}
				else if (removedCount > 1)
				{
					throw new AssertionError();
				}
			});
		});
		updatedNonStackableItems.forEach((uuid, instances) ->
		{
			instances.forEach((instanceId, wear) ->
			{
				this.genoaInventoryHelper.updateItemWear(new GenoaInventoryHelper.Item(uuid, instanceId, wear));
			});
		});

		for (int index : changedIndexes)
		{
			this.currentServerHotbar[index] = newHotbar[index];
		}
	}

	private void queueInventorySyncRequest(boolean clearHotbar)
	{
		if (this.inventorySyncRequestSent || !this.initialiseSent || this.inventorySyncRequestQueued)
		{
			this.inventorySyncRequestQueued = true;
			this.queuedInventorySyncRequestClearHotbar |= clearHotbar;
		}
		else
		{
			this.sendInventorySyncRequest(clearHotbar);
		}
	}

	private void sendQueuedInventorySyncRequest()
	{
		if (this.inventorySyncRequestQueued)
		{
			this.sendInventorySyncRequest(this.queuedInventorySyncRequestClearHotbar);
			this.inventorySyncRequestQueued = false;
			this.queuedInventorySyncRequestClearHotbar = false;
		}
	}

	private void sendInventorySyncRequest(boolean clearHotbar)
	{
		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
		byteBuf.writeBoolean(clearHotbar);
		byte[] data = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(data);
		ServerboundCustomPayloadPacket serverboundCustomPayloadPacket = new ServerboundCustomPayloadPacket("fountain:inventory_sync_request", data);
		this.playerSession.sendJavaPacket(serverboundCustomPayloadPacket);

		this.inventorySyncRequestSent = true;
		this.sentInventorySyncRequestClearHotbar = clearHotbar;
	}

	private void sendSetHotbarRequest(GenoaInventoryHelper.Item[] hotbar)
	{
		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
		byteBuf.writeBoolean(false);
		for (GenoaInventoryHelper.Item item : hotbar)
		{
			try
			{
				this.minecraftCodecHelper.writeItemStack(byteBuf, this.genoaInventoryHelper.toItemStack(item));
			}
			catch (IOException exception)
			{
				throw new AssertionError(exception);
			}
		}
		byte[] data = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(data);
		ServerboundCustomPayloadPacket serverboundCustomPayloadPacket = new ServerboundCustomPayloadPacket("fountain:set_hotbar_request", data);
		this.playerSession.sendJavaPacket(serverboundCustomPayloadPacket);

		this.sentServerHotbar = hotbar;
		this.setHotbarRequestSent = true;
	}

	private void sendInitialSetHotbarRequest(GenoaInventoryHelper.Item[] hotbar)
	{
		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
		byteBuf.writeBoolean(true);
		for (GenoaInventoryHelper.Item item : hotbar)
		{
			try
			{
				this.minecraftCodecHelper.writeItemStack(byteBuf, this.genoaInventoryHelper.toItemStack(item));
			}
			catch (IOException exception)
			{
				throw new AssertionError(exception);
			}
		}
		byte[] data = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(data);
		ServerboundCustomPayloadPacket serverboundCustomPayloadPacket = new ServerboundCustomPayloadPacket("fountain:set_hotbar_request", data);
		this.playerSession.sendJavaPacket(serverboundCustomPayloadPacket);

		this.currentServerHotbar = hotbar;
		this.initialiseSent = true;

		this.queueInventorySyncRequest(false);
		this.sendQueuedInventorySyncRequest();
	}
}