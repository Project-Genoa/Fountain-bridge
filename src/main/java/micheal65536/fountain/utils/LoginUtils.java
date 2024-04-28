package micheal65536.fountain.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public class LoginUtils
{
	@Nullable
	public static LoginInfo getLoginInfo(@NotNull LoginPacket loginPacket)
	{
		String uuid = null;
		String username = null;

		String extra = loginPacket.getExtra();

		String[] parts = extra.split("\\.");
		if (parts.length < 2)
		{
			return null;
		}
		String dataBase64 = parts[1];

		String data;
		try
		{
			data = new String(Base64.getUrlDecoder().decode(dataBase64), StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException exception)
		{
			return null;
		}

		try
		{
			JsonElement root = JsonParser.parseReader(new StringReader(data));

			uuid = root.getAsJsonObject().get("PlatformOnlineId").getAsString().toLowerCase(Locale.ROOT);
			username = root.getAsJsonObject().get("ThirdPartyName").getAsString();
		}
		catch (JsonParseException | UnsupportedOperationException | IllegalStateException | NullPointerException exception)
		{
			return null;
		}

		if (uuid == null || username == null)
		{
			return null;
		}

		return new LoginInfo(uuid, username);
	}

	public static final class LoginInfo
	{
		@NotNull
		public final String uuid;
		@NotNull
		public final String username;

		private LoginInfo(@NotNull String uuid, @NotNull String username)
		{
			this.uuid = uuid;
			this.username = username;
		}
	}
}