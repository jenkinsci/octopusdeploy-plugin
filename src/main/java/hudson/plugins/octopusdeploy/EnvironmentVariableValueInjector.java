package hudson.plugins.octopusdeploy;

import hudson.util.VariableResolver;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injects environment variable values into a string.
 */
public class EnvironmentVariableValueInjector {
    private final Pattern pattern;
    
    public EnvironmentVariableValueInjector() {
        pattern = Pattern.compile("\\$\\{(?<variable>[^\\}]+)\\}");
    }    
    
    /**
     * Takes a string possibly containing tokens that represent Environment Variables and replaces them with the variables' values.
     * If the variable is not defined, the token is not replaced.
     * @param candidate the candidate string possibly containing env tokens.
     * @param resolver the resolver that can find values for environment variables.
     * @return a new string with all possible tokens replaced with values.
     */
    public String injectEnvironmentVariableValues(String candidate, VariableResolver resolver) {
        if (!candidate.contains("${")) { // Early exit
            return candidate;
        }
        String resolved = new String(candidate);
        int locatedMatch = 0;
        Matcher matcher = pattern.matcher(resolved);
        while (matcher.find(locatedMatch)) {
            String variableName = matcher.group("variable");
            locatedMatch = matcher.end();
            Object resolvedVariable = resolver.resolve(variableName);
            if (resolvedVariable != null) {
                resolved = resolved.replace(String.format("${%s}", variableName), resolvedVariable.toString());
            }
        }
        
        return resolved;
    }
}
