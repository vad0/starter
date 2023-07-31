package starter;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static starter.Starter.createNecessaryFolders;

class StarterTest {
    private static final String CONFIG_1_PATH = "src/test/resources/bcd/1_def.conf";
    private static final String CONFIG_2_PATH = "src/test/resources/abc/2_serv.conf";

    @Test
    public void loadBase() {
        loadBase(CONFIG_2_PATH, CONFIG_1_PATH);
        loadBase(CONFIG_1_PATH, CONFIG_2_PATH);
    }

    public void loadBase(String a, String b) {
        var command = Starter.buildCommand(new String[]{a, b});
        var expectedCommand = """
            -XX:+UseZGC \
            -XX:ConcGCThreads=1 \
            -Xlog:gc*:file=/tmp/app/hide/gc.log \
            -XX:+DebugNonSafepoints \
            -XX:+UnlockExperimentalVMOptions \
            --add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
            -Dagrona.disable.bounds.checks=false \
            -Dapp.batchingNs=1000000 \
            -Dapp.o2oBufferSizeMb=20 \
            -Dapp.rootLogPath=/tmp/app \
            -Dlog4j2.AsyncLogger.SleepTimeNs=50000000 \
            -Dlog4j2.AsyncLogger.WaitStrategy=SLEEP \
            -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
            main.Run""";
        assertEquals(expectedCommand, command);
    }

    @Test
    public void testUnquote() {
        assertEquals("asd", Starter.unquote("asd"));
        assertEquals("asd", Starter.unquote("\"asd\""));
    }

    @Test
    public void testEnvConfig() {
        var str = """
            {
                flags:["c", "d"]
                "system_properties":{"a":"b", "e":"f"}
            }
            """;
        var config = ConfigFactory.parseString(str);
        var flags = config.getStringList("flags");
        var systemProperties = config.getConfig("system_properties");
        assertEquals(List.of("c", "d"), flags);
        assertEquals(systemProperties.getString("a"), "b");
        assertEquals(systemProperties.getString("e"), "f");
    }

    @Test
    public void testPrepareFlags() {
        var rawFlags = List.of("-ea", "-XX:+UseZGC", "-ea", "-XX:+UseSerialGC", "-Dapp.remainingShare=0.35");
        var flags = Starter.prepareFlags(rawFlags);
        var expected = List.of("-ea", "-XX:+UseSerialGC", "-Dapp.remainingShare=0.35");
        assertEquals(expected, flags);
    }

    @Test
    public void testEnvConfig2() {
        var envConfig = """
            {"flags": ${flags}["-da"], \
            "system_properties": {\
            app.batchingNs: "12778736"}}""";
        var config = Starter.buildConfig(new String[]{CONFIG_1_PATH, CONFIG_2_PATH}, envConfig);
        int batchingNs = config.getConfig("system_properties").getInt("app.batchingNs");
        assertEquals(12778736, batchingNs);
    }

    @Test
    public void testNoQuotes() {
        var envConfig = """
            {"flags": ${flags}["-da"], \
            "system_properties": {\
            "app.batchingNs": "12778736"}}""";
        assertThrows(
            RuntimeException.class,
            () -> Starter.buildConfig(new String[]{CONFIG_1_PATH, CONFIG_2_PATH}, envConfig));
    }

    @Test
    public void testCreateFolder() {
        var rawFlags = List.of(
            "-Dapp.remainingShare=0.35",
            "-Xlog:gc*:file=/tmp/trade_loader/gc.log");

        var createdFolders = new ArrayList<String>();
        createNecessaryFolders(rawFlags, f -> createdFolders.add(f.toString()));

        assertEquals(List.of("/tmp/trade_loader"), createdFolders);
    }
}