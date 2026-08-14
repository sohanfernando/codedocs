package com.sohan.codedocs;

import com.sohan.codedocs.config.properties.GeminiProperties;
import com.sohan.codedocs.config.properties.IngestionProperties;
import io.sentry.Sentry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GeminiProperties.class, IngestionProperties.class})
public class CodedocsApplication {

	public static void main(String[] args) {
		initSentry();
		SpringApplication.run(CodedocsApplication.class, args);
	}

	/**
	 * Initialized here, ahead of the Spring context, rather than through
	 * Sentry's own Spring Boot starter — see the comment on the sentry
	 * dependency in pom.xml. A blank SENTRY_DSN (the default outside
	 * production) leaves the SDK disabled: capture calls become no-ops
	 * instead of throwing.
	 *
	 * SENTRY_DSN is read once, when this JVM process starts — a devtools
	 * hot-reload after editing .env will NOT pick up a new value, only a
	 * genuine process restart will.
	 */
	private static void initSentry() {
		Sentry.init(options -> {
			options.setDsn(System.getenv().getOrDefault("SENTRY_DSN", ""));
			options.setEnvironment(System.getenv().getOrDefault("SENTRY_ENVIRONMENT", "local"));
			options.setTracesSampleRate(
					Double.parseDouble(System.getenv().getOrDefault("SENTRY_TRACES_SAMPLE_RATE", "0.1")));
			// This app handles user emails and repo contents — neither belongs
			// in an error tracker by default.
			options.setSendDefaultPii(false);
			options.addInAppInclude("com.sohan.codedocs");
		});
	}

}
