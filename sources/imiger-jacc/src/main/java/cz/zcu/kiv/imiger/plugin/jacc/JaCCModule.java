package cz.zcu.kiv.imiger.plugin.jacc;

import cz.zcu.kiv.imiger.spi.IModule;
import cz.zcu.kiv.imiger.vo.Graph;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.regex.Pattern;

/**
 * IModule implementation for JaCC plugin.
 */
public class JaCCModule implements IModule {

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
     * @return raw JSON as string
     */
    @Override
    public String getRawJson(byte[] data) {
        GraphBuilder graphBuilder = new GraphBuilder(data);
        try {
            Graph graph = graphBuilder.build();
            if (!graph.getVertices().isEmpty()) {
                Gson gson = new Gson();
                return gson.toJson(graph, Graph.class);
            } else {
                return "";
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
