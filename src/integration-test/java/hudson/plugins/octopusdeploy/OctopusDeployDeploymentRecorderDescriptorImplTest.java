package hudson.plugins.octopusdeploy;

import com.octopus.helper.BaseRecorderTest;
import com.octopus.sdk.domain.Project;
import com.octopus.sdk.domain.ProjectGroup;
import com.octopus.sdk.model.release.ReleaseResource;
import com.octopus.sdk.model.tag.TagResource;
import com.octopus.sdk.model.tenant.TenantResource;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class OctopusDeployDeploymentRecorderDescriptorImplTest extends BaseRecorderTest {

    private OctopusDeployDeploymentRecorder.DescriptorImpl descriptor;

    @BeforeEach
    public void setUp() {
        descriptor = new OctopusDeployDeploymentRecorder.DescriptorImpl();
    }

    @Test
    public void doCheckProject() {
        final ProjectGroup projGroup = spaceScopedClient.createProjectGroup("ProjGroup1");
        spaceScopedClient.createProject("Proj1", projGroup.getProperties().getId());

        FormValidation validation = descriptor.doCheckProject("Proj1",
                JENKINS_OCTOPUS_SERVER_ID,
                spaceScopedClient.getSpaceId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doCheckDeploymentTimeout() {
        final FormValidation validation = descriptor.doCheckDeploymentTimeout("11:00:00");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doCheckReleaseVersionWithNullProjectFailsValidation() {
        FormValidation validation = descriptor.doCheckReleaseVersion("1.0.0",
                null,
                JENKINS_OCTOPUS_SERVER_ID,
                spaceScopedClient.getSpaceId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(validation.getMessage()).isEqualTo("Project must be set to validate release.");
    }

    @Test
    public void doCheckReleaseVersionWithEmptyProjectFailsValidation() {
        FormValidation validation =
                descriptor.doCheckReleaseVersion("1.0.0",
                        "",
                        JENKINS_OCTOPUS_SERVER_ID,
                        spaceScopedClient.getSpaceId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(validation.getMessage()).isEqualTo("Project must be set to validate release.");
    }

    @Test
    public void doCheckReleaseVersionWithoutCorrespondingProjectFailsValidation() {
        FormValidation validation =
                descriptor.doCheckReleaseVersion("1.0.0",
                        "Proj1",
                        JENKINS_OCTOPUS_SERVER_ID,
                        spaceScopedClient.getSpaceId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Unable to validate release because the project 'Proj1' couldn't be found.");
    }

    @Test
    public void doCheckReleaseVersion() throws IOException {
        final ProjectGroup projGroup = spaceScopedClient.createProjectGroup("ProjGroup1");
        final Project proj1 = spaceScopedClient.createProject("Proj1", projGroup.getProperties().getId());
        spaceScopedClient.getSpace().releases().create(new ReleaseResource("1.0.0", proj1.getProperties().getId()));

        FormValidation validation =
                descriptor.doCheckReleaseVersion("1.0.0",
                        "Proj1",
                        JENKINS_OCTOPUS_SERVER_ID,
                        spaceScopedClient.getSpaceId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doCheckEnvironment() {
        spaceScopedClient.createEnvironment("Env1");

        final FormValidation validation = descriptor.doCheckEnvironment("Env1",
                JENKINS_OCTOPUS_SERVER_ID,
                spaceScopedClient.getSpaceId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doFillEnvironmentItems() {
        List<String> environmentsToCreate = Arrays.asList("Env1", "Env2", "Env3");
        environmentsToCreate.forEach(name -> spaceScopedClient.createEnvironment(name));

        final ComboBoxModel model = descriptor.doFillEnvironmentItems(JENKINS_OCTOPUS_SERVER_ID,
                spaceScopedClient.getSpaceId());

        assertThat(model).containsExactlyInAnyOrderElementsOf(environmentsToCreate);
    }

    @Test
    public void doFillProjectItems() {
        final ProjectGroup projGroup = spaceScopedClient.createProjectGroup("ProjGroup1");
        List<String> projectsToCreate = Arrays.asList("Proj1", "Proj2", "Proj3");
        projectsToCreate.forEach(name -> spaceScopedClient.createProject(name, projGroup.getProperties().getId()));

        final ComboBoxModel model = descriptor.doFillProjectItems(JENKINS_OCTOPUS_SERVER_ID,
                spaceScopedClient.getSpaceId());

        assertThat(model).containsExactlyInAnyOrderElementsOf(projectsToCreate);
    }

    @Test
    public void doFillTenantTagItems() throws IOException {
        List<String> tenantTagsToCreate = Arrays.asList("Tag1", "Tag2");
        spaceScopedClient.createTagSet("TagSet");
        tenantTagsToCreate.forEach(name ->
                spaceScopedClient.createTagInTagSet(name, "#333333", "TagSet"));
        final List<TagResource> tags =
                Objects.requireNonNull(spaceScopedClient
                                .getSpace()
                                .tagSets()
                                .getByName("TagSet")
                                .orElse(null))
                        .getProperties()
                        .getTags();

        final TenantResource tenant = new TenantResource("Tenant1");
        tenant.setTenantTags(new HashSet<>(tags
                .stream()
                .map(TagResource::getCanonicalTagName).collect(Collectors.toList())));
        spaceScopedClient.getSpace().tenants().create(tenant);

        final ComboBoxModel model = descriptor.doFillTenantTagItems(JENKINS_OCTOPUS_SERVER_ID,
                spaceScopedClient.getSpaceId());

        assertThat(model.size()).isEqualTo(tenantTagsToCreate.size());
        assertThat(model)
                .containsAll(tenantTagsToCreate.stream().map(tag -> "TagSet/" + tag).collect(Collectors.toList()));
    }

    @Test
    public void doFillTenantItems() {
        List<String> tenantsToCreate = Arrays.asList("Tenant1", "Tenant2", "Tenant3");
        tenantsToCreate.forEach(name -> spaceScopedClient.createTenant(name));

        final ComboBoxModel model = descriptor.doFillTenantItems(JENKINS_OCTOPUS_SERVER_ID,
                spaceScopedClient.getSpaceId());

        assertThat(model).containsExactlyInAnyOrderElementsOf(tenantsToCreate);
    }
}
