package dev.layseiras.mcp_server.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class Tools {

    private static final Logger logger = Logger.getLogger(Tools.class.getName());

    @Value("${mcp.base-path}")
    private String basePath;

    @Value("${mcp.terraform.templates-path}")
    private String templatesPath;

    @Value("${terraform.binary.path}")
    private String terraformBinaryPath;

    @Tool(name = "get_current_datetime", description = "Get the current date and time in the user's timezone")
    public String getCurrentDatetime() {
        return LocalDateTime.now()
                .atZone(LocaleContextHolder.getTimeZone().toZoneId())
                .toString();
    }

    /* =========================
       TEMPLATE ENGINE DO MCP
       ========================= */
    private void processTemplates(String outputPath, String templateDir, Map<String, String> variables) throws IOException {
        Path templatesRoot = Paths.get(templateDir);

        List<Path> templates = Files.walk(templatesRoot)
                .filter(p -> p.toString().endsWith(".tf.template"))
                .collect(Collectors.toList());

        for (Path templateFile : templates) {
            String content = Files.readString(templateFile);

            for (var entry : variables.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            String relative = templatesRoot.relativize(templateFile).toString();
            String outputFileName = relative.replace(".template", "");

            Path outputFile = Paths.get(outputPath, outputFileName);
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, content);

            logger.info("Generated: " + outputFile);
        }
    }

    /* =========================
       TOOL PRINCIPAL
       ========================= */
    @Tool(name = "create_s3_lambda_aws", description = "Create an AWS S3 bucket with a Lambda triggered on each upload")
    public String createS3LambdaAws(
            @ToolParam(description = "S3 bucket name") String bucketName,
            @ToolParam(description = "Lambda function name") String lambdaFunctionName,
            @ToolParam(description = "Environment (e.g. dev, prod)") String environment,
            @ToolParam(description = "AWS region") String region) throws IOException {

        Map<String, String> variables = new HashMap<>();
        variables.put("aws_region", region);
        variables.put("bucket_name", bucketName);
        variables.put("bucket_resource_name", bucketName.replace("-", "_"));
        variables.put("lambda_function_name", lambdaFunctionName);
        variables.put("lambda_resource_name", lambdaFunctionName.replace("-", "_"));
        variables.put("lambda_role_name", lambdaFunctionName + "_role");
        variables.put("environment", environment);
        variables.put("lambda_zip_path", "lambda.zip");

        String resolvedTemplatesPath = templatesPath + "/aws";
        String terraformPath = basePath + bucketName;

        processTemplates(terraformPath, resolvedTemplatesPath, variables);

        runTerraform(terraformPath, "init");
        runTerraform(terraformPath, "plan");
        runTerraform(terraformPath, "apply", "-auto-approve");

        return "AWS S3 + Lambda provisioned successfully for bucket: " + bucketName;
    }

    /* =========================
       EXECUÇÃO TERRAFORM
       ========================= */
    private void runTerraform(String directory, String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add(terraformBinaryPath);
            command.addAll(List.of(args));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(directory));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(logger::info);
            }

            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("Terraform failed with exit code " + exit);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Terraform execution failed", e);
            throw new RuntimeException(e);
        }
    }

    /* =========================
       DESTRUIR
       ========================= */
    @Tool(name = "delete_s3_lambda_aws", description = "Destroy an AWS S3 + Lambda integration")
    public String deleteS3LambdaAws(
            @ToolParam(description = "S3 bucket name") String bucketName) {

        String terraformPath = basePath + "/terraform/aws/s3-lambda/" + bucketName;

        runTerraform(terraformPath, "destroy", "-auto-approve");

        try {
            Files.walk(Paths.get(terraformPath))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            logger.warning("Terraform destroy OK, but folder cleanup failed: " + e.getMessage());
        }

        return "AWS S3 + Lambda destroyed for bucket: " + bucketName;
    }
}
