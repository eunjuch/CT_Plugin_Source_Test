package com.codescroll.plugins.controllertester.util;

public enum License {
	NONE("0"),
	FLOATING("1"),
	NODELOCKED("2");
	
	private String licenseNumber;
	
	private License(String licenseNumber) {
		this.licenseNumber = licenseNumber;
	}
	
	public String getLicenseNumber() {
		return licenseNumber;
		
		
	}
}
