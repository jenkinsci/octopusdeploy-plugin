package hudson.plugins.octopusdeploy;

import hudson.util.FormValidation;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OctopusValidatorTest {

    @Test
    public void validateDirectoryWithNullPathPassesValidation() {
        final FormValidation validation = OctopusValidator.validateDirectory(null);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateDirectoryWithEmptyPathPassesValidation() {
        final FormValidation validation = OctopusValidator.validateDirectory("");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateDirectoryWithMissingPathFailsValidation() {
        final FormValidation validation = OctopusValidator.validateDirectory("bad-path");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("This is not a path to a directory");
    }

    @Test
    public void validateDeploymentTimeoutWithNullTimeoutPassesValidation() {
        final FormValidation validation = OctopusValidator.validateDeploymentTimeout(null);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateDeploymentTimeoutWithEmptyTimeoutPassesValidation() {
        final FormValidation validation = OctopusValidator.validateDeploymentTimeout("");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateDeploymentTimeoutWithInvalidTimeoutFormatFailsValidation() {
        final FormValidation validation = OctopusValidator.validateDeploymentTimeout("invalid");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("This is not a valid deployment timeout it should be in the format HH:mm:ss");
    }

    @Test
    public void validateDeploymentTimeoutWithValidTimeoutFormatPassesValidation() {
        final FormValidation validation = OctopusValidator.validateDeploymentTimeout("11:30:00");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

}
