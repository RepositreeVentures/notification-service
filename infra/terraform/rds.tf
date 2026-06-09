resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db"
  subnet_ids = local.private_subnet_ids
}

resource "aws_db_instance" "main" {
  identifier = "${local.name_prefix}-pg"

  engine         = "postgres"
  engine_version = "16"
  instance_class = var.db_instance_class

  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_allocated_storage * 2
  storage_encrypted     = true

  db_name  = "notification_service"
  username = "notif_svc"
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  multi_az               = var.db_multi_az
  publicly_accessible    = false
  deletion_protection    = var.environment == "production"
  skip_final_snapshot    = var.environment != "production"
  copy_tags_to_snapshot  = true

  backup_retention_period = var.environment == "production" ? 7 : 1
  backup_window           = "02:00-03:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  performance_insights_enabled = true

  tags = { Name = "${local.name_prefix}-pg" }
}
