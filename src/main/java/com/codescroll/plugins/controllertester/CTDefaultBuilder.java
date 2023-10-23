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
	
	private static final String ARTIFACT_PATTERN = "ctdata/report/TestReport_*.*, ctdata/ini/*.ini";
	
	private static final String LOCAL_REPO = "localRepo";
	private static final String GIT_REPO = "gitRepo";
	
	private static final String HTML_KEY = "HTML";
	private static final String PDF_KEY = "PDF";
	
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	
	/**
	 * Repository Option
	 */
	private String repoOption;
	
	/**
	 * Project from Local
	 */
	private String localProjectPath;
	
	// Project from Git
	private String gitProjectPath;
	private String gitProjectBranch;
	private String gitProjectRootPath;
	private String credentialsId;
	
	// Source Code Root
	private String srcRootPath;
	
	// RTV Test Execution
	private boolean isRtvTest;
	
	// Report Format
	private boolean htmlReport;
	private boolean pdfReport;
	
	// Advanced
	private String mappingFilePath;
	
	@DataBoundConstructor
	public CTDefaultBuilder(String repoOption, String localProjectPath, String gitProjectPath, String gitProjectBranch,
			String gitProjectRootPath, String credentialsId, String srcRootPath, boolean isRtvTest, boolean htmlReport, boolean pdfReport, 
			String mappingFilePath) {
		this.repoOption = repoOption;
		this.localProjectPath = localProjectPath;
		this.gitProjectPath = gitProjectPath;
		this.gitProjectBranch = gitProjectBranch;
		this.gitProjectRootPath = gitProjectRootPath;
		this.credentialsId = credentialsId;
		this.srcRootPath = srcRootPath;
		this.isRtvTest = isRtvTest;
		this.htmlReport = htmlReport;
		this.pdfReport = pdfReport;
		this.mappingFilePath = mappingFilePath;
	}

	public String getRepoOption() {
		return repoOption;
	}

	public String getLocalProjectPath() {
		return localProjectPath;
	}

	public String getGitProjectPath() {
		return gitProjectPath;
	}

	public String getGitProjectBranch() {
		return gitProjectBranch;
	}
	
	public String getGitProjectRootPath() {
		return gitProjectRootPath;
	}

	public String getCredentialsId() {
		return credentialsId;
	}
	
	public String getSrcRootPath() {
		return srcRootPath;
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
		
		FilePath resolvedPath = workspace;
		
		// 내보낸 프로젝트 임시경로로 복사해오기
		if (repoOption.equals(LOCAL_REPO)) {
			resolvedPath = manager.copyProject(localProjectPath);
			
		} else if (repoOption.equals(GIT_REPO)) {
			GitInfo gitInfo = new GitInfo(gitProjectPath, gitProjectRootPath, gitProjectBranch, credentialsId);
			resolvedPath = manager.copyProject(gitInfo, run, env, listener);
		}

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
		CTGeneralCommand generalProjectCommand = new CTGeneralCommand(pathProvider, resolvedPath, mappingFilePath);
		
		// import
		if (startCommand(generalProjectCommand.importCommand(), env, launcher, listener) != Result.SUCCESS.ordinal) {
			run.setResult(Result.FAILURE);
			throw new AbortException("Import Fail");
		}
		
		// 최신 소스코드 반영
		listener.getLogger().println("Update Source Code from SCM");
		manager.updateSourceCode(srcRootPath);
		
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
	// 보고서, ini 파일 아티팩트로 추가하는 부분만 우선 구현
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
            	return FormValidation.error("Enter Your Local Project Path");
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
		public FormValidation doCheckGitProjectRootPath(@QueryParameter String gitProjectRootPath) throws IOException, ServletException {
            if (gitProjectRootPath.isEmpty()) {
            	return FormValidation.error("Enter Your Git Project Root Path");
            }
            return FormValidation.ok();
        }
		
		@RequirePOST
		public FormValidation doCheckSrcRootPath(@QueryParameter String srcRootPath) throws IOException, ServletException {
            if (srcRootPath.isEmpty()) {
            	return FormValidation.error("Enter Your Source Code Root Path from SCM");
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
