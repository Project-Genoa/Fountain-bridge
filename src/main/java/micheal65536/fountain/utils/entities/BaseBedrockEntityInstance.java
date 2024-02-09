package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.utils.EntityManager;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class BaseBedrockEntityInstance extends EntityManager.BedrockEntityInstance
{
	public boolean burning = false;
	public int air = 300;
	public int maxAir = 300;
	public boolean gravity = true;

	public boolean sleeping = false;
	public boolean swimming = false;
	public boolean gliding = false;

	@Nullable
	public String nametag = null;
	public boolean nametagVisible = false;

	private Consumer<EntityManager.BedrockEntityInstance> attackCallback;
	private Consumer<EntityManager.BedrockEntityInstance> interactCallback;

	public BaseBedrockEntityInstance()
	{
		this.attackCallback = null;
		this.interactCallback = null;
	}

	public void setAttackCallback(@Nullable Consumer<EntityManager.BedrockEntityInstance> attackCallback)
	{
		this.attackCallback = attackCallback;
	}

	public void setInteractCallback(@Nullable Consumer<EntityManager.BedrockEntityInstance> interactCallback)
	{
		this.interactCallback = interactCallback;
	}

	@Override
	public void onAttack()
	{
		if (this.attackCallback != null)
		{
			this.attackCallback.accept(this);
		}
	}

	@Override
	public void onInteract()
	{
		if (this.interactCallback != null)
		{
			this.interactCallback.accept(this);
		}
	}

	@Override
	protected void getEntityData(@NotNull Map<EntityDataType<?>, ?> entityData)
	{
		super.getEntityData(entityData);
		putEntityData(entityData, EntityDataTypes.AIR_SUPPLY, (short) this.air);
		putEntityData(entityData, EntityDataTypes.AIR_SUPPLY_MAX, (short) this.maxAir);
		putEntityData(entityData, EntityDataTypes.NAME, this.nametag != null ? this.nametag : "");
		putEntityData(entityData, EntityDataTypes.NAMETAG_ALWAYS_SHOW, this.nametagVisible ? (byte) 1 : (byte) 0);
	}

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		entityFlags.add(EntityFlag.HAS_COLLISION);
		entityFlags.add(EntityFlag.SILENT);
		if (this.burning)
		{
			entityFlags.add(EntityFlag.ON_FIRE);
		}
		if (this.gravity)
		{
			entityFlags.add(EntityFlag.HAS_GRAVITY);
		}
		if (this.sleeping)
		{
			entityFlags.add(EntityFlag.SLEEPING);
		}
		if (this.swimming)
		{
			entityFlags.add(EntityFlag.SWIMMING);
		}
		if (this.gliding)
		{
			entityFlags.add(EntityFlag.GLIDING);
		}
		if (this.nametagVisible)
		{
			entityFlags.add(EntityFlag.ALWAYS_SHOW_NAME);
		}
	}
}