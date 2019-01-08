package p455w0rd.jee;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import p455w0rd.jee.init.ModGlobals;
import p455w0rd.jee.init.ModNetworking;

@Mod(modid = ModGlobals.MODID, name = ModGlobals.NAME, version = ModGlobals.VERSION, dependencies = ModGlobals.DEP_LIST, acceptedMinecraftVersions = "[1.12.2]")
public class JustEnoughEnergistics {

	@Instance(value = "jee")
	public static JustEnoughEnergistics INSTANCE;

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		ModNetworking.init();
	}

}
