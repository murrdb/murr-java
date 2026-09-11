package io.murrdb.client;

import java.net.URI;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * One murr server for the whole test JVM. The image tag follows the client version: {@code 0.2.1-1}
 * tests against {@code murr:0.2.1}. Set {@code MURR_IMAGE} to point at another image.
 */
final class MurrContainer {

    private static final int HTTP_PORT = 8080;
    private static final GenericContainer<?> CONTAINER = start();

    private MurrContainer() {}

    static URI endpoint() {
        return URI.create("http://" + CONTAINER.getHost() + ":" + CONTAINER.getMappedPort(HTTP_PORT));
    }

    private static GenericContainer<?> start() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(image()))
                .withExposedPorts(HTTP_PORT)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("murr")))
                .waitingFor(Wait.forHttp("/health").forPort(HTTP_PORT));
        container.start();
        return container;
    }

    private static String image() {
        String override = System.getenv("MURR_IMAGE");
        if (override != null && !override.isBlank()) {
            return override;
        }
        String clientVersion = System.getProperty("murr.client.version");
        if (clientVersion == null) {
            throw new IllegalStateException("murr.client.version is not set; run through maven or set MURR_IMAGE");
        }
        return "ghcr.io/murrdb/murr:" + serverVersion(clientVersion);
    }

    // 0.2.1-1 or 0.2.1-1-SNAPSHOT tests against server 0.2.1
    static String serverVersion(String clientVersion) {
        return clientVersion.replaceFirst("-SNAPSHOT$", "").replaceFirst("-\\d+$", "");
    }
}
