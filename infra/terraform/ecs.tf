resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_cloudwatch_log_group" "service" {
  name              = "/ecs/${local.name_prefix}"
  retention_in_days = 30
}

resource "aws_ecs_task_definition" "service" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.ecs_task_cpu
  memory                   = var.ecs_task_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name  = "notification-service"
      image = "ghcr.io/repositreeventures/notification-service:${var.image_tag}"

      portMappings = [
        { containerPort = 8080, protocol = "tcp" }
      ]

      environment = [
        { name = "SERVER_PORT", value = "8080" },
        { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
      ]

      secrets = [
        {
          name      = "DB_URL"
          valueFrom = aws_ssm_parameter.db_url.arn
        },
        {
          name      = "DB_USER"
          valueFrom = "arn:aws:ssm:${var.aws_region}:*:parameter/${local.name_prefix}/db-user"
        },
        {
          name      = "DB_PASSWORD"
          valueFrom = aws_ssm_parameter.db_password.arn
        },
        {
          name      = "KAFKA_BROKERS"
          valueFrom = aws_ssm_parameter.kafka_brokers.arn
        },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.service.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -sf http://localhost:8080/actuator/health/liveness || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }

      readonlyRootFilesystem = false
      essential              = true
    }
  ])
}

resource "aws_ecs_service" "main" {
  name            = "${local.name_prefix}-svc"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.service.arn
  desired_count   = var.ecs_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = local.private_subnet_ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.main.arn
    container_name   = "notification-service"
    container_port   = 8080
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_lb_listener.http]

  lifecycle {
    ignore_changes = [task_definition]
  }
}
