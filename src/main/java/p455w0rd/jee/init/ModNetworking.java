package p455w0rd.jee.init;

import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import p455w0rd.jee.packets.PacketJEIPatternRecipe;

public class ModNetworking {

	private static final SimpleNetworkWrapper INSTANCE = new SimpleNetworkWrapper(ModGlobals.MODID);

	public static SimpleNetworkWrapper getInstance() {
		return INSTANCE;
	}

	public static void init() {
		ModNetworking.getInstance().registerMessage(PacketJEIPatternRecipe.class, PacketJEIPatternRecipe.class, 0, Side.SERVER);
	}

}
