resource "aws_s3_bucket" "mcp_s3_bucket" {
  bucket = "mcp-s3-bucket"
  force_destroy = true

  tags = {
    Name        = "mcp-s3-bucket"
    Environment = "dev"
  }
}
