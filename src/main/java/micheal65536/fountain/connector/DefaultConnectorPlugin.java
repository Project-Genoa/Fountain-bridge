package micheal65536.fountain.connector;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.connector.plugin.ConnectorPlugin;
import micheal65536.fountain.connector.plugin.DisconnectResponse;
import micheal65536.fountain.connector.plugin.Inventory;
import micheal65536.fountain.connector.plugin.PlayerLoginInfo;

public final class DefaultConnectorPlugin implements ConnectorPlugin
{
	@Override
	public void init(@NotNull String arg, @NotNull Logger logger) throws ConnectorPluginException
	{
		// empty
	}

	@Override
	@Nullable
	public Inventory playerConnected(@NotNull PlayerLoginInfo playerLoginInfo) throws ConnectorPluginException
	{
		return new Inventory(new Inventory.StackableItem[0], new Inventory.NonStackableItem[0], new Inventory.HotbarItem[7]);
	}

	@Override
	@NotNull
	public DisconnectResponse playerDisconnected(@NotNull String uuid, @NotNull Inventory inventory) throws ConnectorPluginException
	{
		return new DisconnectResponse();
	}
}