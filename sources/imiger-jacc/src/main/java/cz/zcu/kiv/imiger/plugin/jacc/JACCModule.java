package cz.zcu.kiv.imiger.plugin.jacc;

import cz.zcu.kiv.imiger.spi.IModule;
import cz.zcu.kiv.imiger.vo.Graph;

import com.google.gson.Gson;
import java.util.regex.Pattern;

/**
 * IModule implementation for JACC plugin.
 */
public class JACCModule implements IModule {

    /**
     * Returns name of this module.
     *
     * @return - name of this module
     */
    @Override
    public String getModuleName() {
        return "Dependency graph of Java application (ZIP with JARs)";
    }

    @Override
    public Pattern getFileNamePattern() {
        return Pattern.compile("(?s).+\\.(zip)");
    }

    /**
     * Retrieves ZIP file with JARs which has to be converted to raw JSON that
     * IMiGEr support.
     *
     * @param data data to be converted to raw JSON.
     * @return - raw JSON as string
     */
    @Override
    public String getRawJson(byte[] data) {
        Graph graph = new Graph();  // TODO

        if (!graph.getVertices().isEmpty()) {
            Gson gson = new Gson();
            return gson.toJson(graph, Graph.class);
        } else {
            return "";
        }
    }
}
