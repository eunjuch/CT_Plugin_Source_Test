package com.codescroll.plugins.controllertester;

import java.io.IOException;


import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

public class CTCommandBuilder extends Builder implements SimpleBuildStep {
	// Project from Local
	private String command;
	
	@DataBoundConstructor
	public CTCommandBuilder(String command) {
		this.command = command;
	}
	
	public String getCommand() {
		return command;
	}
	
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		BatchFile batchFile = new BatchFile(command);
		if (launcher.isUnix() || !batchFile.perform((AbstractBuild<?, ?>)run, launcher, listener)) {
			run.setResult(Result.FAILURE);
		}
	}
	
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		public DescriptorImpl() {
			load();
		}
		
		@Override
		public String getDisplayName() {
			return "Controller Tester Command";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}
}
