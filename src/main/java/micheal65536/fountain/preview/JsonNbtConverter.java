package micheal65536.fountain.preview;

import com.google.gson.annotations.SerializedName;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class JsonNbtConverter
{
	@NotNull
	public static JsonNbtTag<?> convert(@NotNull NbtMap tag)
	{
		HashMap<String, JsonNbtTag<?>> value = new HashMap<>();
		for (Map.Entry<String, Object> entry : tag.entrySet())
		{
			value.put(entry.getKey(), convert(entry.getValue()));
		}
		return new CompoundJsonNbtTag(value);
	}

	@NotNull
	public static JsonNbtTag<?> convert(@NotNull NbtList<?> tag)
	{
		LinkedList<JsonNbtTag<?>> value = new LinkedList<>();
		for (Object item : tag)
		{
			value.add(convert(item));
		}
		return new ListJsonNbtTag(value.toArray(JsonNbtTag<?>[]::new));
	}

	@NotNull
	private static JsonNbtTag<?> convert(@NotNull Object tag) throws UnsupportedOperationException
	{
		if (tag instanceof NbtMap)
		{
			return convert((NbtMap) tag);
		}
		else if (tag instanceof NbtList<?>)
		{
			return convert((NbtList<?>) tag);
		}
		else if (tag instanceof Integer)
		{
			return new IntJsonNbtTag((int) tag);
		}
		else if (tag instanceof Byte)
		{
			return new ByteJsonNbtTag((byte) tag);
		}
		else if (tag instanceof Float)
		{
			return new FloatJsonNbtTag((float) tag);
		}
		else if (tag instanceof String)
		{
			return new StringJsonNbtTag((String) tag);
		}
		else
		{
			throw new UnsupportedOperationException("Cannot convert tag of type %s".formatted(tag.getClass().getSimpleName()));
		}
	}

	public static abstract class JsonNbtTag<T>
	{
		public enum Type
		{
			@SerializedName("compound") COMPOUND,
			@SerializedName("list") LIST,
			@SerializedName("int") INT,
			@SerializedName("byte") BYTE,
			@SerializedName("float") FLOAT,
			@SerializedName("string") STRING
		}

		@NotNull
		public final Type type;
		@NotNull
		public final T value;

		private JsonNbtTag(@NotNull Type type, @NotNull T value)
		{
			this.type = type;
			this.value = value;
		}
	}

	public static final class CompoundJsonNbtTag extends JsonNbtTag<HashMap<String, JsonNbtTag<?>>>
	{
		private CompoundJsonNbtTag(@NotNull HashMap<String, JsonNbtTag<?>> value)
		{
			super(Type.COMPOUND, value);
		}
	}

	public static final class ListJsonNbtTag extends JsonNbtTag<JsonNbtTag<?>[]>
	{
		public ListJsonNbtTag(@NotNull JsonNbtTag<?>[] value)
		{
			super(Type.LIST, value);
		}
	}

	public static final class IntJsonNbtTag extends JsonNbtTag<Integer>
	{
		public IntJsonNbtTag(int value)
		{
			super(Type.INT, value);
		}
	}

	public static final class ByteJsonNbtTag extends JsonNbtTag<Byte>
	{
		public ByteJsonNbtTag(byte value)
		{
			super(Type.BYTE, value);
		}
	}

	public static final class FloatJsonNbtTag extends JsonNbtTag<Float>
	{
		public FloatJsonNbtTag(float value)
		{
			super(Type.FLOAT, value);
		}
	}

	public static final class StringJsonNbtTag extends JsonNbtTag<String>
	{
		public StringJsonNbtTag(@NotNull String value)
		{
			super(Type.STRING, value);
		}
	}
}