package hudson.plugins.octopusdeploy.exception;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ServerConfigurationNotFoundExceptionTest {

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void constructServerConfigurationNotFoundException(final String serverId, final String message) {
        assertThatThrownBy(() -> {throw new ServerConfigurationNotFoundException(serverId);})
                .hasMessage(message);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideTestArguments() {
        return Stream.of(
                Arguments.of(
                        null,
                        "Server Id not supplied and no default server could be found"),
                Arguments.of(
                        "",
                        "Server Id not supplied and no default server could be found"),
                Arguments.of(
                        "serverId",
                        "Server configuration, 'serverId', not found. " +
                                "The serverId parameter needs to match the Server ID of a configured OctopusDeploy" +
                                " server at Manage Jenkins -> Configure System -> OctopusDeploy Plugin"));
    }

}
