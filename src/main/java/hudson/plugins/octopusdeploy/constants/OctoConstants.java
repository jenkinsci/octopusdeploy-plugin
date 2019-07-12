package hudson.plugins.octopusdeploy.constants;

public class OctoConstants {
    public static final class Commands {
        public static final String CREATE_RELEASE = "create-release";
        public static final String DEPLOY_RELEASE = "deploy-release";

        public static final class Arguments {
            public static final String SERVER_URL = "--server";
            public static final String API_KEY = "--apiKey";
            public static final String SPACE_NAME = "--space";
            public static final String PROJECT_NAME = "--project";

            public static final String[] MaskedArguments = {API_KEY};
        }
    }

    public class Errors {

        public static final String INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT = "OCTOPUS-JENKINS-INPUT-ERROR-0002: %s can not be blank";
    }
}
