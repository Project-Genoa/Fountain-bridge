package micheal65536.fountain;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.data.game.level.event.LevelEventType;
import com.github.steveice10.mc.protocol.data.game.level.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

public class ClientboundAddEntityCustomPacket extends ClientboundAddEntityPacket
{
	public static ClientboundAddEntityCustomPacket read(ByteBuf in, MinecraftCodecHelper helper) throws IOException
	{
		ByteBuf byteBuf = in.duplicate();
		helper.readVarInt(byteBuf);
		helper.readUUID(byteBuf);
		int typeId = helper.readVarInt(byteBuf);
		if (typeId >= EntityType.values().length)
		{
			Int2ObjectMap<LevelEventType> levelEvents;
			Map<String, BuiltinSound> soundNames;
			try
			{
				Field levelEventsField = MinecraftCodecHelper.class.getDeclaredField("levelEvents");
				Field soundNamesField = MinecraftCodecHelper.class.getDeclaredField("soundNames");
				levelEventsField.setAccessible(true);
				soundNamesField.setAccessible(true);
				levelEvents = (Int2ObjectMap<LevelEventType>) levelEventsField.get(helper);
				soundNames = (Map<String, BuiltinSound>) soundNamesField.get(helper);
			}
			catch (Exception exception)
			{
				throw new AssertionError(exception);
			}
			return new ClientboundAddEntityCustomPacket(typeId, false, in, new MinecraftCodecHelper(levelEvents, soundNames)
			{
				private int varIntCounter = 0;

				@Override
				public int readVarInt(ByteBuf buf)
				{
					if (this.varIntCounter++ == 1)
					{
						super.readVarInt(buf);
						return 0;
					}
					else
					{
						return super.readVarInt(buf);
					}
				}
			});
		}
		else
		{
			return new ClientboundAddEntityCustomPacket(typeId, true, in, helper);
		}
	}

	public final int typeId;
	private final boolean hasVanillaType;

	private ClientboundAddEntityCustomPacket(int typeId, boolean hasVanillaType, ByteBuf in, MinecraftCodecHelper helper) throws IOException
	{
		super(in, helper);
		this.typeId = typeId;
		this.hasVanillaType = hasVanillaType;
	}

	@Override
	@Nullable
	public EntityType getType()
	{
		return this.hasVanillaType ? super.getType() : null;
	}
}