package dev.layseiras.mcp_server;

import dev.layseiras.mcp_server.service.Tools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerApplication {

	@Autowired
	Tools tools;

	@Bean
	public ToolCallbackProvider utilTools() {
		return MethodToolCallbackProvider.builder().toolObjects(tools).build();
	}

	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}

}
