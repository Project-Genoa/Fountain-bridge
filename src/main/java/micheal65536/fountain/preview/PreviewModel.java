package micheal65536.fountain.preview;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

record PreviewModel(
		int format_version, // always 1
		boolean isNight,
		@NotNull SubChunk[] sub_chunks,
		BlockEntity[] blockEntities,
		Entity[] entities
)
{
	public record Position(
			int x,
			int y,
			int z
	)
	{
	}

	public record SubChunk(
			@NotNull Position position,
			@NotNull PaletteEntry[] block_palette,
			int[] blocks
	)
	{
		public record PaletteEntry(
				@NotNull String name,
				int data
		)
		{
		}
	}

	public record BlockEntity(
			int type,
			@NotNull Position position,
			@NotNull JsonNbtConverter.JsonNbtTag<?> data
	)
	{
	}

	public record Entity(
			@NotNull String name,
			@NotNull Position position,
			@NotNull Rotation rotation,
			@NotNull Position shadowPosition,
			float shadowSize,
			int overlayColor,
			int changeColor,
			int multiplicitiveTintChangeColor,
			@Nullable HashMap<String, Object> extraData,
			@NotNull String skinData,
			boolean isPersonaSkin
	)
	{
		public record Position(
				float x,
				float y,
				float z
		)
		{
		}

		public record Rotation(
				float x,
				float y
		)
		{
		}
	}
}