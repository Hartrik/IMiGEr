package cz.zcu.kiv.imiger.plugin.dot;

import cz.zcu.kiv.imiger.plugin.jacc.JaCCModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;

public class JaCCModuleIntegrityTest {

    @Test
    public void testJar() throws IOException {
        test("test-jar.zip");
    }

    @Test
    public void testJarNotFound() throws IOException {
        test("test-jar-not-found.zip");
    }

    @Test
    public void testSimpleBooking() throws IOException {
        test("simple-booking.zip");
    }

    private static void test(String fileName) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/", fileName));

        JaCCModule jaccModule = new JaCCModule();
        String result = jaccModule.getRawJson(data);
        byte[] bytes = result.getBytes(StandardCharsets.UTF_8);

        Files.write(Paths.get("build/", fileName + ".json"), bytes);

        // note: because of JaCC the result may have edges in different order
        byte[] reference = Files.readAllBytes(Paths.get("src/test/resources/", fileName + ".json"));
        Assert.assertEquals(countChecksum(reference), countChecksum(bytes));
    }

    private static int countChecksum(byte[] data) {
        int c = 0;
        for (byte b : data) {
            c += b;
        }
        return c;
    }

}