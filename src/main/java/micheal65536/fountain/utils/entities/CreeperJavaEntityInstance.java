package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import org.jetbrains.annotations.NotNull;

public class CreeperJavaEntityInstance<T extends CreeperBedrockEntityInstance> extends MobJavaEntityInstance<T>
{
	public CreeperJavaEntityInstance(@NotNull String bedrockEntityIdentifier, @NotNull T bedrockEntityInstance)
	{
		super(bedrockEntityIdentifier, bedrockEntityInstance);
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		super.onMetadataFieldChanged(metadata);
		getMetadataField(metadata, 16, MetadataType.INT, value ->
		{
			this.bedrockEntityInstance.ignited = value == 1;
		});
		getMetadataField(metadata, 17, MetadataType.BOOLEAN, value ->
		{
			this.bedrockEntityInstance.charged = value;
		});
		getMetadataField(metadata, 18, MetadataType.BOOLEAN, value ->
		{
			this.bedrockEntityInstance.ignitedByFlintAndSteel = value;
		});
	}
}