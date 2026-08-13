package com.sohan.codedocs;

import com.sohan.codedocs.config.properties.GeminiProperties;
import com.sohan.codedocs.config.properties.IngestionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GeminiProperties.class, IngestionProperties.class})
public class CodedocsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodedocsApplication.class, args);
	}

}
