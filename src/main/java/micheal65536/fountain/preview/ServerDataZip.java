package micheal65536.fountain.preview;

import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class ServerDataZip
{
	@NotNull
	public static ServerDataZip read(@NotNull InputStream inputStream) throws IOException
	{
		return new ServerDataZip(inputStream);
	}

	private final HashMap<String, byte[]> files = new HashMap<>();

	private ServerDataZip(@NotNull InputStream inputStream) throws IOException
	{
		try (ZipInputStream zipInputStream = new ZipInputStream(inputStream))
		{
			for (ZipEntry zipEntry = zipInputStream.getNextEntry(); zipEntry != null; zipEntry = zipInputStream.getNextEntry())
			{
				if (zipEntry.isDirectory())
				{
					zipInputStream.closeEntry();
					continue;
				}

				byte[] data = zipInputStream.readAllBytes();
				this.files.put(zipEntry.getName(), data);
				zipInputStream.closeEntry();
			}
		}
	}

	@NotNull
	public CompoundTag getChunkNBT(int x, int z) throws IOException
	{
		int regionX = x >> 5;
		int regionZ = z >> 5;
		int chunkX = x & 31;
		int chunkZ = z & 31;
		int chunkIndex = (chunkZ << 5) | chunkX;

		ByteBuf byteBuf = Unpooled.wrappedBuffer(this.files.get("region/r.%d.%d.mca".formatted(regionX, regionZ)));

		byteBuf.skipBytes(chunkIndex * 4);
		int offset = (int) (byteBuf.readUnsignedInt() >> 8);

		byteBuf.resetReaderIndex();
		byteBuf.skipBytes(offset * 4096);

		int length = (int) byteBuf.readUnsignedInt();
		byte compressionType = byteBuf.readByte();
		byte[] compressed = new byte[length - 1];
		byteBuf.readBytes(compressed);
		byte[] uncompressed = switch (compressionType)
		{
			case 1 ->
			{
				GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressed));
				yield gzipInputStream.readAllBytes();
			}
			case 2 ->
			{
				InflaterInputStream inflaterInputStream = new InflaterInputStream(new ByteArrayInputStream(compressed));
				yield inflaterInputStream.readAllBytes();
			}
			case 3 ->
			{
				yield compressed;
			}
			default ->
			{
				throw new IOException("Invalid compression type %d".formatted(compressionType));
			}
		};

		Tag tag = NBTIO.readTag(new ByteArrayInputStream(uncompressed));
		if (tag == null || !(tag instanceof CompoundTag))
		{
			throw new IOException("Could not decode NBT data");
		}
		return (CompoundTag) tag;
	}
}