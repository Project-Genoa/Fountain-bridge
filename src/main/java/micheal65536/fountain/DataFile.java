package micheal65536.fountain;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public final class DataFile
{
	public static void load(@NotNull String name, @NotNull Consumer<JsonElement> consumer)
	{
		try (InputStream inputStream = DataFile.class.getClassLoader().getResourceAsStream(name); InputStreamReader inputStreamReader = new InputStreamReader(inputStream))
		{
			JsonElement root = JsonParser.parseReader(inputStreamReader);
			consumer.accept(root);
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot read resource %s".formatted(name), exception);
			System.exit(1);
		}
	}
}