package cz.zcu.kiv.imiger.plugin.spade;

import com.google.gson.Gson;
import cz.zcu.kiv.imiger.spi.IModule;
import cz.zcu.kiv.imiger.vo.Graph;
import cz.zcu.kiv.imiger.plugin.spade.graph.GraphManager;
import cz.zcu.kiv.imiger.plugin.spade.graph.loader.GraphJSONDataLoader;
import cz.zcu.kiv.imiger.plugin.spade.graph.loader.JSONConfigLoader;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Spade implements IModule {
    @Override
    public String getModuleName() {
        return "From Spade JSON";
    }

    @Override
    public Pattern getFileNamePattern() {
        return Pattern.compile("(?s).+\\.json");
    }

    /**
     * Convert input spade JSON to RAW JSON.
     *
     * @param data String to be converted to raw JSON.
     * @return Raw JSON.
     */
    @Override
    public String getRawJson(byte[] data) {
        String stringToConvert = new String(data, StandardCharsets.UTF_8);

        GraphManager graphManager = new GraphJSONDataLoader(stringToConvert).loadData();
        JSONConfigLoader configLoader = new JSONConfigLoader(graphManager);

        Graph graph = graphManager.createGraph(configLoader);
        return new Gson().toJson(graph);
    }
}
