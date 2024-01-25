package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeModifier;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.ModifierOperation;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.player.Animation;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.AddItemEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.PlayerSession;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class EntityManager
{
	private final PlayerSession playerSession;

	private final HashMap<Integer, JavaEntityInstance> javaEntities = new HashMap<>();
	private final HashMap<Long, BedrockEntityInstance> bedrockEntities = new HashMap<>();

	public EntityManager(@NotNull PlayerSession playerSession)
	{
		this.playerSession = playerSession;
	}

	public void registerJavaEntity(int instanceId, @NotNull JavaEntityInstance entityInstance)
	{
		if (this.javaEntities.containsKey(instanceId))
		{
			LogManager.getLogger().warn("Duplicate Java entity instance ID " + instanceId);
			return;
		}
		if (entityInstance.entityManager != null)
		{
			throw new IllegalArgumentException("Java entity instance has already been added");
		}

		entityInstance.entityManager = this;
		entityInstance.instanceId = instanceId;
		this.javaEntities.put(instanceId, entityInstance);
		entityInstance.onAdded();
	}

	public long registerLocalPlayerEntity(int javaEntityInstanceId)
	{
		if (this.javaEntities.containsKey(javaEntityInstanceId))
		{
			LogManager.getLogger().warn("Duplicate Java entity instance ID " + javaEntityInstanceId);
		}

		long bedrockEntityInstanceId = this.getNewBedrockEntityId();
		this.javaEntities.put(javaEntityInstanceId, null);
		this.bedrockEntities.put(bedrockEntityInstanceId, null);
		return bedrockEntityInstanceId;
	}

	public void addBedrockEntity(@NotNull String identifier, @NotNull BedrockEntityInstance entityInstance)
	{
		if (entityInstance.entityManager != null)
		{
			throw new IllegalArgumentException("Bedrock entity instance has already been added");
		}

		entityInstance.instanceId = this.getNewBedrockEntityId();
		AddEntityPacket addEntityPacket = new AddEntityPacket();
		addEntityPacket.setIdentifier(identifier);
		addEntityPacket.setUniqueEntityId(entityInstance.instanceId);
		addEntityPacket.setRuntimeEntityId(entityInstance.instanceId);
		addEntityPacket.setPosition(entityInstance.getPos());
		addEntityPacket.setMotion(entityInstance.getVelocity());
		addEntityPacket.setRotation(Vector2f.from(entityInstance.getPitch(), entityInstance.getYaw()));
		addEntityPacket.setHeadRotation(entityInstance.getHeadYaw());
		addEntityPacket.setBodyRotation(entityInstance.getYaw());
		HashMap<EntityDataType<?>, ?> bedrockEntityData = new HashMap<>();
		entityInstance.getEntityData(bedrockEntityData);
		addEntityPacket.getMetadata().putAll(bedrockEntityData);
		EnumSet<EntityFlag> bedrockEntityFlags = EnumSet.noneOf(EntityFlag.class);
		entityInstance.getEntityFlags(bedrockEntityFlags);
		addEntityPacket.getMetadata().putFlags(bedrockEntityFlags);
		LinkedList<AttributeData> bedrockAttributes = new LinkedList<>();
		entityInstance.getEntityAttributes(bedrockAttributes);
		addEntityPacket.getAttributes().addAll(bedrockAttributes);
		this.playerSession.sendBedrockPacket(addEntityPacket);
		LogManager.getLogger().trace("Added Bedrock entity with ID " + entityInstance.instanceId);

		entityInstance.entityManager = this;
		this.bedrockEntities.put(entityInstance.instanceId, entityInstance);
	}

	public void addBedrockItemEntity(@NotNull BedrockEntityInstance entityInstance, @NotNull ItemData item)
	{
		if (entityInstance.entityManager != null)
		{
			throw new IllegalArgumentException("Bedrock entity instance has already been added");
		}

		entityInstance.instanceId = this.getNewBedrockEntityId();
		AddItemEntityPacket addItemEntityPacket = new AddItemEntityPacket();
		addItemEntityPacket.setUniqueEntityId(entityInstance.instanceId);
		addItemEntityPacket.setRuntimeEntityId(entityInstance.instanceId);
		addItemEntityPacket.setPosition(entityInstance.getPos());
		addItemEntityPacket.setMotion(entityInstance.getVelocity());
		addItemEntityPacket.setItemInHand(item);
		addItemEntityPacket.setFromFishing(false);
		HashMap<EntityDataType<?>, ?> bedrockEntityData = new HashMap<>();
		entityInstance.getEntityData(bedrockEntityData);
		addItemEntityPacket.getMetadata().putAll(bedrockEntityData);
		EnumSet<EntityFlag> bedrockEntityFlags = EnumSet.noneOf(EntityFlag.class);
		entityInstance.getEntityFlags(bedrockEntityFlags);
		addItemEntityPacket.getMetadata().putFlags(bedrockEntityFlags);
		this.playerSession.sendBedrockPacket(addItemEntityPacket);
		LogManager.getLogger().trace("Added Bedrock item entity with ID " + entityInstance.instanceId);

		entityInstance.entityManager = this;
		this.bedrockEntities.put(entityInstance.instanceId, entityInstance);
	}

	@Nullable
	public JavaEntityInstance getJavaEntity(int instanceId)
	{
		return this.javaEntities.getOrDefault(instanceId, null);
	}

	@Nullable
	public BedrockEntityInstance getBedrockEntity(long instanceId)
	{
		return this.bedrockEntities.getOrDefault(instanceId, null);
	}

	private void removeJavaEntity(@NotNull JavaEntityInstance entityInstance)
	{
		if (this.javaEntities.remove(entityInstance.instanceId) != entityInstance)
		{
			throw new AssertionError();
		}

		entityInstance.onRemoved();
		entityInstance.entityManager = null;
	}

	private void removeBedrockEntity(@NotNull BedrockEntityInstance entityInstance)
	{
		if (this.bedrockEntities.remove(entityInstance.instanceId) != entityInstance)
		{
			throw new AssertionError();
		}

		entityInstance.entityManager = null;

		RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
		removeEntityPacket.setUniqueEntityId(entityInstance.instanceId);
		this.playerSession.sendBedrockPacket(removeEntityPacket);
		LogManager.getLogger().trace("Removed Bedrock entity with ID " + entityInstance.instanceId);
	}

	private int nextBedrockEntityId = 1;

	private int getNewBedrockEntityId()
	{
		return this.nextBedrockEntityId++;
	}

	public static class JavaEntityInstance
	{
		private EntityManager entityManager = null;
		private int instanceId;

		private Vector3f pos = Vector3f.ZERO;
		private Vector3f velocity = Vector3f.ZERO;
		private boolean onGround;
		private float yaw;
		private float pitch;
		private float headYaw;

		private ItemData handMain = ItemData.AIR;
		private ItemData handSecondary = ItemData.AIR;

		private ItemData armorHead = ItemData.AIR;
		private ItemData armorChest = ItemData.AIR;
		private ItemData armorLegs = ItemData.AIR;
		private ItemData armorFeet = ItemData.AIR;

		public JavaEntityInstance()
		{
			// empty
		}

		public final int getInstanceId()
		{
			this.requireEntityManager();
			return this.instanceId;
		}

		public final void remove()
		{
			this.requireEntityManager().removeJavaEntity(this);
		}

		public final void setPosition(@NotNull Vector3f pos, boolean onGround, float yaw, float pitch)
		{
			this.pos = pos;
			this.onGround = onGround;
			this.yaw = yaw;
			this.pitch = pitch;
			this.onPositionChanged();
		}

		public final void setVelocity(@NotNull Vector3f velocity)
		{
			this.velocity = velocity;
			this.onVelocityChanged();
		}

		public final void setHeadYaw(float headYaw)
		{
			this.headYaw = headYaw;
			this.onHeadYawChanged();
		}

		public final void setEquipment(@NotNull ItemData handMain, @NotNull ItemData handSecondary, @NotNull ItemData armorHead, @NotNull ItemData armorChest, @NotNull ItemData armorLegs, @NotNull ItemData armorFeet)
		{
			this.handMain = handMain;
			this.handSecondary = handSecondary;
			this.armorHead = armorHead;
			this.armorChest = armorChest;
			this.armorLegs = armorLegs;
			this.armorFeet = armorFeet;
			this.onEquipmentChanged();
		}

		public final void metadataChanged(@NotNull EntityMetadata<?, ?>[] metadata)
		{
			for (EntityMetadata<?, ? extends MetadataType<?>> entityMetadata : metadata)
			{
				this.onMetadataFieldChanged(entityMetadata);
			}
			this.afterMetadataBatchChange();
		}

		public final void attributesChanged(@NotNull Attribute[] attributes)
		{
			for (Attribute attribute : attributes)
			{
				this.onAttributeChanged(attribute);
			}
			this.afterAttributeBatchChange();
		}

		public final void hurt()
		{
			this.onHurt();
		}

		protected void onAdded()
		{
			// empty
		}

		protected void onRemoved()
		{
			// empty
		}

		protected void onPositionChanged()
		{
			// empty
		}

		protected void onVelocityChanged()
		{
			// empty
		}

		protected void onHeadYawChanged()
		{
			// empty
		}

		protected void onEquipmentChanged()
		{
			// empty
		}

		protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
		{
			// empty
		}

		protected void afterMetadataBatchChange()
		{
			// empty
		}

		protected void onAttributeChanged(@NotNull Attribute attributes)
		{
			// empty
		}

		protected void afterAttributeBatchChange()
		{
			// empty
		}

		protected void onHurt()
		{
			// empty
		}

		public boolean handleEvent(@NotNull EntityEvent entityEvent)
		{
			return false;
		}

		public boolean handleAnimation(@NotNull Animation animation)
		{
			return false;
		}

		public final void setInitialPosition(@NotNull Vector3f pos, @NotNull Vector3f velocity, boolean onGround, float yaw, float pitch, float headYaw)
		{
			this.pos = pos;
			this.velocity = velocity;
			this.onGround = onGround;
			this.yaw = yaw;
			this.pitch = pitch;
			this.headYaw = headYaw;
		}

		@NotNull
		public final Vector3f getPos()
		{
			return this.pos;
		}

		@NotNull
		public final Vector3f getVelocity()
		{
			return this.velocity;
		}

		public final boolean getOnGround()
		{
			return this.onGround;
		}

		public final float getYaw()
		{
			return this.yaw;
		}

		public final float getPitch()
		{
			return this.pitch;
		}

		public final float getHeadYaw()
		{
			return this.headYaw;
		}

		@NotNull
		public final ItemData getHandMain()
		{
			return this.handMain;
		}

		@NotNull
		public final ItemData getHandSecondary()
		{
			return this.handSecondary;
		}

		@NotNull
		public final ItemData getArmorHead()
		{
			return this.armorHead;
		}

		@NotNull
		public final ItemData getArmorChest()
		{
			return this.armorChest;
		}

		@NotNull
		public final ItemData getArmorLegs()
		{
			return this.armorLegs;
		}

		@NotNull
		public final ItemData getArmorFeet()
		{
			return this.armorFeet;
		}

		protected final void sendAttack()
		{
			ServerboundInteractPacket serverboundInteractPacket = new ServerboundInteractPacket(this.instanceId, InteractAction.ATTACK, Hand.MAIN_HAND, false);
			this.sendJavaPacket(serverboundInteractPacket);
		}

		protected final void sendInteract()
		{
			ServerboundInteractPacket serverboundInteractPacket = new ServerboundInteractPacket(this.instanceId, InteractAction.INTERACT, Hand.MAIN_HAND, false);
			this.sendJavaPacket(serverboundInteractPacket);
		}

		protected final void addBedrockEntity(@NotNull String identifier, @NotNull BedrockEntityInstance bedrockEntityInstance)
		{
			this.requireEntityManager().addBedrockEntity(identifier, bedrockEntityInstance);
		}

		protected final void addBedrockItemEntity(@NotNull BedrockEntityInstance bedrockEntityInstance, @NotNull ItemData item)
		{
			this.requireEntityManager().addBedrockItemEntity(bedrockEntityInstance, item);
		}

		protected final void sendJavaPacket(@NotNull MinecraftPacket packet)
		{
			this.requireEntityManager().playerSession.sendJavaPacket(packet);
		}

		protected final void sendBedrockPacket(@NotNull BedrockPacket packet)
		{
			this.requireEntityManager().playerSession.sendBedrockPacket(packet);
		}

		@NotNull
		private EntityManager requireEntityManager()
		{
			if (this.entityManager == null)
			{
				throw new IllegalStateException();
			}
			return this.entityManager;
		}

		protected static <V, T extends MetadataType<V>> boolean getMetadataField(@NotNull EntityMetadata<?, ?> metadata, int metadataId, @NotNull T targetType, @NotNull Consumer<V> consumer)
		{
			if (metadata.getId() == metadataId)
			{
				if (metadata.getType() != targetType)
				{
					LogManager.getLogger().warn("Java server sent bad entity metadata (ID " + metadataId + " of type " + metadata.getType() + " did not match expected type " + targetType + ")");
					return false;
				}
				consumer.accept((V) metadata.getValue());
				return true;
			}
			return false;
		}

		protected static double getAttributeValueWithModifiers(@NotNull Attribute attribute)
		{
			double value = attribute.getValue();
			for (AttributeModifier modifier : attribute.getModifiers())
			{
				if (modifier.getOperation() == ModifierOperation.ADD)
				{
					value += modifier.getAmount();
				}
			}
			double base = value;
			for (AttributeModifier modifier : attribute.getModifiers())
			{
				if (modifier.getOperation() == ModifierOperation.ADD_MULTIPLIED)
				{
					value += base * modifier.getAmount();
				}
			}
			for (AttributeModifier modifier : attribute.getModifiers())
			{
				if (modifier.getOperation() == ModifierOperation.ADD)
				{
					value += value * modifier.getAmount();
				}
			}
			if (attribute.getType() instanceof AttributeType.Builtin)
			{
				value = Math.min(Math.max(value, ((AttributeType.Builtin) attribute.getType()).getMin()), ((AttributeType.Builtin) attribute.getType()).getMax());
			}
			return value;
		}
	}

	public static class BedrockEntityInstance
	{
		private EntityManager entityManager = null;
		private long instanceId;

		private Vector3f pos = Vector3f.ZERO;
		private Vector3f velocity = Vector3f.ZERO;
		private boolean onGround;
		private float yaw;
		private float pitch;
		private float headYaw;

		private ItemData handMain = ItemData.AIR;
		private ItemData handSecondary = ItemData.AIR;

		private ItemData armorHead = ItemData.AIR;
		private ItemData armorChest = ItemData.AIR;
		private ItemData armorLegs = ItemData.AIR;
		private ItemData armorFeet = ItemData.AIR;

		public BedrockEntityInstance()
		{
			// empty
		}

		public final long getInstanceId()
		{
			this.requireEntityManager();
			return this.instanceId;
		}

		public final void remove()
		{
			this.requireEntityManager().removeBedrockEntity(this);
		}

		public void onAttack()
		{
			// empty
		}

		public void onInteract()
		{
			// empty
		}

		protected void getEntityData(@NotNull Map<EntityDataType<?>, ?> entityData)
		{
			// empty
		}

		protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
		{
			// empty
		}

		protected void getEntityAttributes(@NotNull LinkedList<AttributeData> entityAttributes)
		{
			// empty
		}

		@NotNull
		public final Vector3f getPos()
		{
			return this.pos;
		}

		public final void setPos(@NotNull Vector3f pos)
		{
			this.pos = pos;
		}

		@NotNull
		public final Vector3f getVelocity()
		{
			return this.velocity;
		}

		public final void setVelocity(@NotNull Vector3f velocity)
		{
			this.velocity = velocity;
		}

		public final boolean getOnGround()
		{
			return this.onGround;
		}

		public final void setOnGround(boolean onGround)
		{
			this.onGround = onGround;
		}

		public final float getYaw()
		{
			return this.yaw;
		}

		public final void setYaw(float yaw)
		{
			this.yaw = yaw;
		}

		public final float getPitch()
		{
			return this.pitch;
		}

		public final void setPitch(float pitch)
		{
			this.pitch = pitch;
		}

		public final float getHeadYaw()
		{
			return this.headYaw;
		}

		public final void setHeadYaw(float headYaw)
		{
			this.headYaw = headYaw;
		}

		@NotNull
		public final ItemData getHandMain()
		{
			return this.handMain;
		}

		public final void setHandMain(@NotNull ItemData handMain)
		{
			this.handMain = handMain;
		}

		@NotNull
		public final ItemData getHandSecondary()
		{
			return this.handSecondary;
		}

		public final void setHandSecondary(@NotNull ItemData handSecondary)
		{
			this.handSecondary = handSecondary;
		}

		@NotNull
		public final ItemData getArmorHead()
		{
			return this.armorHead;
		}

		public final void setArmorHead(@NotNull ItemData armorHead)
		{
			this.armorHead = armorHead;
		}

		@NotNull
		public final ItemData getArmorChest()
		{
			return this.armorChest;
		}

		public final void setArmorChest(@NotNull ItemData armorChest)
		{
			this.armorChest = armorChest;
		}

		@NotNull
		public final ItemData getArmorLegs()
		{
			return this.armorLegs;
		}

		public final void setArmorLegs(@NotNull ItemData armorLegs)
		{
			this.armorLegs = armorLegs;
		}

		@NotNull
		public final ItemData getArmorFeet()
		{
			return this.armorFeet;
		}

		public final void setArmorFeet(@NotNull ItemData armorFeet)
		{
			this.armorFeet = armorFeet;
		}

		public final void sendPosition()
		{
			// TODO: does it matter if we use relative or absolute packets e.g. regarding client-side interpolation?
			MoveEntityAbsolutePacket moveEntityAbsolutePacket = new MoveEntityAbsolutePacket();
			moveEntityAbsolutePacket.setRuntimeEntityId(this.instanceId);
			moveEntityAbsolutePacket.setPosition(this.pos);
			moveEntityAbsolutePacket.setRotation(Vector3f.from(this.pitch, this.yaw, this.headYaw));
			moveEntityAbsolutePacket.setOnGround(this.onGround);
			moveEntityAbsolutePacket.setForceMove(false);
			moveEntityAbsolutePacket.setTeleported(false);
			this.sendBedrockPacket(moveEntityAbsolutePacket);
		}

		public final void sendVelocity()
		{
			SetEntityMotionPacket setEntityMotionPacket = new SetEntityMotionPacket();
			setEntityMotionPacket.setRuntimeEntityId(this.instanceId);
			setEntityMotionPacket.setMotion(this.velocity);
			this.sendBedrockPacket(setEntityMotionPacket);
		}

		public final void sendHeadYaw()
		{
			MoveEntityDeltaPacket moveEntityDeltaPacket = new MoveEntityDeltaPacket();
			moveEntityDeltaPacket.setRuntimeEntityId(this.instanceId);
			moveEntityDeltaPacket.setHeadYaw(this.headYaw);
			moveEntityDeltaPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_HEAD_YAW);
			this.sendBedrockPacket(moveEntityDeltaPacket);
		}

		public final void sendData()
		{
			SetEntityDataPacket setEntityDataPacket = new SetEntityDataPacket();
			setEntityDataPacket.setRuntimeEntityId(this.instanceId);
			HashMap<EntityDataType<?>, ?> bedrockEntityData = new HashMap<>();
			this.getEntityData(bedrockEntityData);
			setEntityDataPacket.getMetadata().putAll(bedrockEntityData);
			EnumSet<EntityFlag> bedrockEntityFlags = EnumSet.noneOf(EntityFlag.class);
			this.getEntityFlags(bedrockEntityFlags);
			setEntityDataPacket.getMetadata().putFlags(bedrockEntityFlags);
			this.sendBedrockPacket(setEntityDataPacket);

			UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
			updateAttributesPacket.setRuntimeEntityId(this.instanceId);
			LinkedList<AttributeData> bedrockAttributes = new LinkedList<>();
			this.getEntityAttributes(bedrockAttributes);
			updateAttributesPacket.setAttributes(bedrockAttributes);
			this.sendBedrockPacket(updateAttributesPacket);
		}

		public final void sendEquipment()
		{
			MobEquipmentPacket mobEquipmentPacket = new MobEquipmentPacket();
			mobEquipmentPacket.setRuntimeEntityId(this.instanceId);
			mobEquipmentPacket.setItem(this.handMain);
			mobEquipmentPacket.setInventorySlot(0);
			mobEquipmentPacket.setContainerId(0);
			this.sendBedrockPacket(mobEquipmentPacket);

			// TODO: secondary hand

			// TODO: armor does not display
			MobArmorEquipmentPacket mobArmorEquipmentPacket = new MobArmorEquipmentPacket();
			mobArmorEquipmentPacket.setRuntimeEntityId(this.instanceId);
			mobArmorEquipmentPacket.setHelmet(this.armorHead);
			mobArmorEquipmentPacket.setChestplate(this.armorChest);
			mobArmorEquipmentPacket.setLeggings(this.armorLegs);
			mobArmorEquipmentPacket.setBoots(this.armorLegs);
			this.sendBedrockPacket(mobArmorEquipmentPacket);
		}

		public final void sendEvent(@NotNull EntityEventType entityEventType)
		{
			EntityEventPacket entityEventPacket = new EntityEventPacket();
			entityEventPacket.setRuntimeEntityId(this.instanceId);
			entityEventPacket.setType(entityEventType);
			this.sendBedrockPacket(entityEventPacket);
		}

		public final void sendEvent(@NotNull EntityEventType entityEventType, int data)
		{
			EntityEventPacket entityEventPacket = new EntityEventPacket();
			entityEventPacket.setRuntimeEntityId(this.instanceId);
			entityEventPacket.setType(entityEventType);
			entityEventPacket.setData(data);
			this.sendBedrockPacket(entityEventPacket);
		}

		public final void sendAnimation(@NotNull AnimatePacket.Action action)
		{
			AnimatePacket animatePacket = new AnimatePacket();
			animatePacket.setRuntimeEntityId(this.instanceId);
			animatePacket.setAction(action);
			this.sendBedrockPacket(animatePacket);
		}

		protected final void sendJavaPacket(@NotNull MinecraftPacket packet)
		{
			this.requireEntityManager().playerSession.sendJavaPacket(packet);
		}

		protected final void sendBedrockPacket(@NotNull BedrockPacket packet)
		{
			this.requireEntityManager().playerSession.sendBedrockPacket(packet);
		}

		@NotNull
		private EntityManager requireEntityManager()
		{
			if (this.entityManager == null)
			{
				throw new IllegalStateException();
			}
			return this.entityManager;
		}

		protected static <T> void putEntityData(@NotNull Map<EntityDataType<?>, ?> entityData, @NotNull EntityDataType<T> entityDataType, T value)
		{
			((Map<EntityDataType<?>, Object>) entityData).put(entityDataType, value);
		}
	}
}