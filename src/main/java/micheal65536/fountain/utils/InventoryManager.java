package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.GenoaInventoryDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.PlayerSession;

import java.io.IOException;
import java.util.Arrays;

public class InventoryManager
{
	private final PlayerSession playerSession;
	private final MinecraftCodecHelper minecraftCodecHelper;

	private final GenoaInventory genoaInventory;

	private boolean initialiseSent = false;

	private boolean inventorySyncRequestSent = false;
	private boolean inventorySyncRequestQueued;
	private boolean sentInventorySyncRequestClearHotbar;
	private boolean queuedInventorySyncRequestClearHotbar;

	private boolean updatePending = false;
	private String pendingUpdateJSON;
	private boolean setHotbarRequestSent = false;

	public InventoryManager(@NotNull PlayerSession playerSession, @NotNull MinecraftCodecHelper minecraftCodecHelper)
	{
		this.playerSession = playerSession;
		this.minecraftCodecHelper = minecraftCodecHelper;

		this.genoaInventory = new GenoaInventory(); // TODO: initialise from API server
	}

	public void initialiseServerInventory()
	{
		if (!this.initialiseSent)
		{
			this.sendInitialSetHotbarRequest(this.genoaInventory.getHotbarForJavaServer());
		}
	}

	public void syncInventory()
	{
		this.syncInventory(false);
	}

	private void syncInventory(boolean clearHotbar)
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

	public void onGenoaHotbarChange(@NotNull String json)
	{
		this.updatePending = true;
		this.pendingUpdateJSON = json;
		this.syncInventory(true);
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
			this.genoaInventory.addItemFromJavaServer(itemStack);
		}

		if (this.sentInventorySyncRequestClearHotbar)
		{
			this.genoaInventory.setHotbarFromJavaServer(hotbar);

			if (!this.updatePending)
			{
				throw new AssertionError();
			}
			this.genoaInventory.updateHotbarFromClient(this.pendingUpdateJSON);

			this.sendSetHotbarRequest(this.genoaInventory.getHotbarForJavaServer());
		}
		else
		{
			this.genoaInventory.setHotbarFromJavaServer(hotbar);

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
		if (!this.updatePending)
		{
			throw new AssertionError();
		}

		if (success)
		{
			this.updatePending = false;
			this.syncInventory(false);
			this.sendQueuedInventorySyncRequest();
		}
		else
		{
			this.genoaInventory.clearHotbar();
			this.syncInventory(true);
			this.sendQueuedInventorySyncRequest();
		}
	}

	public void sendClientHotbar()
	{
		InventoryContentPacket inventoryContentPacket = new InventoryContentPacket();
		inventoryContentPacket.setContainerId(0);
		inventoryContentPacket.setContents(Arrays.stream(this.genoaInventory.getHotbarForJavaServer()).map(itemStack ->
		{
			if (itemStack == null || itemStack.getAmount() == 0)
			{
				return ItemData.builder().build();
			}
			else
			{
				return ItemTranslator.translateJavaToBedrock(itemStack);
			}
		}).toList());
		this.playerSession.sendBedrockPacket(inventoryContentPacket);
	}

	public void sendClientGenoaInventory()
	{
		GenoaInventoryDataPacket genoaInventoryDataPacket = new GenoaInventoryDataPacket();
		genoaInventoryDataPacket.setJson(this.genoaInventory.getGenoaInventoryResponseJSON());
		this.playerSession.sendBedrockPacket(genoaInventoryDataPacket);
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

	private void sendSetHotbarRequest(ItemStack[] hotbar)
	{
		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
		byteBuf.writeBoolean(false);
		for (ItemStack itemStack : hotbar)
		{
			try
			{
				this.minecraftCodecHelper.writeItemStack(byteBuf, itemStack);
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

		this.setHotbarRequestSent = true;
	}

	private void sendInitialSetHotbarRequest(ItemStack[] hotbar)
	{
		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
		byteBuf.writeBoolean(true);
		for (ItemStack itemStack : hotbar)
		{
			try
			{
				this.minecraftCodecHelper.writeItemStack(byteBuf, itemStack);
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

		this.initialiseSent = true;
		this.syncInventory(false);
		this.sendQueuedInventorySyncRequest();
	}
}