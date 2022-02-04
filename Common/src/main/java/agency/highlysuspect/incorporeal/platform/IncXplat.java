package agency.highlysuspect.incorporeal.platform;

import agency.highlysuspect.incorporeal.Inc;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Incorporeal-specific cross-loader abstractions.
 * 
 * In the built mod, this `platform` package contains more platform-specific implementation details, such as
 * an implementation of IncXplat for the particular platform, and the main mod entrypoint.
 */
public interface IncXplat {
	IncXplat INSTANCE = get();
	
	private static IncXplat get() {
		//Literally pasted from Botania, lol
		List<ServiceLoader.Provider<IncXplat>> providers = ServiceLoader.load(IncXplat.class).stream().toList();
		if (providers.size() != 1) {
			throw new IllegalStateException("There should be exactly one IncXplat implementation on the classpath. Found: " + providers.stream().map(p -> p.type().getName()).collect(Collectors.joining(",", "[", "]")));
		} else {
			ServiceLoader.Provider<IncXplat> provider = providers.get(0);
			Inc.LOGGER.debug("Instantiating IncXplat impl: " + provider.type().getName());
			return provider.get();
		}
	}
	
	//Item.Properties with a creative tab set, basically
	Item.Properties defaultItemProperties();
}
