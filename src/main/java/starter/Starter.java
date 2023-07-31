package starter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * What parts of config are used?
 * <il>
 * <li>system_properties map</li>
 * <li>flags array</li>
 * <li>--add-opens map</li>
 * <li>--add-exports map</li>
 * <li>main class</li>
 * </il>
 */
public class Starter {
    public static final String SYSTEM_PROPERTIES_KEY = "system_properties";
    /**
     * Env config is applied after all other configs. It stores config itself, not a path to config.
     */
    private static final String ENV_CONFIG = System.getProperty("envConfig");
    private static final String ADD_OPENS = "--add-opens";
    private static final String ADD_EXPORTS = "--add-exports";
    private static final List<String> FLAGS_TO_CREATE_FOLDERS = List.of("-Xlog");

    /**
     * @param configs are applied in <strong>alphabetic</strong> order. Later overrides and extends earlier.
     */
    public static void main(String[] configs) {
        System.out.println(buildCommand(configs));
    }

    static String buildCommand(String[] configs) {
        Config config = buildConfig(configs, ENV_CONFIG);
        List<String> commandParts = buildCommandParts(config);
        createNecessaryFolders(commandParts, Starter::createFolder);
        return joinCommandParts(commandParts);
    }

    public static void createDirs(Config config) {
        List<String> commandParts = buildCommandParts(config);
        createNecessaryFolders(commandParts, Starter::createFolder);
    }

    private static String joinCommandParts(List<String> command) {
        var joiner = new StringJoiner(" ");
        for (var item : command) {
            joiner.add(item);
        }
        return joiner.toString();
    }

    public static void addSystemProperties(List<String> args, Config config) {
        var res = new ArrayList<String>();
        for (var entry : getSystemProperties(config).entrySet()) {
            var option = "-D" + entry.getKey() + "=" + entry.getValue();
            res.add(option);
        }
        // Sort for predictability
        res.sort(Comparator.naturalOrder());
        args.addAll(res);
    }

    public static Map<String, String> getSystemProperties(Config config) {
        return config.getConfig(SYSTEM_PROPERTIES_KEY)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> unquote(entry.getKey()),
                e -> e.getValue().unwrapped().toString()));
    }

    private static String string(ConfigValue value) {
        return String.valueOf(value.unwrapped());
    }

    public static List<String> getAllFlags(Config config) {
        List<String> res = new ArrayList<>();
        res.addAll(getFlags(config));
        res.addAll(getJigsawFlags(config));
        return res;
    }

    public static List<String> getFlags(Config config) {
        var rawFlags = config.getStringList("flags");
        return prepareFlags(rawFlags);
    }

    /**
     * This method removes duplicate flags and keeps only the latest GC flag
     */
    static List<String> prepareFlags(List<String> rawFlags) {
        List<String> flags = new ArrayList<>();
        boolean hasGcFlag = false;
        for (int i = rawFlags.size() - 1; i >= 0; i--) {
            var flag = rawFlags.get(i);
            if (flag.endsWith("GC")) {
                if (hasGcFlag) {
                    continue;
                }
                hasGcFlag = true;
                flags.add(flag);
                continue;
            }
            if (flags.contains(flag)) {
                // avoid duplicates
                continue;
            }
            flags.add(flag);
        }
        Collections.reverse(flags);
        return flags;
    }

    public static List<String> getJigsawFlags(Config config) {
        List<String> res = new ArrayList<>();
        res.addAll(getJigsawFlags(config, ADD_OPENS));
        res.addAll(getJigsawFlags(config, ADD_EXPORTS));
        return res;
    }

    private static List<String> getJigsawFlags(Config config, String addType) {
        List<String> jigsawFlags = new ArrayList<>();
        var jigsaw = getSubConfig(config, addType);
        for (var entry : jigsaw.entrySet()) {
            jigsawFlags.add(addType);
            jigsawFlags.add(keyValue(entry));
        }
        return jigsawFlags;
    }

    private static String keyValue(Map.Entry<String, ConfigValue> entry) {
        final String key = unquote(entry.getKey());
        final String value = string(entry.getValue());
        return key + "=" + value;
    }

    private static Config getSubConfig(Config config, String path) {
        if (config.hasPath(path)) {
            return config.getConfig(path);
        } else {
            return ConfigFactory.empty();
        }
    }

    public static String unquote(String in) {
        if (in.charAt(0) != '\"') {
            return in;
        }
        int last = in.length() - 1;
        assert in.charAt(last) == '\"';
        return in.substring(1, last);
    }

    public static Config buildConfig(String[] configs, String envConfigString) {
        Arrays.sort(configs, Comparator.comparing(s -> Path.of(s).getFileName()));
        return buildConfigSorted(configs, envConfigString);
    }

    private static Config buildConfigSorted(String[] configs, String envConfigString) {
        var unresolvedConfig = ConfigFactory.empty();
        for (int i = configs.length - 1; i >= 0; i--) {
            String path = configs[i];
            final File file = new File(path);
            if (!file.exists()) {
                System.err.println("No such file " + file.getAbsolutePath());
                System.exit(1);
            }
            var config = ConfigFactory.parseFile(file);
            unresolvedConfig = unresolvedConfig.withFallback(config);
        }
        if (envConfigString != null) {
            var envConfig = ConfigFactory.parseString(envConfigString);
            unresolvedConfig = envConfig.withFallback(unresolvedConfig);
        }
        var resolvedConfig = unresolvedConfig.resolve();
        checkQuotes(resolvedConfig);
        return resolvedConfig;
    }

    private static void checkQuotes(Config resolvedConfig) {
        var keysWithQuotes = resolvedConfig.getConfig(SYSTEM_PROPERTIES_KEY)
            .entrySet()
            .stream()
            .map(Map.Entry::getKey)
            .filter(k -> k.contains("\""))
            .toList();
        if (!keysWithQuotes.isEmpty()) {
            throw new RuntimeException("Keys with quotes: " + keysWithQuotes);
        }
    }

    public static List<String> buildCommandParts(Config config) {
        List<String> args = new ArrayList<>(getAllFlags(config));
        addSystemProperties(args, config);
        args.add(getMainClass(config));
        return args;
    }

    public static String getMainClass(Config config) {
        return config.getString("main");
    }

    private static void createFolder(Path f) {
        try {
            Files.createDirectories(f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createNecessaryFolders(List<String> args, Consumer<Path> folderCreator) {
        for (var arg : args) {
            for (var flagStart : FLAGS_TO_CREATE_FOLDERS) {
                if (arg.startsWith(flagStart)) {
                    var argElements = arg.split("=");
                    // we split flag XXXX=YYY and process YYY as target file
                    if (argElements.length != 2) {
                        continue;
                    }
                    var targetFile = Path.of(argElements[1]).getParent();
                    folderCreator.accept(targetFile);
                }
            }
        }
    }
}
