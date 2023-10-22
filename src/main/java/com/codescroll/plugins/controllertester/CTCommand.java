package com.codescroll.plugins.controllertester;

import java.io.IOException;

import hudson.util.ArgumentListBuilder;

// interface 추가
public abstract class CTCommand {
	
	public static final String CMD_CMD_EXE = "cmd.exe";
	public static final String CMD_C = "/C";
	public static final String CMD_G = "-g";
	public static final String CMD_E = "-e";
	public static final String CMD_W = "-w";
	public static final String CMD_O = "-O";
	public static final String CMD_INI = "--ini";
	public static final String CMD_PATH = "--path";
	public static final String CMD_IMPORT = "--import";
	public static final String CMD_CSC_EXE= "csc.exe";
	public static final String CMD_INCLUDE_SRC = "--include-src";
	public static final String CMD_INCLUDE_TCH = "--include-tch";
	public static final String CMD_MAPPING_FILE = "--mapping-file";
	
	public abstract ArgumentListBuilder importCommand() throws IOException, InterruptedException;
	public abstract ArgumentListBuilder executeCommand() throws IOException, InterruptedException;
}
