package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import org.jetbrains.annotations.NotNull;

public class ZombieJavaEntityInstance<T extends ZombieBedrockEntityInstance> extends AgeableJavaEntityInstance<T>
{
	public ZombieJavaEntityInstance(@NotNull String bedrockEntityIdentifier, @NotNull T bedrockEntityInstance)
	{
		super(bedrockEntityIdentifier, bedrockEntityInstance);
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		super.onMetadataFieldChanged(metadata);
		getMetadataField(metadata, 18, MetadataType.BOOLEAN, value ->
		{
			this.bedrockEntityInstance.convertingToDrowned = value;
		});
	}
}