package com.codescroll.plugins.controllertester.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;

import com.codescroll.plugins.controllertester.util.PathProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.codescroll.plugins.controllertester.util.GitInfo;
import com.codescroll.plugins.controllertester.util.KeyConstant;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ProjectManager {

	public record ResourceInfo(String resourcePath, FilePath targetPath, Map<String, String> keyValue) { }
	
	private static final String GIT_EXE = "git.exe";
	private static final String ORIGIN = "origin";
	
	private PathProvider pathProvider;
	
	public ProjectManager(PathProvider pathProvider) {
		this.pathProvider = pathProvider;
	}
	
	// 워크스페이스 정리
	public void initWorkspace() {
		
		// ctdata, ctworkspace 폴더 삭제 (global, ini, report, 임시프로젝트 경로 포함)
		
		ArrayList<FilePath> pathToDelete = new ArrayList<>();
		pathToDelete.add(pathProvider.getCtDataPath());
		pathToDelete.add(pathProvider.getCtWorkspacePath());
		
		deleteDir(pathToDelete);
		
		ArrayList<FilePath> pathToCreate = new ArrayList<>();
		pathToCreate.add(pathProvider.getFloatingLicensePath());
		pathToCreate.add(pathProvider.getIniPath());
		pathToCreate.add(pathProvider.getReportPath());
		pathToCreate.add(pathProvider.getTempProjectPath());
		pathToCreate.add(pathProvider.getCtWorkspacePath());
		
		// ctdata, ctworkspace 생성
		makeDir(pathToCreate);
		
	}
	
	// 로컬에서 임시 디렉터리로 재귀복사
	public FilePath copyProject(String localProjectPath) throws IOException, InterruptedException {
		
		FilePath resolvedLocalProjectPath = new FilePath(new File(localProjectPath));
		FilePath tempPath = pathProvider.getTempProjectPath();
		
		resolvedLocalProjectPath.copyRecursiveTo(tempPath);
		return resolvedLocalProjectPath;
	}
	
	// git에서 임시 디렉터리로 클론
	public FilePath copyProject(GitInfo gitInfo, Run<?, ?> run, EnvVars env, TaskListener listener) throws IOException, InterruptedException {
		
		StandardUsernameCredentials credential = CredentialsProvider.findCredentialById(
	            gitInfo.credentialsId(),
	            StandardUsernameCredentials.class,
	            run,
	            URIRequirementBuilder.fromUri(gitInfo.gitrepo()).build());
		
		if (credential == null || !GitClient.CREDENTIALS_MATCHER.matches(credential)) {
			run.setResult(Result.FAILURE);
			throw new AbortException("No Credential Specified");
		}
		
		String revName = String.join(PathProvider.SEPARATOR, ORIGIN, gitInfo.branch());
		
		FilePath tempPath = pathProvider.getTempProjectPath();
		
		Git git = Git.with(listener, env).in(tempPath).using(GIT_EXE);
		GitClient client = git.getClient();
		client.addCredentials(gitInfo.gitrepo(), credential);
		client.setRemoteUrl(ORIGIN, gitInfo.gitrepo());
		client.clone(gitInfo.gitrepo(), ORIGIN, false, null);
		client.checkoutBranch(gitInfo.branch(), client.revParse(revName).getName());
		
		FilePath resolvedGitProjectPath = new FilePath(tempPath, gitInfo.repoRoot());
		listener.getLogger().println(resolvedGitProjectPath);
		
		return resolvedGitProjectPath;
	}
	
	// import 이후에 사용할 소스코드 덮어쓰는 함수
	public void updateSourceCode(String srcRootPath) throws IOException, InterruptedException {
		
		/*
		 * TODO 소스코드 덮어쓰는 로직 고민
		 * 유사도가 높은 두 디렉터리 경로를 찾아서 덮어쓰기
		 * 재귀?
		 * 
		 * 1. 사용자가 입력한 소스코드 루트 경로 하위의 디렉터리 탐색 후 구조 저장
		 * 2. sources 하위의 디렉터리 탐색 후 구조 저장
		 * 3. 탐색해두었던 sources 하위의 경로를 각각 탐색하면서 사용자가 지정한 소스코드 루트 경로 하위 구조와의 유사도 계산
		 * 4. 가장 높은 유사도를 갖는 경로 리턴
		 * 
		 */
		FilePath importedSourcePath = pathProvider.getImportedSourcePath();
		
		// ******임시 코드*******
		importedSourcePath = importedSourcePath.listDirectories().get(0);
		
		FilePath gitSourcePath = new FilePath(pathProvider.getWorkSpace(), srcRootPath);
		if (gitSourcePath.exists()) {
			gitSourcePath.copyRecursiveTo(importedSourcePath);
		}
		
	}
	
	// 디렉토리 재귀 삭제
	public void deleteDir(List<FilePath> dirPath) {
		
		try {
			for (FilePath eachDirPath : dirPath) {
				eachDirPath.deleteRecursive();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		
	}
	
	// 디렉토리 구조 생성
	public void makeDir(List<FilePath> dirPath) {
		
		try {
			for (FilePath eachDirPath : dirPath) {				
				eachDirPath.mkdirs();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		
	}
	
	// 리소스 작성 파트
	public void writeInfo(EnvVars env) {
		writeIniInfo(env);
		writeLicenseInfo(env);
	}
	
	// TODO ini 따로
	private void writeIniInfo(EnvVars env) {
		
		// ini 쓰기
		Map<String, String> iniInfo = new HashMap<>();
		iniInfo.put(KeyConstant.NAME, env.get(KeyConstant.NAME));
		iniInfo.put(KeyConstant.REMOTE_TARGET, env.get(KeyConstant.REMOTE_TARGET));
		iniInfo.put(KeyConstant.OUTPUT_DIR, env.get(KeyConstant.OUTPUT_DIR));
		iniInfo.put(KeyConstant.HTML, env.get(KeyConstant.HTML));
		iniInfo.put(KeyConstant.PDF, env.get(KeyConstant.PDF));
		
		FilePath iniFilePath = new FilePath(pathProvider.getIniPath(), PathProvider.SAMPLE_CLI_INI);
		ResourceInfo iniResourceInfo = new ResourceInfo(PathProvider.INI_RESOURCE_PATH, iniFilePath, iniInfo);
		writeResource(iniResourceInfo);
				
	}
	
	private void writeLicenseInfo(EnvVars env) {
		
		// 라이선스 쓰기
		Map<String, String> licInfo = new HashMap<>();
		licInfo.put(KeyConstant.SERVER_IP, env.get(KeyConstant.SERVER_IP));
		licInfo.put(KeyConstant.SERVER_OS, env.get(KeyConstant.SERVER_OS));
		licInfo.put(KeyConstant.PORT, env.get(KeyConstant.PORT));
		licInfo.put(KeyConstant.LICENSE_TYPE, env.get(KeyConstant.LICENSE_TYPE));

		FilePath licFilePath = new FilePath(pathProvider.getFloatingLicensePath(), PathProvider.CSUT_LIC_PROPERTIES);
		ResourceInfo licResourceInfo = new ResourceInfo(PathProvider.FLOATING_LICENSE_RESOURCE_PATH, licFilePath, licInfo);
		writeResource(licResourceInfo);
		
	}
	
	// 리소스 파일 작성 후 복사
	private void writeResource(ResourceInfo resourceInfo) {
		
		// 리소스 읽어오기
		String resourcePath = resourceInfo.resourcePath();
		
		String line = PathProvider.NO_LENGTH;
		// TODO utf-8
		try (InputStream is = ProjectManager.class.getResourceAsStream(resourcePath)) {
			
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
			
			// TODO 한번에 읽기
			String tempLine;
			while ((tempLine = bufferedReader.readLine()) != null ) {
				line = line.concat(tempLine).concat(System.lineSeparator());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// replace 해서 저장
		Map<String, String> resourceInfoMap = resourceInfo.keyValue();
		try (OutputStream os = resourceInfo.targetPath().write()) {
			
			String replacedLine = line;
			for (Map.Entry<String, String> arg : resourceInfoMap.entrySet()) {
				replacedLine = replacedLine.replace(arg.getKey(), arg.getValue());
			}
			
			os.write(replacedLine.getBytes(StandardCharsets.UTF_8));
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		
	}
}
