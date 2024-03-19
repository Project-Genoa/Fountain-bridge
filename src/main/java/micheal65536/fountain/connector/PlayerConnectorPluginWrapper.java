package micheal65536.fountain.connector;

import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.connector.plugin.ConnectorPlugin;
import micheal65536.fountain.connector.plugin.DisconnectResponse;
import micheal65536.fountain.connector.plugin.Inventory;

public final class PlayerConnectorPluginWrapper
{
	private final ConnectorPlugin connectorPlugin;
	private final String playerId;

	public PlayerConnectorPluginWrapper(@NotNull ConnectorPlugin connectorPlugin, @NotNull String playerId)
	{
		this.connectorPlugin = connectorPlugin;
		this.playerId = playerId;
	}

	@NotNull
	public DisconnectResponse onPlayerDisconnected(@NotNull Inventory inventory) throws ConnectorPlugin.ConnectorPluginException
	{
		return this.connectorPlugin.onPlayerDisconnected(this.playerId, inventory);
	}

	public void onPlayerInventoryAddItem(@NotNull String itemId, int count) throws ConnectorPlugin.ConnectorPluginException
	{
		this.connectorPlugin.onPlayerInventoryAddItem(this.playerId, itemId, count);
	}

	public void onPlayerInventoryAddItem(@NotNull String itemId, @NotNull String instanceId, int wear) throws ConnectorPlugin.ConnectorPluginException
	{
		this.connectorPlugin.onPlayerInventoryAddItem(this.playerId, itemId, instanceId, wear);
	}

	public void onPlayerInventoryRemoveItem(@NotNull String itemId, int count) throws ConnectorPlugin.ConnectorPluginException
	{
		this.connectorPlugin.onPlayerInventoryRemoveItem(this.playerId, itemId, count);
	}

	public void onPlayerInventoryRemoveItem(@NotNull String itemId, @NotNull String instanceId) throws ConnectorPlugin.ConnectorPluginException
	{
		this.connectorPlugin.onPlayerInventoryRemoveItem(this.playerId, itemId, instanceId);
	}

	public void onPlayerInventoryUpdateItemWear(@NotNull String itemId, @NotNull String instanceId, int wear) throws ConnectorPlugin.ConnectorPluginException
	{
		this.connectorPlugin.onPlayerInventoryUpdateItemWear(this.playerId, itemId, instanceId, wear);
	}

	public void onPlayerInventorySetHotbar(Inventory.HotbarItem[] hotbar) throws ConnectorPlugin.ConnectorPluginException
	{
		this.connectorPlugin.onPlayerInventorySetHotbar(this.playerId, hotbar);
	}
}