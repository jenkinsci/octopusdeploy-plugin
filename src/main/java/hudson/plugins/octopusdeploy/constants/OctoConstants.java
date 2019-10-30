package hudson.plugins.octopusdeploy.constants;

public class OctoConstants {
    public static final class Commands {
        public static final String CREATE_RELEASE_COMMAND = "create-release";
        public static final String DEPLOY_RELEASE_COMMAND = "deploy-release";

        public static final class Arguments {
            public static final String SERVER_URL_ARGUMENT = "--server";
            public static final String API_KEY_ARGUMENT = "--apiKey";

            public static final String PROJECT_NAME_ARGUMENT = "--project";

            public static final String[] MaskedArguments = {API_KEY_ARGUMENT};
        }
    }

    public class Errors {

        public static final String INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT = "OCTOPUS-JENKINS-INPUT-ERROR-0002: %s can not be blank";
    }
}
