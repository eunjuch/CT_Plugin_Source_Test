package com.codescroll.plugins.controllertester;

import java.io.IOException;

import com.codescroll.plugins.controllertester.util.PathProvider;

import hudson.FilePath;
import hudson.util.ArgumentListBuilder;

public class CTGeneralCommand extends CTCommand {
	
	private PathProvider pathProvider;
	private FilePath projectPath;
	private String mappingFilePath;
	
	protected CTGeneralCommand(PathProvider pathProvider, FilePath projectPath, String mappingFilePath) {
		this.pathProvider = pathProvider;
		this.projectPath = projectPath;
		this.mappingFilePath = mappingFilePath;
	}
	
	public PathProvider getPathProvider() {
		return pathProvider;
	}

	public FilePath getProjectPath() {
		return projectPath;
	}

	public String getMappingFilePath() {
		return mappingFilePath;
	}

	private ArgumentListBuilder baseCommand() {
		ArgumentListBuilder cmd = new ArgumentListBuilder(CMD_CMD_EXE, CMD_C);
		
		cmd.add(CMD_CSC_EXE, CMD_G);
		cmd.addQuoted(pathProvider.getGlobalPath().getRemote());
		cmd.add(CMD_E, CMD_W);
		cmd.addQuoted(pathProvider.getCtWorkspacePath().getRemote());
		
		return cmd;
	}
	
	@Override
	public ArgumentListBuilder importCommand() {
		ArgumentListBuilder cmd = baseCommand();
		cmd.add(CMD_IMPORT, CMD_O);
		
		String pathOption = String.join(PathProvider.NO_LENGTH, PathProvider.SINGLE_QUOTE, projectPath.getRemote(),
				PathProvider.SINGLE_QUOTE);
		String finalOption = String.join(PathProvider.BLANK, CMD_PATH, pathOption, CMD_INCLUDE_SRC, CMD_INCLUDE_TCH);
		/*
		 * mapping file option
		 * 
		 * if (mappingFilePath.isEmpty()) { option += String.join(" ",
		 * CommandConstant.CMD_MAPPING_FILE, CommandConstant.CMD_SINGLE_QUOTE,
		 * mappingFilePath, CommandConstant.CMD_SINGLE_QUOTE); }
		 */
		cmd.addQuoted(finalOption);
		
		return cmd;
	}

	@Override
	public ArgumentListBuilder executeCommand() throws IOException, InterruptedException {
		ArgumentListBuilder cmd = baseCommand();
		
		cmd.add(CMD_INI, CMD_O);
		cmd.addQuoted(String.join(PathProvider.SEPARATOR, pathProvider.getIniPath().getRemote(), PathProvider.SAMPLE_CLI_INI));
		
		// TODO --result-path
		
		return cmd;
	}

}
