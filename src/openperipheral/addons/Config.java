package openperipheral.addons;

import net.minecraftforge.common.Configuration;
import openmods.config.BlockId;
import openmods.config.ConfigProcessing;
import openperipheral.addons.common.block.BlockGlassesBridge;

public class Config {

	@BlockId(description = "The id of the glasses bridge")
	public static int blockGlassesBridgeId = 3000;

	public static void readConfig(Configuration configFile) {
		ConfigProcessing.processAnnotations(configFile, Config.class);
	}

	public static void register() {
		if (ConfigProcessing.canRegisterBlock(blockGlassesBridgeId)) {
			OpenPeripheralAddons.Blocks.glassesBridge = new BlockGlassesBridge();
		}

		ConfigProcessing.registerItems(OpenPeripheralAddons.Items.class, "openperipheral");
		ConfigProcessing.registerBlocks(OpenPeripheralAddons.Blocks.class, "openperipheral");
	}
}