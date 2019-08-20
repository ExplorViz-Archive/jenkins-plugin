package net.explorviz.jenkins.kiekerConfiguration;

import hudson.Util;
import org.apache.commons.lang.Validate;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * A kieker configuration using the
 * <pre>
 *     kieker.monitoring.writer.filesystem.FileWriter
 * </pre>
 * writer to write records to a local directory.
 */
public class FileWriterConfiguration extends AbstractKiekerConfiguration {
    private static final long serialVersionUID = 8387714832520127794L;

    private static final String WRITER_CLASS_FILE = "kieker.monitoring.writer.filesystem.FileWriter";

    public static final String PROP_STORAGE_PATH = WRITER_CLASS_FILE + ".customStoragePath"; // default: empty

    public FileWriterConfiguration() throws IOException {
        super(WRITER_CLASS_FILE);
    }

    /**
     * Set the path where kieker will store the record files in.
     *
     * @param storagePath Can be an empty string to use the default file path (which is up to Kieker)
     */
    public void setStoragePath(@Nonnull String storagePath) {
        Validate.notNull(storagePath, "storagePath may not be null");
        this.configuration.setProperty(PROP_STORAGE_PATH, storagePath);
    }

    public String getStoragePath() {
        return this.configuration.getStringProperty(PROP_STORAGE_PATH);
    }
}
