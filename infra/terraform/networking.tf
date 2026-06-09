# Uses an existing VPC when vpc_id is provided; otherwise creates a minimal one.
# For production, provide vpc_id + subnet IDs rather than letting Terraform manage the VPC.

locals {
  create_vpc = var.vpc_id == ""

  vpc_id             = local.create_vpc ? aws_vpc.main[0].id : var.vpc_id
  private_subnet_ids = local.create_vpc ? aws_subnet.private[*].id : var.private_subnet_ids
  public_subnet_ids  = local.create_vpc ? aws_subnet.public[*].id : var.public_subnet_ids

  azs = ["${var.aws_region}a", "${var.aws_region}b"]
}

resource "aws_vpc" "main" {
  count = local.create_vpc ? 1 : 0

  cidr_block           = "10.100.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = "${local.name_prefix}-vpc" }
}

resource "aws_subnet" "private" {
  count = local.create_vpc ? 2 : 0

  vpc_id            = aws_vpc.main[0].id
  cidr_block        = cidrsubnet("10.100.0.0/16", 8, count.index)
  availability_zone = local.azs[count.index]

  tags = { Name = "${local.name_prefix}-private-${count.index}" }
}

resource "aws_subnet" "public" {
  count = local.create_vpc ? 2 : 0

  vpc_id                  = aws_vpc.main[0].id
  cidr_block              = cidrsubnet("10.100.0.0/16", 8, count.index + 10)
  availability_zone       = local.azs[count.index]
  map_public_ip_on_launch = true

  tags = { Name = "${local.name_prefix}-public-${count.index}" }
}

resource "aws_internet_gateway" "main" {
  count  = local.create_vpc ? 1 : 0
  vpc_id = aws_vpc.main[0].id
  tags   = { Name = "${local.name_prefix}-igw" }
}

resource "aws_eip" "nat" {
  count  = local.create_vpc ? 1 : 0
  domain = "vpc"
}

resource "aws_nat_gateway" "main" {
  count         = local.create_vpc ? 1 : 0
  allocation_id = aws_eip.nat[0].id
  subnet_id     = aws_subnet.public[0].id
  depends_on    = [aws_internet_gateway.main]
  tags          = { Name = "${local.name_prefix}-nat" }
}

resource "aws_route_table" "public" {
  count  = local.create_vpc ? 1 : 0
  vpc_id = aws_vpc.main[0].id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main[0].id
  }

  tags = { Name = "${local.name_prefix}-public-rt" }
}

resource "aws_route_table_association" "public" {
  count          = local.create_vpc ? 2 : 0
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public[0].id
}

resource "aws_route_table" "private" {
  count  = local.create_vpc ? 1 : 0
  vpc_id = aws_vpc.main[0].id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[0].id
  }

  tags = { Name = "${local.name_prefix}-private-rt" }
}

resource "aws_route_table_association" "private" {
  count          = local.create_vpc ? 2 : 0
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[0].id
}

# --- Security Groups ---

resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb"
  description = "ALB security group"
  vpc_id      = local.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/8"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ecs" {
  name        = "${local.name_prefix}-ecs"
  description = "ECS task security group"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds"
  description = "RDS security group"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }
}

resource "aws_security_group" "msk" {
  name        = "${local.name_prefix}-msk"
  description = "MSK security group"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  ingress {
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }
}
