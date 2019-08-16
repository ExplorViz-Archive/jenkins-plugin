package net.explorviz.jenkins.kiekerConfiguration;

import hudson.FilePath;
import kieker.common.configuration.Configuration;
import kieker.common.configuration.ReadOnlyConfiguration;

import java.io.IOException;
import java.io.Serializable;

/**
 * Wraps a kieker {@link Configuration} with utility methods.
 * Implementing classes should offer setters and getters to set relevant options directly.
 */
public interface KiekerConfiguration extends Serializable {
    /**
     * @return The backing {@link Configuration} used to store the options.
     */
    Configuration getConfiguration();

    /**
     * @return A read-only copy of the backing {@link Configuration}.
     * @see ReadOnlyConfiguration
     */
    Configuration readOnlyConfiguration();

    /**
     * Write the current configuration to the given file.
     *
     * @param outputFile File to write to. Will be created if it doesn't exist.
     */
    void write(FilePath outputFile) throws IOException, InterruptedException;
}
