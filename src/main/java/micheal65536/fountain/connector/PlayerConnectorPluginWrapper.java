package micheal65536.fountain.connector;

import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.connector.plugin.ConnectorPlugin;
import micheal65536.fountain.connector.plugin.DisconnectResponse;
import micheal65536.fountain.connector.plugin.Inventory;

public final class PlayerConnectorPluginWrapper
{
	private final ConnectorPlugin connectorPlugin;
	private final String uuid;

	public PlayerConnectorPluginWrapper(@NotNull ConnectorPlugin connectorPlugin, @NotNull String uuid)
	{
		this.connectorPlugin = connectorPlugin;
		this.uuid = uuid;
	}

	@NotNull
	public DisconnectResponse playerDisconnected(@NotNull Inventory inventory) throws ConnectorPlugin.ConnectorPluginException
	{
		return this.connectorPlugin.playerDisconnected(this.uuid, inventory);
	}
}