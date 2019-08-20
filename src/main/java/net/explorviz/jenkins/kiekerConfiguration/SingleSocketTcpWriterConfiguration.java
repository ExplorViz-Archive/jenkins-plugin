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

    private static final String PROP_HOSTNAME = WRITER_CLASS_TCP + ".hostname"; // default: localhost
    private static final String PROP_PORT = WRITER_CLASS_TCP + ".port"; // default: 10133

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
