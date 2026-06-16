package universal66.liteshield;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;

final class Update {
    static Runnable updatePending = null;
    static void please() {
        try {
            var website = URI.create("https://github.com/Universal66/LiteShield/releases/latest/download/LiteShield.jar").toURL();
            var rbc = Channels.newChannel(website.openStream());
            var fos = new FileOutputStream("plugins/LiteShield-update.jar");

            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.getChannel().close();
            fos.close();

            rbc.close();

            var path = Path.of("plugins/LiteShield-update.jar");
            var classLoader = new URLClassLoader(new URL[]{
                    path.toUri().toURL()
            });

            var newVersion = getPropertyFromYAML(classLoader.getResourceAsStream("plugin.yml"), "version");
            var oldVersion = "1.0";

            if (newVersion.equalsIgnoreCase(oldVersion)) {
                classLoader.close();
                Files.delete(path);
                System.out.println("[LiteShield] [AutoUpdater] No new update available.");
            } else {
                Path self = findThis();
                if (self == null) {
                    System.out.println("[LiteShield] [AutoUpdater] Self plugin JAR couldn't be found!");
                    return;
                }

                try {
                    Class<?> cls = classLoader.loadClass("universal66.liteshield.UpdateDynamic");
                    var method = cls.getDeclaredMethod("any");
                    method.invoke(null);
                } catch (Exception e) {
//                    LOGGER.warning("Failed to execute on-update code.");
                }

                classLoader.close();

                updatePending = () -> {
                    if (Files.notExists(self) || Files.notExists(path)) {
                        System.out.println("[LiteShield] [AutoUpdater] Update missing!");
                        return;
                    }

                    try {
                        while (true) {
                            try {
                                Files.delete(self);
                            } catch (IOException e) {
                                try {
                                    Thread.sleep(70);
                                } catch (InterruptedException ex) {
                                    System.out.println("[LiteShield] [AutoUpdater] Update interrupted!");
                                    return;
                                }

                                continue;
                            } catch (Exception e) {
                                System.out.println("[LiteShield] [AutoUpdater] Update interrupted!");
                                return;
                            }

                            break;
                        }

                        Files.move(path, self);

                        System.out.println("[LiteShield] [AutoUpdater] Update successful.");
                    } catch (IOException e) {
                        System.out.println("[LiteShield] [AutoUpdater] Failed to update!");
                    }
                };

                System.out.println("[LiteShield] [AutoUpdater] Update pending.");
            }
        } catch (UnknownHostException ex) {
            System.out.println("[LiteShield] [AutoUpdater] Omitting auto-update (no internet connection).");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[LiteShield] [AutoUpdater] Cannot auto-update!");
        }
    }

    private static Path findThis() {
        try (var filesStream = Files.list(Path.of("plugins"))) {
            var files = filesStream.toList();
            for (Path path : files) {
                if (path.getFileName().toString().endsWith(".jar")) {
                    try (
                            URLClassLoader classLoader =
                                    new URLClassLoader(new URL[]{path.toUri().toURL()})
                    ) {
                        var name = getPropertyFromYAML(classLoader.getResourceAsStream("plugin.yml"), "name");
                        if (name.equalsIgnoreCase("LiteShield")) {
                            return path;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException e) {
            System.out.println("[LiteShield] [AutoUpdater] Failed to find the self plugin JAR");
        }

        return null;
    }

    private static String getPropertyFromYAML(InputStream stream, String prop) throws IOException {
        try (stream) {
            var bytes = stream.readAllBytes();
            var lines = new String(bytes).split("\n");

            for (var line : lines) {
                if (line.startsWith(prop + ": ")) {
                    return line.split(":")[1].trim().replaceAll("['\"]", "");
                }
            }
        }

        return "";
    }
}
