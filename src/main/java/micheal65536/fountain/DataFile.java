package micheal65536.fountain;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.io.IOException;
import java.util.function.Consumer;

public final class DataFile
{
	public static void load(@NotNull String filename, @NotNull Consumer<JsonElement> consumer)
	{
		try (FileReader fileReader = new FileReader("data/" + filename))
		{
			JsonElement root = JsonParser.parseReader(fileReader);
			consumer.accept(root);
		}
		catch (IOException | JsonParseException | UnsupportedOperationException | NullPointerException exception)
		{
			LogManager.getLogger().fatal("Cannot read data file %s".formatted(filename), exception);
			System.exit(1);
		}
	}
}