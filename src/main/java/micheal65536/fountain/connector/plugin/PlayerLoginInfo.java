package micheal65536.fountain.connector.plugin;

import org.jetbrains.annotations.NotNull;

public final class PlayerLoginInfo
{
	@NotNull
	public final String uuid;
	// TODO: should have join code here as well

	public PlayerLoginInfo(@NotNull String uuid)
	{
		this.uuid = uuid;
	}
}