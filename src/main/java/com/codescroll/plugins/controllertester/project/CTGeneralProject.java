package com.codescroll.plugins.controllertester.project;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.codescroll.plugins.controllertester.util.PathProvider;

import hudson.FilePath;

public final class CTGeneralProject extends CTProject {
	
	// 프로젝트 이름
	private String projectName = PathProvider.NO_LENGTH;
	
	// 내보낸 프로젝트가 있는 경로
	private FilePath projectPath;
	
	public CTGeneralProject(FilePath projectPath) {
		this.projectPath = projectPath;
	}
	
	public String getProjectName() {
		if (projectName.isEmpty()) {
			projectName = parseJson();
		}
		return projectName;
	}
	
	// 내보낸 프로젝트가 있는 경로에서 json 파일 파싱
	private String parseJson() {
		
		String parsedName = PathProvider.NO_LENGTH;
		
		// json parser
		JSONParser parser = new JSONParser();
		FilePath macroFilePath = new FilePath(projectPath, PathProvider.CTMACRO_JSON);
		
		try (InputStream reader = macroFilePath.read()){
			
			Reader fileReader = new InputStreamReader(reader);
			JSONObject json = (JSONObject) parser.parse(fileReader);
			parsedName = (String) json.get(PROJECT_NAME);
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		} 
		
		return parsedName;
	}
}
