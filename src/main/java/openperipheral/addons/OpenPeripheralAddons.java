package openperipheral.addons;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import openmods.Mods;
import openmods.OpenMods;
import openmods.api.IProxy;
import openmods.config.BlockInstances;
import openmods.config.ItemInstances;
import openmods.config.game.ModStartupHelper;
import openmods.config.game.RegisterBlock;
import openmods.config.game.RegisterItem;
import openmods.config.properties.ConfigProcessing;
import openmods.network.event.NetworkEventManager;
import openperipheral.addons.glasses.*;
import openperipheral.addons.glasses.GlassesEvent.GlassesChangeBackground;
import openperipheral.addons.glasses.GlassesEvent.GlassesComponentMouseButtonEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesComponentMouseWheelEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesKeyDownEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesKeyUpEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesMouseButtonEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesMouseWheelEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetGuiVisibility;
import openperipheral.addons.glasses.GlassesEvent.GlassesSetKeyRepeat;
import openperipheral.addons.glasses.GlassesEvent.GlassesSignalCaptureEvent;
import openperipheral.addons.glasses.GlassesEvent.GlassesStopCaptureEvent;
import openperipheral.addons.glasses.TerminalEvent.TerminalClearEvent;
import openperipheral.addons.glasses.TerminalEvent.TerminalDataEvent;
import openperipheral.addons.glasses.TerminalEvent.TerminalResetEvent;
import openperipheral.addons.pim.BlockPIM;
import openperipheral.addons.pim.TileEntityPIM;
import openperipheral.addons.selector.BlockSelector;
import openperipheral.addons.selector.TileEntitySelector;
import openperipheral.addons.sensors.AdapterSensor;
import openperipheral.addons.sensors.BlockSensor;
import openperipheral.addons.sensors.TileEntitySensor;
import openperipheral.addons.ticketmachine.TileEntityTicketMachine;
import openperipheral.api.ApiAccess;
import openperipheral.api.adapter.IPeripheralAdapterRegistry;
import openperipheral.api.meta.IItemStackMetaBuilder;

import org.apache.commons.lang3.ObjectUtils;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

@Mod(modid = OpenPeripheralAddons.MODID, name = "OpenPeripheralAddons", version = "$VERSION$", dependencies = "required-after:OpenMods@[$LIB-VERSION$,$NEXT-LIB-VERSION$);required-after:OpenPeripheralCore;after:ComputerCraft@[1.70,]")
public class OpenPeripheralAddons {

	public static final String MODID = "OpenPeripheral";

	public static class Blocks implements BlockInstances {
		@RegisterBlock(name = "glassesbridge", tileEntity = TileEntityGlassesBridge.class, itemBlock = ItemGlassesBridge.class)
		public static BlockGlassesBridge glassesBridge;

		@RegisterBlock(name = "pim", tileEntity = TileEntityPIM.class, unlocalizedName = "playerinventory")
		public static BlockPIM pim;

		@RegisterBlock(name = "sensor", tileEntity = TileEntitySensor.class)
		public static BlockSensor sensor;

		@RegisterBlock(name = "selector", tileEntity = TileEntitySelector.class)
		public static BlockSelector selector;
	}

	public static class Items implements ItemInstances {
		@RegisterItem(name = "glasses")
		public static ItemGlasses glasses;

		@RegisterItem(name = "keyboard")
		public static ItemKeyboard keyboard;

		@RegisterItem(name = "generic")
		public static ItemOPGeneric generic;
	}

	public static class Icons {
		public static IIcon sensorTurtle;
		public static IIcon narcissiticTurtle;
	}

	public static int renderId;

	@Instance(MODID)
	public static OpenPeripheralAddons instance;

	@SidedProxy(clientSide = "openperipheral.addons.proxy.ClientProxy", serverSide = "openperipheral.addons.proxy.ServerProxy")
	public static IProxy proxy;

	public static CreativeTabs tabOpenPeripheralAddons = new CreativeTabs("tabOpenPeripheralAddons") {
		@Override
		public Item getTabIconItem() {
			return ObjectUtils.firstNonNull(Items.glasses, net.minecraft.init.Items.fish);
		}
	};

	private final ModStartupHelper startupHelper = new ModStartupHelper("openperipheral") {
		@Override
		protected void populateConfig(Configuration config) {
			ConfigProcessing.processAnnotations("OpenPeripheralAddons", config, Config.class);
		}
	};

	@EventHandler
	public void preInit(FMLPreInitializationEvent evt) {
		startupHelper.registerBlocksHolder(OpenPeripheralAddons.Blocks.class);
		startupHelper.registerItemsHolder(OpenPeripheralAddons.Items.class);

		if (Loader.isModLoaded(Mods.COMPUTERCRAFT)) ModuleComputerCraft.preInit(startupHelper);
		if (Loader.isModLoaded(Mods.RAILCRAFT)) ModuleRailcraft.preInit(startupHelper);

		startupHelper.preInit(evt.getSuggestedConfigurationFile());

		Recipes.register();
		MetasGeneric.registerItems();

		NetworkEventManager.INSTANCE
				.startRegistration()
				.register(TerminalResetEvent.class)
				.register(TerminalClearEvent.class)
				.register(TerminalDataEvent.class)

				.register(GlassesSignalCaptureEvent.class)
				.register(GlassesStopCaptureEvent.class)
				.register(GlassesMouseWheelEvent.class)
				.register(GlassesMouseButtonEvent.class)
				.register(GlassesComponentMouseWheelEvent.class)
				.register(GlassesComponentMouseButtonEvent.class)
				.register(GlassesKeyDownEvent.class)
				.register(GlassesKeyUpEvent.class)
				.register(GlassesChangeBackground.class)
				.register(GlassesSetKeyRepeat.class)
				.register(GlassesSetGuiVisibility.class);

		Items.generic.initRecipes();

		IPeripheralAdapterRegistry adapters = ApiAccess.getApi(IPeripheralAdapterRegistry.class);
		adapters.registerInline(TileEntityGlassesBridge.class);
		adapters.registerInline(TileEntityTicketMachine.class);
		adapters.registerInline(TileEntitySelector.class);
		adapters.register(new AdapterSensor());

		IItemStackMetaBuilder itemStackBuilder = ApiAccess.getApi(IItemStackMetaBuilder.class);
		itemStackBuilder.register(new ItemTerminalMetaProvider());

		MinecraftForge.EVENT_BUS.register(TerminalManagerServer.instance);

		NetworkRegistry.INSTANCE.registerGuiHandler(instance, OpenMods.proxy.wrapHandler(null));

		proxy.preInit();
	}

	@EventHandler
	public void handleRenames(FMLMissingMappingsEvent event) {
		startupHelper.handleRenames(event);
	}

	@EventHandler
	public void init(FMLInitializationEvent evt) {
		proxy.init();
		proxy.registerRenderInformation();

		if (Loader.isModLoaded(Mods.COMPUTERCRAFT)) ModuleComputerCraft.init();
	}
}
