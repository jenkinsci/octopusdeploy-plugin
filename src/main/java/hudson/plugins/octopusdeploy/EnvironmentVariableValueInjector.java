package hudson.plugins.octopusdeploy;

import hudson.EnvVars;
import hudson.util.VariableResolver;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injects environment variable values into a string.
 */
public class EnvironmentVariableValueInjector {
    private final Pattern pattern;
    private final VariableResolver resolver;
    private final EnvVars environment;
    
    public EnvironmentVariableValueInjector(VariableResolver resolver, EnvVars environment) {
        pattern = Pattern.compile("\\$\\{(?<variable>[^\\}]+)\\}");
        this.resolver = resolver;
        this.environment = environment;
    }    
    
    /**
     * Takes a string possibly containing tokens that represent Environment Variables and replaces them with the variables' values.
     * If the variable is not defined, the token is not replaced.
     * First looks in environment variables, then looks at the build variable resolver for values.
     * @param candidate the candidate string possibly containing env tokens.
     * @return a new string with all possible tokens replaced with values.
     */
    public String injectEnvironmentVariableValues(String candidate) {
        if (!candidate.contains("${")) { // Early exit
            return candidate;
        }
        String resolved = new String(candidate);
        int locatedMatch = 0;
        Matcher matcher = pattern.matcher(resolved);
        while (matcher.find(locatedMatch)) {
            String variableName = matcher.group("variable");
            locatedMatch = matcher.end();
            Object resolvedVariable = null;
            resolvedVariable = environment.get(variableName);
            if (resolvedVariable == null) {
                resolvedVariable = resolver.resolve(variableName);
            }
            if (resolvedVariable != null) {
                resolved = resolved.replace(String.format("${%s}", variableName), resolvedVariable.toString());
            }
        }
        
        return resolved;
    }
}
