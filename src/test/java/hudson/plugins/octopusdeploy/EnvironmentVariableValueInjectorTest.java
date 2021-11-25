package hudson.plugins.octopusdeploy;

import hudson.EnvVars;
import hudson.util.VariableResolver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvironmentVariableValueInjectorTest {

    final EnvVars envVars = new EnvVars(Collections.singletonMap("ENV_VAR", "env_value"));
    final VariableResolver<String> resolver =  new VariableResolver.ByMap<>(envVars);
    final EnvironmentVariableValueInjector injector = new EnvironmentVariableValueInjector(resolver, envVars);

    @ParameterizedTest
    @NullAndEmptySource
    public void injectEnvironmentVariableValuesEarlyExitReturnsCandidate(final String candidate) {
        assertThat(injector.injectEnvironmentVariableValues(candidate)).isEqualTo(candidate);
    }

    @Test
    public void injectEnvironmentVariableValuesUnmatchedReturnsCandidate() {
        final String unmatchedCandidate = "${unmatched}";
        assertThat(injector.injectEnvironmentVariableValues(unmatchedCandidate)).isEqualTo(unmatchedCandidate);
    }

    @Test
    public void injectEnvironmentVariableValuesMatchesUsingEnvVars() {
        final String matchedCandidate = "${ENV_VAR}";
        assertThat(injector.injectEnvironmentVariableValues(matchedCandidate)).isEqualTo(envVars.get("ENV_VAR"));
    }

    // TODO - Enable test after method refactor
    @Disabled("Refactor method under test, should be able to resolve without '${}' - See VariableResolver<V> javadoc")
    @Test
    public void injectEnvironmentVariableValuesMatchesUsingResolver() {
        final String matchedCandidate = "ENV_VAR";
        assertThat(injector.injectEnvironmentVariableValues(matchedCandidate)).isEqualTo(envVars.get("ENV_VAR"));
    }

}
