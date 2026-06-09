output "alb_dns_name" {
  description = "ALB DNS name for the notification service"
  value       = aws_lb.main.dns_name
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.main.name
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.main.endpoint
  sensitive   = true
}

output "msk_bootstrap_brokers" {
  description = "MSK bootstrap brokers (SASL/IAM)"
  value       = aws_msk_cluster.main.bootstrap_brokers_sasl_iam
  sensitive   = true
}

output "cloudwatch_log_group" {
  description = "CloudWatch log group for ECS tasks"
  value       = aws_cloudwatch_log_group.service.name
}
