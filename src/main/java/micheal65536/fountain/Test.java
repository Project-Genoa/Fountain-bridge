package micheal65536.fountain;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundPingPacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundPongPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;

import java.util.Arrays;

public class Test
{
	public static void main(String[] args)
	{
		MinecraftProtocol javaProtocol = new MinecraftProtocol("user");
		TcpClientSession tcpClientSession = new TcpClientSession("127.0.0.1", 25565, javaProtocol);
		tcpClientSession.addListener(new SessionAdapter()
		{
			@Override
			public void packetReceived(Session session, Packet packet)
			{
				if (packet instanceof ClientboundLoginPacket)
				{
					ServerboundClientInformationPacket serverboundClientInformationPacket = new ServerboundClientInformationPacket("en_GB", 20, ChatVisibility.FULL, true, Arrays.asList(SkinPart.values()), HandPreference.RIGHT_HAND, false, true);
					session.send(serverboundClientInformationPacket);
				}
				else if (packet instanceof ClientboundPingPacket)
				{
					session.send(new ServerboundPongPacket(((ClientboundPingPacket) packet).getId()));
				}
			}
		});
		tcpClientSession.connect(true);

		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException exception)
		{
			// empty
		}
	}
}