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
	public DisconnectResponse onPlayerDisconnected() throws ConnectorPlugin.ConnectorPluginException
	{
		return this.connectorPlugin.onPlayerDisconnected(this.playerId);
	}

	public boolean onPlayerDead() throws ConnectorPlugin.ConnectorPluginException
	{
		return this.connectorPlugin.onPlayerDead(this.playerId);
	}

	@NotNull
	public Inventory onPlayerGetInventory() throws ConnectorPlugin.ConnectorPluginException
	{
		return this.connectorPlugin.onPlayerGetInventory(this.playerId);
	}

	public void onPlayerInventoryAddItem(@NotNull String itemId, int count) throws ConnectorPlugin.ConnectorPluginException
	{
		this.connectorPlugin.onPlayerInventoryAddItem(this.playerId, itemId, count);
	}

	public void onPlayerInventoryAddItem(@NotNull String itemId, @NotNull String instanceId, int wear) throws ConnectorPlugin.ConnectorPluginException
	{
		this.connectorPlugin.onPlayerInventoryAddItem(this.playerId, itemId, instanceId, wear);
	}

	public int onPlayerInventoryRemoveItem(@NotNull String itemId, int count) throws ConnectorPlugin.ConnectorPluginException
	{
		return this.connectorPlugin.onPlayerInventoryRemoveItem(this.playerId, itemId, count);
	}

	public boolean onPlayerInventoryRemoveItem(@NotNull String itemId, @NotNull String instanceId) throws ConnectorPlugin.ConnectorPluginException
	{
		return this.connectorPlugin.onPlayerInventoryRemoveItem(this.playerId, itemId, instanceId);
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