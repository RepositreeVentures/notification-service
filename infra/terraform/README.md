# notification-service Terraform

Provisions the AWS infrastructure to run notification-service on ECS Fargate.

## What this creates

| Resource | Notes |
|---|---|
| ECS Fargate cluster + service | Pulls image from GHCR |
| RDS PostgreSQL 16 | `db.t4g.small`, single-AZ (bump for prod) |
| MSK Kafka cluster | `kafka.t3.small`, single-AZ (bump for prod) |
| Application Load Balancer | Internal; fronts the ECS service |
| VPC + subnets + security groups | Or pass existing VPC via vars |
| IAM task execution + task roles | Least-privilege |
| SSM Parameter Store | DB password, Kafka brokers |

## Prerequisites

- Terraform >= 1.7
- AWS credentials with appropriate permissions
- Docker image already pushed to GHCR by CI

## Usage

```bash
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars

terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

## Tear down

```bash
terraform destroy
```

## Variables

See [`variables.tf`](variables.tf) for full list with descriptions.
Minimum required in `terraform.tfvars`:

```hcl
aws_region     = "ap-south-1"
environment    = "staging"
image_tag      = "abc1234"   # git sha from CI
db_password    = "..."       # use a strong password
```
