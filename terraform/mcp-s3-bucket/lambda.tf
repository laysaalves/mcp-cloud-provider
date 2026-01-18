resource "aws_lambda_function" "mcp_s3_bucket_lambda" {
  function_name = "mcp-s3-bucket-lambda"
  role          = aws_iam_role.mcp_s3_bucket_lambda_role.arn
  handler       = "handler.lambda_handler"
  runtime       = "python3.11"
  filename      = "lambda.zip"

  source_code_hash = filebase64sha256("lambda.zip")

  timeout = 30
  memory_size = 128

  environment {
    variables = {
      ENV = "dev"
    }
  }
}
