resource "aws_s3_bucket_notification" "notify_lambda" {
  bucket = aws_s3_bucket.mcp_s3_bucket.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.mcp_s3_bucket_lambda.arn
    events              = ["s3:ObjectCreated:*"]
  }
}

resource "aws_lambda_permission" "allow_s3" {
  statement_id  = "AllowExecutionFromS3"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.mcp_s3_bucket_lambda.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.mcp_s3_bucket.arn
}
