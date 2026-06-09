resource "aws_msk_cluster" "main" {
  cluster_name           = "${local.name_prefix}-kafka"
  kafka_version          = var.msk_kafka_version
  number_of_broker_nodes = var.msk_broker_count

  broker_node_group_info {
    instance_type  = var.msk_broker_instance_type
    client_subnets = slice(local.private_subnet_ids, 0, var.msk_broker_count)

    storage_info {
      ebs_storage_info {
        volume_size = 100
      }
    }

    security_groups = [aws_security_group.msk.id]
  }

  client_authentication {
    sasl {
      iam = true
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  configuration_info {
    arn      = aws_msk_configuration.main.arn
    revision = aws_msk_configuration.main.latest_revision
  }

  tags = { Name = "${local.name_prefix}-kafka" }
}

resource "aws_msk_configuration" "main" {
  name = "${local.name_prefix}-kafka-config"

  kafka_versions = [var.msk_kafka_version]

  server_properties = <<-EOT
    auto.create.topics.enable=true
    default.replication.factor=1
    min.insync.replicas=1
    num.partitions=6
    log.retention.hours=168
  EOT
}
