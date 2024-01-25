package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import org.jetbrains.annotations.NotNull;

public class SpiderJavaEntityInstance<T extends SpiderBedrockEntityInstance> extends MobJavaEntityInstance<T>
{
	public SpiderJavaEntityInstance(@NotNull String bedrockEntityIdentifier, @NotNull T bedrockEntityInstance)
	{
		super(bedrockEntityIdentifier, bedrockEntityInstance);
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		super.onMetadataFieldChanged(metadata);
		getMetadataField(metadata, 16, MetadataType.BYTE, value ->
		{
			this.bedrockEntityInstance.climbing = (value & 0x01) != 0;
		});
	}
}