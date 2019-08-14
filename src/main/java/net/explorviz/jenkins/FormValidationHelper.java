package net.explorviz.jenkins;

import hudson.FilePath;
import hudson.Util;
import hudson.util.FormValidation;
import hudson.util.Messages;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Provides {@link FormValidation} shortcut methods that can be used like {@link FormValidation#validateRequired}.
 */
public final class FormValidationHelper {
    private FormValidationHelper() {
    }

    /**
     * Validate a given filepath form input. Makes sure a value is given (issuing an error otherwise, unless {@code
     * required} is {@code false}), and checks if the filepath points to an existing file, issuing a warning otherwise.
     *
     * @param workspace Base directory for relative filepaths. Can be {@code null} if no known workspace exists, in
     *                  which case the existence of the filepath isn't validated
     * @param filepath  The filepath that is expected to exist
     * @param required  {@code true} if absence of the {@code filepath} should be considered an error, {@code false}
     *                  means the it is optional, but if specified, should still point to an existing file
     * @return {@link FormValidation}
     */
    public static FormValidation validateFilePath(@Nullable FilePath workspace, @Nullable String filepath,
                                                  boolean required) {
        filepath = Util.fixEmptyAndTrim(filepath);

        if (filepath == null && required) {
            return FormValidation.error(Messages.FormValidation_ValidateRequired());
        }

        // Workspace can be null for projects that haven't been built yet
        if (filepath != null && workspace != null) {
            FilePath child = workspace.child(filepath);
            try {
                if (!child.exists()) {
                    return FormValidation.warning("The given file path doesn't currently exist. " +
                        "Make sure it is available when this build step is run.");
                } else if (child.isDirectory()) {
                    return FormValidation.error("The given file path points to a directory. This is invalid.");
                }
            } catch (IOException | InterruptedException e) {
                return FormValidation.error(e, "Error trying to validate this file path. " +
                    "Make sure it is available when this build step is run.");
            }
        }

        return FormValidation.ok();
    }
}
