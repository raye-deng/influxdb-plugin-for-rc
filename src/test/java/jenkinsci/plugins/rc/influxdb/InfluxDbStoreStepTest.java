package jenkinsci.plugins.rc.influxdb;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.rc.influxdb.models.Target;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class InfluxDbStoreStepTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private FilePath workspace = new FilePath(new File("."));
    @Mock
    private Run build;
    @Mock
    private Launcher launcher;
    @Mock
    private TaskListener listener;
    @Mock
    private EnvVars envVars;

    @Test
    public void testEmptyTargetShouldThrowException() throws Exception {
        exception.expect(RuntimeException.class);
        exception.expectMessage("Target was null!");

        InfluxDbStoreStep.DescriptorImpl descriptorMock = Mockito.mock(InfluxDbStoreStep.DescriptorImpl.class);
        Jenkins jenkinsMock = Mockito.mock(Jenkins.class);
        Mockito.when(descriptorMock.getTargets()).thenReturn(Collections.emptyList());
        Mockito.when(jenkinsMock.getDescriptorByType(InfluxDbStoreStep.DescriptorImpl.class)).thenReturn(descriptorMock);

        try {
            new InfluxDbStoreStep("").perform(build, workspace, envVars, launcher, listener);
        } catch (NullPointerException e) {
            Assert.fail("NullPointerException raised");
        }
    }

    @Test
    @Issue("JENKINS-61305")
    public void testConfigRoundTripShouldPreserveSelectedTarget() throws Exception {
        InfluxDbGlobalConfig globalConfig = InfluxDbGlobalConfig.getInstance();
        Target target1 = new Target();
        target1.setDescription("Target1");
        Target target2 = new Target();
        target2.setDescription("Target2");
        globalConfig.addTarget(target1);
        globalConfig.addTarget(target2);

        InfluxDbStoreStep before = new InfluxDbStoreStep("Target2");
        assertEquals(before.getSelectedTarget(), "Target2");
        assertEquals(before.getTarget(), target2);

        FreeStyleProject project = j.createFreeStyleProject();
        project.getPublishersList().add(before);
        j.submit(j.createWebClient().getPage(project,"configure").getFormByName("config"));

        InfluxDbStoreStep after = project.getPublishersList().get(InfluxDbStoreStep.class);
        j.assertEqualBeans(before, after, "selectedTarget");
    }
}
