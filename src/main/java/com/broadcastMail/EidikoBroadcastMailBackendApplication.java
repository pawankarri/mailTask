package com.broadcastMail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class EidikoBroadcastMailBackendApplication extends SpringBootServletInitializer {


	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(EidikoBroadcastMailBackendApplication.class);
	}


	public static void main(String[] args) {
		SpringApplication.run(EidikoBroadcastMailBackendApplication.class, args);
	}

}
