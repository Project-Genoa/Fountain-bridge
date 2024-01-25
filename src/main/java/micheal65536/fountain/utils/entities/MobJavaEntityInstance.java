package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import org.jetbrains.annotations.NotNull;

public class MobJavaEntityInstance<T extends MobBedrockEntityInstance> extends LivingJavaEntityInstance<T>
{
	public MobJavaEntityInstance(@NotNull String bedrockEntityIdentifier, @NotNull T bedrockEntityInstance)
	{
		super(bedrockEntityIdentifier, bedrockEntityInstance);
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		super.onMetadataFieldChanged(metadata);
		getMetadataField(metadata, 15, MetadataType.BYTE, value ->
		{
			this.bedrockEntityInstance.ai = !((value & 0x01) != 0);
		});
	}
}