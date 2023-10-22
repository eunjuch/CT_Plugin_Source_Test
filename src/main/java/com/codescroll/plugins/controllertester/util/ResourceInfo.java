package com.codescroll.plugins.controllertester.util;

import java.util.Map;

import hudson.FilePath;

// TODO manager 내부로
public record ResourceInfo(String resourcePath, FilePath targetPath, Map<String, String> keyValue) {
	
}
