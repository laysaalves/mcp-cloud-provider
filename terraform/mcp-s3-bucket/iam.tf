resource "aws_iam_role" "mcp_s3_bucket_lambda_role" {
  name = "mcp-s3-bucket-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "mcp_s3_bucket_lambda_basic_attach" {
  role       = aws_iam_role.mcp_s3_bucket_lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}
