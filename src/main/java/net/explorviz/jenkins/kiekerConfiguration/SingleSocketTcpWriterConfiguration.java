package net.explorviz.jenkins.kiekerConfiguration;

import org.apache.commons.lang.Validate;

import java.io.IOException;

/**
 * A kieker configuration using the
 * <pre>
 *     kieker.monitoring.writer.tcp.SingleSocketTcpWriter
 * </pre>
 * writer to send records to a TCP server, like ExplorViz.
 */
public class SingleSocketTcpWriterConfiguration extends AbstractKiekerConfiguration {
    private static final long serialVersionUID = 5631963572612065232L;

    private static final String WRITER_CLASS_TCP = "kieker.monitoring.writer.tcp.SingleSocketTcpWriter";

    public static final String PROP_HOSTNAME =
        "kieker.monitoring.writer.tcp.SingleSocketTcpWriter.hostname"; // default: localhost
    public static final String PROP_PORT =
        "kieker.monitoring.writer.tcp.SingleSocketTcpWriter.port"; // default: 10133

    public SingleSocketTcpWriterConfiguration() throws IOException {
        super(WRITER_CLASS_TCP);
    }

    public void setHostname(String hostname) {
        Validate.notEmpty(hostname, "hostname may not be empty");
        this.configuration.setProperty(PROP_HOSTNAME, hostname);
    }

    public String getHostname() {
        return this.configuration.getStringProperty(PROP_HOSTNAME);
    }

    public void setPort(int port) {
        Validate.isTrue(port > 0 && port <= 65535, "port must be in range (0,65535]");
        this.configuration.setProperty(PROP_PORT, port);
    }

    public int getPort() {
        return this.configuration.getIntProperty(PROP_PORT);
    }
}
