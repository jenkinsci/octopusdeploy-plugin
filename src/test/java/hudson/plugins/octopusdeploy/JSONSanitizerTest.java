package hudson.plugins.octopusdeploy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JSONSanitizerTest {

    @Test
    public void jsonSanitizerProducesExpectedHtmlString() {
        String testString = "These release notes include quotes and some special characters.\n" +
                "Consider this: \"I am a quote\" -anonymous, or \"\"I am a double-quote\" -anonymous\" -some other guy\n" +
                "Sometimes you have some \"quotes\", sometimes some other characters like ! @ # $ % ^ & * () - + = _ {} [] ~ `\n" +
                "Backslashes too: C:\\Program Files (x86)\\Jenkins\\workspace or \"C:\\Program Files (x86)\\Jenkins\\workspace\"\n" +
                "\\\\ 2 backslashes\n" +
                "	This paragraph starts with a tab. This paragraph starts with a tab. This paragraph starts with a tab.\n" +
                "This paragraph starts with a tab. This paragraph starts with a tab. This paragraph starts with a tab.\n";

        final String answer = "These release notes include quotes and some special characters.<br/>" +
                "Consider this: \\\"I am a quote\\\" -anonymous, or \\\"\\\"I am a double-quote\\\" -anonymous\\\" -some other guy<br/>" +
                "Sometimes you have some \\\"quotes\\\", sometimes some other characters like ! @ # $ % ^ & * () - + = _ {} [] ~ `<br/>" +
                "Backslashes too: C:\\u005CProgram Files (x86)\\u005CJenkins\\u005Cworkspace or \\\"C:\\u005CProgram Files (x86)\\u005CJenkins\\u005Cworkspace\\\"<br/>\\u005C\\u005C 2 backslashes<br/>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;This paragraph starts with a tab. This paragraph starts with a tab. " +
                "This paragraph starts with a tab.<br/>This paragraph starts with a tab. This paragraph starts with a tab. This paragraph starts with a tab.<br/>";

        String sanitized = JSONSanitizer.getInstance().sanitize(testString);
        assertThat(sanitized).isEqualTo(answer);
    }
}
