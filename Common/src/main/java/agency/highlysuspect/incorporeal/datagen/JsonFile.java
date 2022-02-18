package agency.highlysuspect.incorporeal.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * ("Thing which can be written as json", "list of strings") tuple.
 * First path segment is the jar root, so probably "assets" or "data"
 * <p>
 * passing ("data", "incorporeal", "recipes", "blah") will write to data/incorporeal/recipes/blah.json.
 * investigate: could this be a (JValue<?>, Path) tuple instead? idk
 */
public record JsonFile(JsonElement value, List<String> pathSegments) {
	public static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	
	public static JsonFile create(JsonElement value, String... pathSegments) {
		return new JsonFile(value, List.of(pathSegments));
	}
	
	public Path getOutputPath(DataGenerator datagen) {
		Path path = datagen.getOutputFolder();
		
		//grimy code to concatenate all path segments with a .json on the end
		//there's not like a "Path#withFileName" or anything >.>
		//probably could be cleaner
		for(int i = 0; i < pathSegments.size(); i++) {
			if(i == pathSegments.size() - 1) path = path.resolve(pathSegments.get(i) + ".json");
			else path = path.resolve(pathSegments.get(i));
		}
		
		return path;
	}
	
	//wrapper for DataProvider#save that swallows errors
	//mainly so you don't have to slap "throws IOException" on the whole mod
	//and so you can use them inside lambdas (for the same reason)
	public void save(DataGenerator datagen, HashCache cache) {
		try {
			DataProvider.save(PRETTY_GSON, cache, value, getOutputPath(datagen));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
