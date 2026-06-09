data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# Execution role — allows ECS agent to pull images and write logs
resource "aws_iam_role" "execution" {
  name               = "${local.name_prefix}-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "execution_ssm" {
  name = "ssm-read"
  role = aws_iam_role.execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ssm:GetParameters", "ssm:GetParameter"]
      Resource = "arn:aws:ssm:${var.aws_region}:*:parameter/${local.name_prefix}/*"
    }]
  })
}

# Task role — runtime permissions for the application itself
resource "aws_iam_role" "task" {
  name               = "${local.name_prefix}-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy" "task_msk" {
  name = "msk-connect"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "kafka-cluster:Connect",
        "kafka-cluster:DescribeCluster",
        "kafka-cluster:ReadData",
        "kafka-cluster:WriteData",
        "kafka-cluster:DescribeTopic",
        "kafka-cluster:CreateTopic",
        "kafka-cluster:DescribeGroup",
        "kafka-cluster:AlterGroup",
      ]
      Resource = [
        aws_msk_cluster.main.arn,
        "arn:aws:kafka:${var.aws_region}:*:topic/${aws_msk_cluster.main.cluster_name}/*",
        "arn:aws:kafka:${var.aws_region}:*:group/${aws_msk_cluster.main.cluster_name}/*",
      ]
    }]
  })
}
