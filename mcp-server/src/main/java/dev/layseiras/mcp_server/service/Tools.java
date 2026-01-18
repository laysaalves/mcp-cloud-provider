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
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    /*
     * =========================
     * TEMPLATE ENGINE DO MCP
     * =========================
     */
    private void processTemplates(String targetPath, String templatesPath, Map<String, String> variables)
            throws IOException {

        Files.createDirectories(Paths.get(targetPath));

        try (Stream<Path> paths = Files.walk(Paths.get(templatesPath))) {
            paths.filter(Files::isRegularFile).forEach(templateFile -> {
                try {
                    String content = Files.readString(templateFile);

                    for (Map.Entry<String, String> entry : variables.entrySet()) {
                        content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
                    }

                    String fileName = templateFile.getFileName().toString();

                    if (fileName.endsWith(".template")) {
                        fileName = fileName.replace(".template", "");
                    }

                    Path outputFile = Paths.get(targetPath, fileName);
                    Files.writeString(outputFile, content);

                    logger.info("Generated: " + outputFile);
                } catch (IOException e) {
                    throw new RuntimeException("Erro ao processar template: " + templateFile, e);
                }
            });
        }
    }

    /*
     * =========================
     * TOOL PRINCIPAL
     * =========================
     */
    @Tool(name = "create_s3_lambda_aws", description = "Create an AWS S3 bucket with a Lambda triggered on each upload")
    public String createS3LambdaAws(
            @ToolParam(description = "S3 bucket name") String bucketName,
            @ToolParam(description = "Lambda function name") String lambdaFunctionName,
            @ToolParam(description = "Environment (e.g. dev, prod)") String environment,
            @ToolParam(description = "AWS region") String region) {

        Map<String, String> variables = new HashMap<>();
        variables.put("aws_region", region);
        variables.put("bucket_name", bucketName);
        variables.put("bucket_resource_name", bucketName.replace("-", "_"));
        variables.put("lambda_function_name", lambdaFunctionName);
        variables.put("lambda_resource_name", lambdaFunctionName.replace("-", "_"));
        variables.put("lambda_role_name", lambdaFunctionName + "-role");
        variables.put("lambda_role_resource_name", (lambdaFunctionName + "_role").replace("-", "_"));
        variables.put("lambda_basic_policy_attach",
                lambdaFunctionName.replace("-", "_") + "_basic_attach");
        variables.put("environment", environment);
        variables.put("lambda_zip_path", "lambda.zip");

        String resolvedTemplatesPath = templatesPath + "/aws";
        String terraformPath = basePath + bucketName;

        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting provisioning for bucket: " + bucketName);

                processTemplates(terraformPath, resolvedTemplatesPath, variables);
                writeTfVars(terraformPath, variables);

                createLambdaHandler(terraformPath);
                zipLambda(terraformPath);

                runTerraform(terraformPath, "init");
                runTerraform(terraformPath, "plan", "-input=false");
                runTerraform(terraformPath, "apply", "-auto-approve", "-input=false");

                logger.info("✅ Provisioning completed for bucket: " + bucketName);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "❌ Provisioning failed for bucket: " + bucketName, e);
            }
        });

        return "Provisioning started for bucket: " + bucketName +
                ". You can follow progress in the MCP Server logs.";
    }

        /*
     * =================================
     * CRIACÃO DO ARQUIVO .TF PARA VARS
     * =================================
     */
    private void writeTfVars(String targetPath, Map<String, String> variables) throws IOException {
        String tfvars = String.format("""
                aws_region = "%s"
                bucket_name = "%s"
                environment = "%s"
                lambda_function_name = "%s"
                lambda_zip_path = "%s"
                """,
                variables.get("aws_region"),
                variables.get("bucket_name"),
                variables.get("environment"),
                variables.get("lambda_function_name"),
                variables.get("lambda_zip_path"));

        Files.writeString(Paths.get(targetPath, "terraform.tfvars"), tfvars);
    }

    /*
     * =========================
     * EXECUÇÃO TERRAFORM
     * =========================
     */
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

    /*
     * =========================
     * DESTRUIR
     * =========================
     */
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

        /*
     * =========================
     * HANDLER PARA LAMBDA
     * =========================
     */
    private void createLambdaHandler(String terraformPath) throws IOException {
        String handlerCode = """
                import json

                def lambda_handler(event, context):
                    print("Evento recebido do S3:")
                    print(json.dumps(event))
                    return {
                        "statusCode": 200,
                        "body": "Arquivo processado com sucesso!"
                    }
                """;

        Path handlerPath = Paths.get(terraformPath, "handler.py");
        Files.writeString(handlerPath, handlerCode);
        logger.info("Lambda handler criado em: " + handlerPath);
    }

        /*
     * =========================
     * METODO ZIP DA LAMBDA
     * =========================
     */

    private void zipLambda(String terraformPath) throws IOException {
        Path zipPath = Paths.get(terraformPath, "lambda.zip");
        Path handlerPath = Paths.get(terraformPath, "handler.py");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry("handler.py"));
            Files.copy(handlerPath, zos);
            zos.closeEntry();
        }

        logger.info("Lambda zip criado em: " + zipPath);
    }
}
