package com.codescroll.plugins.controllertester;

import java.io.File;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.codescroll.plugins.controllertester.util.KeyConstant;
import com.codescroll.plugins.controllertester.util.License;
import com.codescroll.plugins.controllertester.util.PathProvider;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONObject;

public class CTBuildWrapper extends SimpleBuildWrapper {
	
	// Installed CT Path
	private String ctPath;
	
	// License Properties 
	private String licenseOption;
	private String licenseFilePath;
	
	private String serverOs;
	private String serverIp;
	private String port;
	
	@DataBoundConstructor
	public CTBuildWrapper(String ctPath, String licenseOption, String licenseFilePath, String serverOs, String serverIp,
			String port) {
		this.ctPath = ctPath;
		this.licenseOption = licenseOption;
		this.licenseFilePath = Util.fixNull(licenseFilePath);
		this.serverOs = Util.fixNull(serverOs);
		this.serverIp = Util.fixNull(serverIp);
		this.port = Util.fixNull(port);

		DescriptorImpl globalDescriptor = (DescriptorImpl) getDescriptor();

		globalDescriptor.setGlobal(ctPath, licenseOption, licenseFilePath, serverOs, serverIp, port);
	}
	
	public String getCtPath() {
		return ctPath;
	}
	
	public String getLicenseOption() {
		return licenseOption;
	}

	public String getLicenseFilePath() {
		return licenseFilePath;
	}

	public String getServerOs() {
		return serverOs;
	}

	public String getServerIp() {
		return serverIp;
	}

	public String getPort() {
		return port;
	}

	@Override
	public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener,
			EnvVars initialEnvironment) throws IOException, InterruptedException {
		
		DescriptorImpl globalDescriptor = (DescriptorImpl) getDescriptor();
		
		context.env(KeyConstant.CTPATH, globalDescriptor.getGlobalCtPath());
		context.env(KeyConstant.SERVER_OS, globalDescriptor.getGlobalServerOs());
		context.env(KeyConstant.SERVER_IP, globalDescriptor.getGlobalServerIp());
		context.env(KeyConstant.PORT, globalDescriptor.getGlobalPort());
		
		String type;
		
		// keyconstant - enum 하나로
		if (licenseOption.equals(KeyConstant.FLOATING)) {
			
			// TODO ordinal 제외
			type = String.valueOf(License.FLOATING.ordinal());
			
		} else if (licenseOption.equals(KeyConstant.NODELOCKED)) {
			
			copyLicense();
			type = String.valueOf(License.NODELOCKED.ordinal());
			
		} else {
			
			type = String.valueOf(License.NONE.ordinal());
			
		}
		
		context.env(KeyConstant.LICENSE_TYPE, type);
		
	}
	
	// TODO admin이 아닌 경우 테스트
	private void copyLicense() {
		
		FilePath ctInstalledLicensePath = getLicPath(ctPath);
		FilePath localLicenseFilePath = getLicPath(licenseFilePath);
		
		try {
			localLicenseFilePath.copyTo(ctInstalledLicensePath);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		
	}
	
	private FilePath getLicPath(String rootPathString) {
		
		FilePath rootPath = new FilePath(new File(rootPathString));
		return new FilePath(rootPath, PathProvider.CT_LIC);
		
	}
	
	@Extension
	public static final class DescriptorImpl extends BuildWrapperDescriptor {

		// Installed CT Path
		private String globalCtPath;
		
		// License Properties 
		private String globalLicenseOption;
		private String globalLicenseFilePath;
		private String globalServerOs;
		private String globalServerIp;
		private String globalPort;

		public DescriptorImpl() {
			load();
		}

		public void setGlobal(String globalCtPath, String globalLicenseOption, String globalLicenseFilePath, String globalServerOs, String globalServerIp, String globalPort) {
			this.globalCtPath = globalCtPath;
			this.globalLicenseOption = globalLicenseOption;
			this.globalLicenseFilePath = globalLicenseFilePath;
			this.globalServerOs = globalServerOs;
			this.globalServerIp = globalServerIp;
			this.globalPort = globalPort;
			save();
		}

		public String getGlobalCtPath() {
			return globalCtPath;
		}
		
		public String getGlobalLicenseOption() {
			return globalLicenseOption;
		}
		
		public String getGlobalLicenseFilePath() {
			return globalLicenseFilePath;
		}
		
		public String getGlobalServerOs() {
			return globalServerOs;
		}

		public String getGlobalServerIp() {
			return globalServerIp;
		}		

		public String getGlobalPort() {
			return globalPort;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			globalCtPath = json.getString("globalCtPath");
			globalServerIp = json.getString("globalServerIp");
			globalServerOs = json.getString("globalServerOs");
			globalPort = json.getString("globalPort");
			globalLicenseOption= json.getString("globalLicenseOption");
			save();
			return super.configure(req, json);
		}
		
		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}
		
		@Override
		public String getDisplayName() {
			return "Controller Tester";
		}
	}
}
