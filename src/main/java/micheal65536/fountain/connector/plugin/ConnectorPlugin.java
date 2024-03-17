package micheal65536.fountain.connector.plugin;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConnectorPlugin
{
	void init(@NotNull String arg, @NotNull Logger logger) throws ConnectorPluginException;

	@Nullable
	Inventory playerConnected(@NotNull PlayerLoginInfo playerLoginInfo) throws ConnectorPluginException;

	@NotNull
	DisconnectResponse playerDisconnected(@NotNull String uuid, @NotNull Inventory inventory) throws ConnectorPluginException;

	public static final class ConnectorPluginException extends Exception
	{
		public ConnectorPluginException()
		{
			super();
		}

		public ConnectorPluginException(String message)
		{
			super(message);
		}

		public ConnectorPluginException(String message, Throwable cause)
		{
			super(message, cause);
		}

		public ConnectorPluginException(Throwable cause)
		{
			super(cause);
		}
	}
}