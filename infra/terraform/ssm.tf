resource "aws_ssm_parameter" "db_password" {
  name  = "/${local.name_prefix}/db-password"
  type  = "SecureString"
  value = var.db_password

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "db_url" {
  name  = "/${local.name_prefix}/db-url"
  type  = "String"
  value = "jdbc:postgresql://${aws_db_instance.main.endpoint}/notification_service"
}

resource "aws_ssm_parameter" "kafka_brokers" {
  name  = "/${local.name_prefix}/kafka-brokers"
  type  = "String"
  value = aws_msk_cluster.main.bootstrap_brokers_sasl_iam
}
