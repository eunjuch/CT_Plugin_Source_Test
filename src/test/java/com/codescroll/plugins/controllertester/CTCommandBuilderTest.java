package com.codescroll.plugins.controllertester;

import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;

public class CTCommandBuilderTest {
	@ClassRule
	public static JenkinsRule jenkins = new JenkinsRule();
	
	@Test
	public void testWindowCommand() throws Exception {
		// Creating the agent with a specific label
		LabelAtom testingLabel = new LabelAtom("ctAgent");
		DumbSlave agent = jenkins.createSlave(testingLabel);
		
		// Connecting the agent
		jenkins.waitOnline(agent);
		
		FreeStyleProject project = jenkins.createFreeStyleProject();
		project.setAssignedLabel(testingLabel);
		project.getBuildersList().add(new CTCommandBuilder("echo \"CTCommandBuiler Run\""));
		FreeStyleBuild build = project.scheduleBuild2(0).get();
		jenkins.assertBuildStatus(Result.SUCCESS, build);
	}
}