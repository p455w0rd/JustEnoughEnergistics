package p455w0rd.jee.init;

import net.minecraftforge.fml.common.Loader;

public class ModIntegration {

    public static enum Mods {
        JEI("jei", "Just Enough Items");
        
        private String modid;
        private String name;

        private Mods(String modidIn, String nameIn) {
            this.modid = modidIn;
            this.name = nameIn;
        }

        public String getId() {
            return this.modid;
        }

        public String getName() {
            return this.name;
        }

        public boolean isLoaded() {
            return Loader.isModLoaded((String)this.getId());
        }
    }

}

