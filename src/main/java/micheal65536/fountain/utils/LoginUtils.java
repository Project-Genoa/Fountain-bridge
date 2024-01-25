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

public class LoginUtils
{
	@Nullable
	public static String getUsername(@NotNull LoginPacket loginPacket)
	{
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
				String displayName = root.getAsJsonObject().get("extraData").getAsJsonObject().get("displayName").getAsString();
				if (displayName != null)
				{
					return displayName;
				}
			}
			catch (JsonParseException | UnsupportedOperationException | IllegalStateException | NullPointerException exception)
			{
				continue;
			}
		}
		return null;
	}
}