######################################
# Provider
######################################

variable "aws_access_key" {
  description = "AWS Access Key"
  type        = string
  sensitive   = true
}

variable "aws_secret_key" {
  description = "AWS Secret Key"
  type        = string
  sensitive   = true
}

variable "aws_region" {
  description = "AWS Region"
  type        = string
}

######################################
# S3
######################################

variable "aws_s3_bucket_name" {
  description = "Nome do bucket S3"
  type        = string
}

variable "env_for_aws_s3_bucket" {
  description = "Ambiente do bucket (dev, stage, prod, etc)"
  type        = string
}

variable "aws_s3_bucket_notification_name" {
  description = "Nome lógico do recurso de notificação do bucket"
  type        = string
}

######################################
# Lambda - Identidade
######################################

variable "lambda_function_name" {
  description = "Nome da função Lambda"
  type        = string
}

variable "lambda_iam_role_name" {
  description = "Nome da IAM Role da Lambda"
  type        = string
}

variable "lambda_policy_name" {
  description = "Nome da IAM Policy da Lambda"
  type        = string
}

variable "lambda_policy_attachment_name" {
  description = "Nome do attachment da policy na role"
  type        = string
}

variable "lambda_log_group_name" {
  description = "Nome lógico do log group"
  type        = string
}

######################################
# Lambda - Configuração
######################################

variable "lambda_runtime" {
  description = "Runtime da Lambda"
  type        = string
  default     = "provided.al2"
}

variable "lambda_handler" {
  description = "Handler da Lambda"
  type        = string
  default     = "bootstrap"
}

variable "lambda_filename" {
  description = "Nome do arquivo zip da Lambda"
  type        = string
  default     = "lambda.zip"
}

variable "lambda_memory_size" {
  description = "Memória da Lambda"
  type        = number
  default     = 1024
}

variable "lambda_timeout" {
  description = "Timeout da Lambda em segundos"
  type        = number
  default     = 900
}

######################################
# Lambda - Environment Variables
######################################

variable "dynamo_table" {
  description = "Nome da tabela DynamoDB"
  type        = string
}

variable "workers" {
  description = "Quantidade de workers"
  type        = string
}

variable "batch_size" {
  description = "Batch size de processamento"
  type        = string
}

variable "buffer_size" {
  description = "Buffer size"
  type        = string
}

######################################
# Logs
######################################

variable "log_retention_days" {
  description = "Dias de retenção dos logs"
  type        = number
  default     = 14
}
