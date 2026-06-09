terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
  }

  # Uncomment and configure for remote state before first apply.
  # backend "s3" {
  #   bucket         = "repositree-terraform-state"
  #   key            = "notification-service/terraform.tfstate"
  #   region         = "ap-south-1"
  #   dynamodb_table = "repositree-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Service     = "notification-service"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

locals {
  name_prefix = "repositree-${var.environment}-notification"
}
