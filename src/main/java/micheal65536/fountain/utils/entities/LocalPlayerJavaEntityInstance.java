package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.utils.EntityManager;

import java.util.LinkedList;

public final class LocalPlayerJavaEntityInstance extends EntityManager.JavaEntityInstance
{
	private float health = 20.0f;
	private float maxHealth = 20.0f;

	public final EntityManager.BedrockEntityInstance bedrockEntityInstance = new LocalPlayerBedrockEntityInstance();

	private boolean dead = false;
	private final PlayerDeadCallback playerDeadCallback;

	public LocalPlayerJavaEntityInstance(@NotNull LocalPlayerJavaEntityInstance.PlayerDeadCallback playerDeadCallback)
	{
		this.playerDeadCallback = playerDeadCallback;
	}

	public float getHealth()
	{
		return this.health;
	}

	public float getMaxHealth()
	{
		return this.maxHealth;
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		super.onMetadataFieldChanged(metadata);
		getMetadataField(metadata, 9, MetadataType.FLOAT, value ->
		{
			this.health = value;
		});
	}

	@Override
	protected void onAttributeChanged(@NotNull Attribute attribute)
	{
		super.onAttributeChanged(attribute);
		if (attribute.getType() instanceof AttributeType.Builtin)
		{
			switch ((AttributeType.Builtin) attribute.getType())
			{
				case GENERIC_MAX_HEALTH ->
				{
					this.maxHealth = (float) getAttributeValueWithModifiers(attribute);
				}
			}
		}
	}

	@Override
	protected void afterMetadataBatchChange()
	{
		this.bedrockEntityInstance.sendData();

		if (this.health == 0.0f)
		{
			if (!this.dead)
			{
				this.dead = true;
				this.playerDeadCallback.onPlayerDead();
			}
		}
		else
		{
			this.dead = false;
		}
	}

	@Override
	protected void afterAttributeBatchChange()
	{
		this.bedrockEntityInstance.sendData();
	}

	private final class LocalPlayerBedrockEntityInstance extends EntityManager.BedrockEntityInstance
	{
		@Override
		protected void getEntityAttributes(@NotNull LinkedList<AttributeData> entityAttributes)
		{
			super.getEntityAttributes(entityAttributes);
			entityAttributes.add(new AttributeData("minecraft:health", 0.0f, LocalPlayerJavaEntityInstance.this.maxHealth, LocalPlayerJavaEntityInstance.this.health > 0.0f && LocalPlayerJavaEntityInstance.this.health < 1.0f ? 1.0f : LocalPlayerJavaEntityInstance.this.health));
		}
	}

	public interface PlayerDeadCallback
	{
		void onPlayerDead();
	}
}