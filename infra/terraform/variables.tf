variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "environment" {
  description = "Deployment environment (staging, production)"
  type        = string
}

variable "image_tag" {
  description = "Docker image tag to deploy (git SHA from CI)"
  type        = string
}

# --- Networking ---

variable "vpc_id" {
  description = "Existing VPC ID. If empty, a new VPC is created."
  type        = string
  default     = ""
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for ECS tasks and RDS. Required when vpc_id is set."
  type        = list(string)
  default     = []
}

variable "public_subnet_ids" {
  description = "Public subnet IDs for the ALB. Required when vpc_id is set."
  type        = list(string)
  default     = []
}

# --- ECS ---

variable "ecs_task_cpu" {
  description = "ECS task CPU units"
  type        = number
  default     = 512
}

variable "ecs_task_memory" {
  description = "ECS task memory (MiB)"
  type        = number
  default     = 1024
}

variable "ecs_desired_count" {
  description = "Number of ECS task instances"
  type        = number
  default     = 1
}

# --- RDS ---

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.small"
}

variable "db_allocated_storage" {
  description = "RDS storage (GiB)"
  type        = number
  default     = 20
}

variable "db_multi_az" {
  description = "Enable RDS Multi-AZ"
  type        = bool
  default     = false
}

variable "db_password" {
  description = "RDS master password (stored in SSM)"
  type        = string
  sensitive   = true
}

# --- MSK ---

variable "msk_broker_instance_type" {
  description = "MSK broker instance type"
  type        = string
  default     = "kafka.t3.small"
}

variable "msk_broker_count" {
  description = "Number of MSK broker nodes (must match AZ count)"
  type        = number
  default     = 1
}

variable "msk_kafka_version" {
  description = "Apache Kafka version for MSK"
  type        = string
  default     = "3.6.0"
}

# --- ALB ---

variable "alb_internal" {
  description = "Make the ALB internal (not internet-facing)"
  type        = bool
  default     = true
}

variable "health_check_path" {
  description = "ALB health check path"
  type        = string
  default     = "/actuator/health/liveness"
}
