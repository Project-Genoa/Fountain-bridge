package micheal65536.fountain.connector;

import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.connector.plugin.ConnectorPlugin;
import micheal65536.fountain.connector.plugin.DisconnectResponse;
import micheal65536.fountain.connector.plugin.Inventory;
import micheal65536.fountain.connector.plugin.Logger;
import micheal65536.fountain.connector.plugin.PlayerLoginInfo;

import java.util.HashMap;

public final class DefaultConnectorPlugin implements ConnectorPlugin
{
	private final HashMap<String, GenoaInventory> playerInventories = new HashMap<>();

	@Override
	public void init(@NotNull String arg, @NotNull Logger logger) throws ConnectorPluginException
	{
		// empty
	}

	@Override
	public void shutdown() throws ConnectorPluginException
	{
		// empty
	}

	@Override
	public void onServerReady() throws ConnectorPluginException
	{
		// empty
	}

	@Override
	public void onServerStopping() throws ConnectorPluginException
	{
		// empty
	}

	@Override
	public void onWorldSaved(byte[] data) throws ConnectorPluginException
	{
		// empty
	}

	@Override
	public boolean onPlayerConnected(@NotNull PlayerLoginInfo playerLoginInfo) throws ConnectorPluginException
	{
		this.playerInventories.put(playerLoginInfo.uuid, new GenoaInventory(new Inventory(new Inventory.StackableItem[0], new Inventory.NonStackableItem[0], new Inventory.HotbarItem[7])));
		return true;
	}

	@Override
	@NotNull
	public DisconnectResponse onPlayerDisconnected(@NotNull String playerId) throws ConnectorPluginException
	{
		this.playerInventories.remove(playerId);
		return new DisconnectResponse();
	}

	@Override
	@NotNull
	public Inventory onPlayerGetInventory(@NotNull String playerId) throws ConnectorPluginException
	{
		return this.getInventoryForPlayer(playerId).toConnectorPluginInventory();
	}

	@Override
	public void onPlayerInventoryAddItem(@NotNull String playerId, @NotNull String itemId, int count) throws ConnectorPluginException
	{
		this.getInventoryForPlayer(playerId).addItem(itemId, count);
	}

	@Override
	public void onPlayerInventoryAddItem(@NotNull String playerId, @NotNull String itemId, @NotNull String instanceId, int wear) throws ConnectorPluginException
	{
		this.getInventoryForPlayer(playerId).addItem(itemId, instanceId, wear);
	}

	@Override
	public int onPlayerInventoryRemoveItem(@NotNull String playerId, @NotNull String itemId, int count) throws ConnectorPluginException
	{
		return this.getInventoryForPlayer(playerId).takeItem(itemId, count);
	}

	@Override
	public boolean onPlayerInventoryRemoveItem(@NotNull String playerId, @NotNull String itemId, @NotNull String instanceId) throws ConnectorPluginException
	{
		return this.getInventoryForPlayer(playerId).takeItem(itemId, instanceId);
	}

	@Override
	public void onPlayerInventoryUpdateItemWear(@NotNull String playerId, @NotNull String itemId, @NotNull String instanceId, int wear) throws ConnectorPluginException
	{
		this.getInventoryForPlayer(playerId).updateItemWear(itemId, instanceId, wear);
	}

	@Override
	public void onPlayerInventorySetHotbar(@NotNull String playerId, Inventory.HotbarItem[] hotbar) throws ConnectorPluginException
	{
		this.getInventoryForPlayer(playerId).setHotbar(hotbar);
	}

	@NotNull
	private GenoaInventory getInventoryForPlayer(@NotNull String playerId) throws ConnectorPluginException
	{
		GenoaInventory inventory = this.playerInventories.getOrDefault(playerId, null);
		if (inventory == null)
		{
			throw new ConnectorPluginException("No inventory found for player %s".formatted(playerId));
		}
		return inventory;
	}
}