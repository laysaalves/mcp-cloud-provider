package dev.layseiras.mcp_server.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class Tools {
    private static final Logger logger = Logger.getLogger(Tools.class.getName());

    @Value("${mcp.base-path}")
    private String basePath;

    @Value("${mcp.terraform.templates-path}")
    private String templatesPath;

    @Value("${mcp.compartments-file}")
    private String compartmentsFilePath;

    @Value("${terraform.binary.path}")
    private String terraformBinaryPath;

    @Tool(name = "get_current_datetime", description = "Get the current date and time in the user's timezone")
    public String getCurrentDatetime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }

    private void processTemplates(String outputPath, String templatesPath, Map<String, String> variables) throws IOException {
        Map<String, String> templates = Map.of(
                "terraform.tfvars", "terraform.tfvars.template",
                "compute.tf", "compute.tf.template"
        );

        for (var entry : templates.entrySet()) {
            String outputFile = outputPath + "/" + entry.getKey();
            String templateFile = templatesPath + entry.getValue();
            String content = Files.readString(Paths.get(templateFile));

            for (var variable : variables.entrySet()) {
                content = content.replace("{{" + variable.getKey() + "}}", variable.getValue());
            }

            Files.createDirectories(Paths.get(outputFile).getParent());
            Files.writeString(Paths.get(outputFile), content);
        }

        List<String> otherFiles = List.of("variables.tf", "provider.tf", "versions.tf");
        for (String file : otherFiles) {
            copyFile(templatesPath + file, outputPath + "/" + file);
        }
    }

    @Tool(name = "create_s3_lambda_aws", description = "Create an AWS S3 bucket with a Lambda triggered on each upload")
    public void createS3LambdaAws(
            @ToolParam(description = "S3 bucket name") String bucketName,
            @ToolParam(description = "Lambda function name") String lambdaName,
            @ToolParam(description = "Lambda handler (e.g. index.handler)") String handler,
            @ToolParam(description = "Lambda runtime (e.g. nodejs18.x, python3.11)") String runtime,
            @ToolParam(description = "AWS region") String region) throws IOException {

        Map<String, String> variables = new HashMap<>();
        variables.put("aws_region", region);
        variables.put("bucket_name", bucketName);
        variables.put("lambda_name", lambdaName);
        variables.put("lambda_handler", handler);
        variables.put("lambda_runtime", runtime);

        String resolvedTemplatesPath = templatesPath + "aws/s3-lambda/";
        String terraformPath = basePath + "/Terraform/aws/s3-lambda/" + bucketName;

        processTemplates(terraformPath, resolvedTemplatesPath, variables);
        runTerraformCommand(terraformPath, List.of(terraformBinaryPath, "init"));
        runTerraformCommand(terraformPath, List.of(terraformBinaryPath, "plan"));
        runTerraformCommand(terraformPath, List.of(terraformBinaryPath, "apply", "-auto-approve"));
    }

    private boolean runTerraformCommand(String directory, List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(directory));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(logger::info);
            }

            return process.waitFor() == 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to run terraform command: " + command, e);
            return false;
        }
    }

    private void copyFile(String sourcePath, String destinationPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path destination = Paths.get(destinationPath);
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    @Tool(name = "delete_s3_lambda_aws", description = "Destroy an AWS S3 + Lambda integration")
    public void deleteS3LambdaAws(
            @ToolParam(description = "S3 bucket name") String bucketName) {

        String terraformPath = basePath + "/Terraform/aws/s3-lambda/" + bucketName;

        boolean success = runTerraformCommand(terraformPath, List.of(terraformBinaryPath, "destroy", "-auto-approve"));
        if (success) {
            try {
                Files.walk(Paths.get(terraformPath))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                logger.warning("Terraform destroy success, but folder not deleted: " + e.getMessage());
            }
        }
    }

}
