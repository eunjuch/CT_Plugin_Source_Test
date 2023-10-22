package com.codescroll.plugins.controllertester.util;

import java.io.IOException;

import hudson.FilePath;

public class PathProvider {
	public static final String NO_LENGTH = "";
	public static final String BLANK = " ";
	public static final String SINGLE_QUOTE = "\'";
	public static final String SEPARATOR = "/";
	public static final String REV_SEPARATOR = "\\";
	
	public static final String CTDATA = "ctdata";
	public static final String GLOBAL = "global";
	public static final String CODESCROLL = "CodeScroll";
	
	public static final String LICENSE = "license";
	public static final String CSUT_LIC_PROPERTIES = "csut_lic.properties";
	public static final String CT_LIC = "ct.lic";
	
	public static final String ONE_ONE = "1.1";
	public static final String IMPORTED = "imported";
	public static final String SOURCES = "sources";
	
	public static final String INI = "ini";
	public static final String SAMPLE_CLI_INI = "sample_cli.ini";
	
	public static final String REPORT = "report";
	public static final String TESTREPORT_FILE = "TestReport_*.*";
	
	public static final String CTWORKSPACE = "ctworkspace";
	
	public static final String TEMPPROJECT = "tempproject";
	
	public static final String CTMACRO_JSON = "ctmacro.json";
	
	public static final String INI_RESOURCE_PATH = "/ini/sample_cli.ini";
	public static final String FLOATING_LICENSE_RESOURCE_PATH = "/lic/csut_lic.properties";
	
	private FilePath workspace;
	
	public PathProvider(FilePath workspace) {
		this.workspace = workspace;
	}
	
	public FilePath getWorkSpace() {
		return workspace;
	}
	
	public FilePath getGlobalPath() {
		return new FilePath(workspace, String.join(SEPARATOR, CTDATA, GLOBAL));
	}
	
	public FilePath getFloatingLicensePath() {
		return new FilePath(workspace, String.join(SEPARATOR, CTDATA, GLOBAL, CODESCROLL, LICENSE));
	}
	
	public FilePath getImportedSourcePath() throws IOException, InterruptedException {
		FilePath importPath = new FilePath(workspace, String.join(SEPARATOR, CTDATA, GLOBAL, CODESCROLL, ONE_ONE, IMPORTED));
		return new FilePath(importPath.listDirectories().get(0), SOURCES);
	}
	
	public FilePath getIniPath() {
		return new FilePath(workspace, String.join(SEPARATOR, CTDATA, INI));
	}
	
	public FilePath getReportPath() {
		return new FilePath(workspace, String.join(SEPARATOR, CTDATA, REPORT));
	}
	
	public FilePath getTempProjectPath() {
		return new FilePath(workspace, String.join(SEPARATOR, CTDATA, TEMPPROJECT));
	}
	
	public FilePath getCtDataPath() {
		return new FilePath(workspace, CTDATA);
	}
	
	public FilePath getCtWorkspacePath() {
		return new FilePath(workspace, CTWORKSPACE);
	}
}
