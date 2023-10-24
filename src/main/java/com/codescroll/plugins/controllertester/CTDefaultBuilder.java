package com.codescroll.plugins.controllertester;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import com.codescroll.plugins.controllertester.project.CTGeneralProject;
import com.codescroll.plugins.controllertester.project.ProjectManager;
import com.codescroll.plugins.controllertester.util.GitInfo;
import com.codescroll.plugins.controllertester.util.KeyConstant;
import com.codescroll.plugins.controllertester.util.PathProvider;
import com.google.inject.Inject;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.UserRemoteConfig;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;

public class CTDefaultBuilder extends Builder implements SimpleBuildStep {
	
	private static final String ARTIFACT_PATTERN = "ct/report/TestReport_*.*, ct/ini/*.ini";
	
	private static final String GENERAL_PROJECT = "generalProject";
	private static final String TEAM_PROJECT = "teamProject";
	
	/**
	 * Repository Option
	 */
	private String projectOption;
	
	/**
	 * Project from Local
	 */
	private String localProjectPath;
	
	/**
	 * Project from team
	 */
	// 팀 프로젝트 변수
	
	/**
	 * Source Code from Git
	 */
	private String gitSourcePath;
	private String gitSourceRootPath;
	private String gitSourceBranch;
	private String credentialsId;
	
	// RTV Test Execution
	private boolean isRtvTest;
	
	// Report Format
	private boolean htmlReport;
	private boolean pdfReport;
	
	// Advanced
	private String mappingFilePath;
	
	@DataBoundConstructor
	public CTDefaultBuilder(String projectOption, String localProjectPath, String gitSourcePath, String gitSourceBranch,
			String gitSourceRootPath, String credentialsId, boolean isRtvTest, boolean htmlReport, boolean pdfReport,
			String mappingFilePath) {
		this.projectOption = projectOption;
		this.localProjectPath = localProjectPath;
		this.gitSourcePath = gitSourcePath;
		this.gitSourceBranch = gitSourceBranch;
		this.gitSourceRootPath = gitSourceRootPath;
		this.credentialsId = credentialsId;
		this.isRtvTest = isRtvTest;
		this.htmlReport = htmlReport;
		this.pdfReport = pdfReport;
		this.mappingFilePath = mappingFilePath;
	}

	public String getRepoOption() {
		return projectOption;
	}

	public String getLocalProjectPath() {
		return localProjectPath;
	}

	public String getGitProjectPath() {
		return gitSourcePath;
	}

	public String getGitProjectBranch() {
		return gitSourceBranch;
	}
	
	public String getGitProjectRootPath() {
		return gitSourceRootPath;
	}

	public String getCredentialsId() {
		return credentialsId;
	}
	
	public boolean getRtvTest() {
		return isRtvTest;
	}
	
	public boolean getHtmlReport() {
		return htmlReport;
	}
	
	public boolean getPdfReport() {
		return pdfReport;
	}

	public String getMappingFilePath() {
		return mappingFilePath;
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		
		PathProvider pathProvider = new PathProvider(workspace);
		ProjectManager manager = new ProjectManager(pathProvider);
		manager.initWorkspace();
		
		// AbstractBuild 타입으로 Run 타입 형변환 -> 해당 타입 활용해서 job 정보를 가져오고 Publisher 추가
		AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
		addArtifact(build.getProject());
		
		// 내보낸 프로젝트 임시경로로 복사해오기
		if (projectOption.equals(GENERAL_PROJECT)) {
			manager.copyProject(localProjectPath);
			
		} else if (projectOption.equals(TEAM_PROJECT)) {
			/*
			 * 팀프로젝트 가져오기
			 */
		}
		
		// git에서 소스코드 가져오기
		GitInfo gitInfo = new GitInfo(gitSourcePath, gitSourceRootPath, gitSourceBranch, credentialsId);
		manager.cloneSources(gitInfo, run, env, listener);

		// TODO Team일 경우 CTTeamProject 생성
		CTGeneralProject singleProject = new CTGeneralProject(pathProvider.getTempProjectPath());
		
		// 환경변수에 대입
		env.put(KeyConstant.NAME, singleProject.getProjectName());
		env.put(KeyConstant.REMOTE_TARGET, String.valueOf(isRtvTest));
		
		String reportPath = pathProvider.getReportPath().getRemote();
		String replacedReportPath = reportPath.replace(PathProvider.REV_SEPARATOR, PathProvider.SEPARATOR);
		env.put(KeyConstant.OUTPUT_DIR, replacedReportPath);
		
		env.put(KeyConstant.HTML, String.valueOf(htmlReport));
		env.put(KeyConstant.PDF, String.valueOf(pdfReport));
		
		manager.writeInfo(env);
		
		// 커맨드 생성
		// TODO Team일 경우 CTTeamCommand 생성
		CTGeneralCommand generalProjectCommand = new CTGeneralCommand(pathProvider, mappingFilePath);
		
		// import
		if (startCommand(generalProjectCommand.importCommand(), env, launcher, listener) != Result.SUCCESS.ordinal) {
			run.setResult(Result.FAILURE);
			throw new AbortException("Import Fail");
		}
		
		// 최신 소스코드 반영
		listener.getLogger().println("Update Source Code from SCM");
		manager.updateSourceCode(gitSourceRootPath);
		
		// execute
		if (startCommand(generalProjectCommand.executeCommand(), env, launcher, listener) != Result.SUCCESS.ordinal) {
			run.setResult(Result.FAILURE);
			throw new AbortException("Execution Fail");
		}
	}
	
	// 커맨드 실행 메소드
	private int startCommand(ArgumentListBuilder cmd, EnvVars env, Launcher launcher, TaskListener listener) {
		
		String envCtPath = env.get(KeyConstant.CTPATH);
		
		ProcStarter initProcStarter = launcher.launch();
		ProcStarter listenerProcStarter = initProcStarter.stdout(listener).stderr(listener.getLogger());
		ProcStarter cmdProcStarter = listenerProcStarter.pwd(envCtPath).cmds(cmd);
		
		int result = Result.UNSTABLE.ordinal;
		
		try {
			result = cmdProcStarter.join();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		
		return result;
		
	}
	
	// 아티팩트 추가 메소드
	// 보고서, ini 파일 아티팩트로 추가
	private void addArtifact(AbstractProject<?, ?> project) {
		
		// ArtifactArchiver 추가
		if (project.getPublishersList().get(ArtifactArchiver.class) == null) {
			ArtifactArchiver artifactArchiver = createArtifactArchiver();
			project.getPublishersList().add(artifactArchiver);
		}
	}
	
	private ArtifactArchiver createArtifactArchiver() {
		ArtifactArchiver artifactArchiver = new ArtifactArchiver(ARTIFACT_PATTERN);
		artifactArchiver.setOnlyIfSuccessful(true);
		return artifactArchiver;
	}
	
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		@Inject
		private UserRemoteConfig.DescriptorImpl delegate;
		
		public DescriptorImpl() {
			load();
		}
		
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
				@QueryParameter String gitProjectPath,
				@QueryParameter String credentialsId) {
			return delegate.doFillCredentialsIdItems(project, gitProjectPath, credentialsId);
		}

		@RequirePOST
		public FormValidation doCheckLocalProjectPath(@QueryParameter String localProjectPath) throws IOException, ServletException {
			if (localProjectPath.isEmpty()) {
            	return FormValidation.error("필수 입력란입니다.");
            }
            return FormValidation.ok();
        }
		
		@RequirePOST
		public FormValidation doCheckUrl(@AncestorInPath Item item,
									    @QueryParameter String credentialsId,
									    @QueryParameter String gitProjectPath) throws IOException, InterruptedException {
			return delegate.doCheckUrl(item, credentialsId, gitProjectPath);
		}
		
		@RequirePOST
		public FormValidation doCheckGitSourcePath(@QueryParameter String gitSourcePath) throws IOException, ServletException {
            if (gitSourcePath.isEmpty()) {
            	return FormValidation.error("필수 입력란입니다.");
            }
            return FormValidation.ok();
        }
		
		@RequirePOST
		public FormValidation doCheckGitSourceRootPath(@QueryParameter String gitSourceRootPath) throws IOException, ServletException {
			if (gitSourceRootPath.isEmpty()) {
				return FormValidation.error("필수 입력란입니다.");
			}
			return FormValidation.ok();
		}
		
		@RequirePOST
		public FormValidation doCheckGitSourceBranch(@QueryParameter String gitSourceBranch) throws IOException, ServletException {
            if (gitSourceBranch.isEmpty()) {
            	return FormValidation.error("필수 입력란입니다.");
            }
            return FormValidation.ok();
        }
		
		@Override
		public String getDisplayName() {
			return "Controller Tester";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}
}
