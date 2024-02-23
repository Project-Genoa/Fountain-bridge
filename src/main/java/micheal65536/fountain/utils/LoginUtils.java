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

		for (String token : loginPacket.getChain())
		{
			String[] parts = token.split("\\.");
			if (parts.length < 2)
			{
				continue;
			}
			String dataBase64 = parts[1];

			String data;
			try
			{
				data = new String(Base64.getUrlDecoder().decode(dataBase64), StandardCharsets.UTF_8);
			}
			catch (IllegalArgumentException exception)
			{
				continue;
			}

			try
			{
				JsonElement root = JsonParser.parseReader(new StringReader(data));

				if (root.getAsJsonObject().has("extraData"))
				{
					if (uuid == null && root.getAsJsonObject().get("extraData").getAsJsonObject().has("XUID"))
					{
						uuid = root.getAsJsonObject().get("extraData").getAsJsonObject().get("XUID").getAsString().toLowerCase(Locale.ROOT);
					}

					if (username == null && root.getAsJsonObject().get("extraData").getAsJsonObject().has("displayName"))
					{
						username = root.getAsJsonObject().get("extraData").getAsJsonObject().get("displayName").getAsString();
					}
				}
			}
			catch (JsonParseException | UnsupportedOperationException | IllegalStateException | NullPointerException exception)
			{
				continue;
			}
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