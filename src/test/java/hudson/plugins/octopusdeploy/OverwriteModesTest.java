package hudson.plugins.octopusdeploy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class OverwriteModesTest {

    @Test
    public void getOverwriteModesReturnsPopulatedMap() {
        final Map<String, String> overwriteModes = OverwriteModes.getOverwriteModes();

        assertThat(overwriteModes).contains(
                entry(OverwriteMode.FailIfExists.name(), "Fail if exists"),
                entry(OverwriteMode.OverwriteExisting.name(), "Overwrite existing"),
                entry(OverwriteMode.IgnoreIfExists.name(), "Ignore if exists")
        );
    }
}
