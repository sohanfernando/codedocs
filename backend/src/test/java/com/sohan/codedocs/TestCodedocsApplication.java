package com.sohan.codedocs;

import org.springframework.boot.SpringApplication;

public class TestCodedocsApplication {

	public static void main(String[] args) {
		SpringApplication.from(CodedocsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
