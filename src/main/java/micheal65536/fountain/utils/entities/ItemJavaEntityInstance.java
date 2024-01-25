package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.utils.EntityManager;
import micheal65536.fountain.utils.ItemTranslator;

public final class ItemJavaEntityInstance extends EntityManager.JavaEntityInstance
{
	private boolean burning = false;
	private boolean gravity = true;

	private ItemData item = null;

	private BaseBedrockEntityInstance bedrockEntityInstance = null;

	@Override
	protected void onRemoved()
	{
		if (this.bedrockEntityInstance != null)
		{
			this.bedrockEntityInstance.remove();
			this.bedrockEntityInstance = null;
		}
	}

	@Override
	protected void onPositionChanged()
	{
		if (this.bedrockEntityInstance != null)
		{
			this.bedrockEntityInstance.setPos(this.getPos());
			this.bedrockEntityInstance.setOnGround(this.getOnGround());
			this.bedrockEntityInstance.setYaw(this.getYaw());
			this.bedrockEntityInstance.setPitch(this.getPitch());
			this.bedrockEntityInstance.sendPosition();
		}
	}

	@Override
	protected void onVelocityChanged()
	{
		if (this.bedrockEntityInstance != null)
		{
			this.bedrockEntityInstance.setVelocity(this.getVelocity());
			this.bedrockEntityInstance.sendVelocity();
		}
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ?> metadata)
	{
		getMetadataField(metadata, 0, MetadataType.BYTE, value ->
		{
			this.burning = (value & 0x01) != 0;
			if (this.bedrockEntityInstance != null)
			{
				this.bedrockEntityInstance.burning = this.burning;
			}
		});
		getMetadataField(metadata, 5, MetadataType.BOOLEAN, value ->
		{
			this.gravity = value;
			if (this.bedrockEntityInstance != null)
			{
				this.bedrockEntityInstance.gravity = this.gravity;
			}
		});
		if (this.bedrockEntityInstance != null)
		{
			this.bedrockEntityInstance.sendData();
		}

		getMetadataField(metadata, 8, MetadataType.ITEM, value ->
		{
			ItemData oldItem = this.item;
			this.item = ItemTranslator.translateJavaToBedrock(value);
			if (oldItem == null || !oldItem.equals(this.item, false, true, true))
			{
				if (this.bedrockEntityInstance != null)
				{
					this.bedrockEntityInstance.remove();
				}

				this.bedrockEntityInstance = new BaseBedrockEntityInstance();
				this.bedrockEntityInstance.setPos(this.getPos());
				this.bedrockEntityInstance.setVelocity(this.getVelocity());
				this.bedrockEntityInstance.burning = this.burning;
				this.bedrockEntityInstance.gravity = this.gravity;
				this.addBedrockItemEntity(this.bedrockEntityInstance, this.item);
			}
			else
			{
				this.bedrockEntityInstance.sendEvent(EntityEventType.UPDATE_ITEM_STACK_SIZE, this.item.getCount());
			}
		});
	}
}