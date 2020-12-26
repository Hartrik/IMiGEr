package cz.zcu.kiv.imiger.plugin.jacc;

import cz.zcu.kiv.ccu.ApiCheckersFactory;
import cz.zcu.kiv.ccu.ApiCheckersSetting;
import cz.zcu.kiv.ccu.ApiInterCompatibilityChecker;
import cz.zcu.kiv.ccu.ApiInterCompatibilityResult;
import cz.zcu.kiv.imiger.vo.Graph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 *
 */
public class GraphBuilder {

    private final byte[] data;

    public GraphBuilder(byte[] data) {
        this.data = data;
    }

    public Graph build() throws IOException {
        ApiCheckersSetting setting = new ApiCheckersSetting.Builder().defaultSett().build();
        ApiInterCompatibilityChecker<File> checker = ApiCheckersFactory.getApiInterCompatibilityChecker(setting);

        try (TemporaryZipFileExtractor zipFileExtractor = new TemporaryZipFileExtractor(data)) {
            Path tempDirectory = zipFileExtractor.getTempDirectory();

            File[] files = tempDirectory.toFile().listFiles();
            ApiInterCompatibilityResult result = checker.checkInterCompatibility(null, files);
            GraphMaker graphMaker = new GraphMaker(result, files);
            try {
                return graphMaker.generate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
