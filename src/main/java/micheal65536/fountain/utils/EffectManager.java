package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.level.event.BreakBlockEventData;
import com.github.steveice10.mc.protocol.data.game.level.event.LevelEventData;
import com.github.steveice10.mc.protocol.data.game.level.particle.Particle;
import com.github.steveice10.mc.protocol.data.game.level.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.level.sound.Sound;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.math.vector.Vector4f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.PlayerSession;
import micheal65536.fountain.mappings.DirectSounds;
import micheal65536.fountain.registry.JavaBlocks;

// this groups level events, particle events, and sound events together and dispatches each event in the appropriate way for Bedrock
public class EffectManager
{
	private final PlayerSession playerSession;

	public EffectManager(@NotNull PlayerSession playerSession)
	{
		this.playerSession = playerSession;
	}

	public boolean handleLevelEvent(@NotNull Vector3i position, @NotNull com.github.steveice10.mc.protocol.data.game.level.event.LevelEvent event, @NotNull LevelEventData eventData)
	{
		if (!(event instanceof com.github.steveice10.mc.protocol.data.game.level.event.LevelEventType))
		{
			return false;
		}

		switch ((com.github.steveice10.mc.protocol.data.game.level.event.LevelEventType) event)
		{
			case BREAK_BLOCK ->
			{
				int javaId = ((BreakBlockEventData) eventData).getBlockState();
				JavaBlocks.BedrockMapping bedrockMapping = JavaBlocks.getBedrockMapping(javaId);
				if (bedrockMapping == null)
				{
					LogManager.getLogger().warn("Break block level event for block with no mapping {}", JavaBlocks.getName(javaId));
					return true;
				}
				this.sendLevelEvent(LevelEvent.PARTICLE_DESTROY_BLOCK, bedrockMapping.id, position.toFloat());
				return true;
			}
			default ->
			{
				return false;
			}
		}
	}

	public boolean handleParticleEvent(@NotNull Particle particle, @NotNull Vector3f position, @NotNull Vector4f offset, int amount, boolean longDistance)
	{
		return false;
	}

	public boolean handleSoundEvent(@NotNull Sound sound, @NotNull Vector3f position, float pitch, float volume)
	{
		if (!(sound instanceof BuiltinSound))
		{
			return false;
		}

		String javaName = sound.getName();

		// TODO: do specific handling here

		String bedrockName = DirectSounds.getDirectSoundMapping(javaName);
		if (bedrockName == null)
		{
			LogManager.getLogger().warn("No direct sound mapping for {}", javaName);
			return true;
		}
		else if (bedrockName.equals("_ignore"))
		{
			return true;
		}
		else
		{
			this.sendPlaySound(bedrockName, position, pitch, volume);
			return true;
		}
	}

	private void sendLevelEvent(@NotNull LevelEventType levelEventType, int data, @NotNull Vector3f position)
	{
		LevelEventPacket levelEventPacket = new LevelEventPacket();
		levelEventPacket.setType(levelEventType);
		levelEventPacket.setData(data);
		levelEventPacket.setPosition(position);
		this.playerSession.sendBedrockPacket(levelEventPacket);
	}

	private void sendPlaySound(@NotNull String sound, @NotNull Vector3f position, float pitch, float volume)
	{
		PlaySoundPacket playSoundPacket = new PlaySoundPacket();
		playSoundPacket.setSound(sound);
		playSoundPacket.setPosition(position);
		playSoundPacket.setPitch(pitch);
		playSoundPacket.setVolume(volume);
		this.playerSession.sendBedrockPacket(playSoundPacket);
	}

	private void sendSoundEvent(@NotNull SoundEvent soundEvent, @NotNull String identifier, int extraData, @NotNull Vector3f position)
	{
		LevelSoundEventPacket levelSoundEventPacket = new LevelSoundEventPacket();
		levelSoundEventPacket.setSound(soundEvent);
		levelSoundEventPacket.setIdentifier(identifier);
		levelSoundEventPacket.setExtraData(extraData);
		levelSoundEventPacket.setPosition(position);
		levelSoundEventPacket.setBabySound(false);
		levelSoundEventPacket.setRelativeVolumeDisabled(false);
		this.playerSession.sendBedrockPacket(levelSoundEventPacket);
	}
}