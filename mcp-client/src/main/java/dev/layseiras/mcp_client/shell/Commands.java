package dev.layseiras.mcp_client.shell;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

@ShellComponent
public class Commands {
    private final ChatClient chatClient;
    private final ToolCallbackProvider tools;
    ListToolsResult toolsResult;

    public Commands(ChatClient.Builder builder, ToolCallbackProvider tools) {
        this.tools = tools;
        this.chatClient = builder
                .defaultTools(tools)
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
        System.out.println(tools);
    }

    @ShellMethod(key = "chat")
    public String interactiveChat(@ShellOption(defaultValue = "Hi I am the MCP Client") String prompt) {
        try {
            return this.chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            System.err.println("Failed to access MCP tools: " + e.getMessage());
            return "I was unable to access the MCP Server right now. Please try again in a few moments.";
        }
    }
}
