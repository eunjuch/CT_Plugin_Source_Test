package com.codescroll.plugins.controllertester;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.SingleFileSCM;

import hudson.model.FreeStyleProject;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;

public class CTDefaultBuilderTest {
	
	@ClassRule
	public static JenkinsRule jenkins = new JenkinsRule();
	
	@Rule
	public LoggerRule logger = new LoggerRule();
	
	// TODO
	@Test
	public void testCt() throws Exception {
		
		// Creating the agent with a specific label
		LabelAtom testingLabel = new LabelAtom("ctAgent");
		DumbSlave agent = jenkins.createSlave(testingLabel);
		
		// Connecting the agent
		jenkins.waitOnline(agent);
		
		FreeStyleProject project = jenkins.createFreeStyleProject();
		project.setAssignedLabel(testingLabel);
		
		project.setScm(new SingleFileSCM("test.c", """
					#include <stdio.h>
					using namespace std;
					
					int main() {
						printf("Hello World!");
					}
				"""));
		// form 입력 체크
		
		// 워크스페이스에서 파일 가져오기
		/*
		 * FilePath workspace = j.jenkins.getWorkspaceFor(job);
		 * FilePath report = workspace.child("target").child("lint-results.xml");
		 * report.copyFrom(getClass().getResourceAsStream("lint-results_r20.xml"));
		 */
		
	}
}
