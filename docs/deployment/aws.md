# AWS Deployment Guide

This guide covers deploying InvoiceMe to Amazon Web Services (AWS) using ECS Fargate, RDS PostgreSQL, and Application Load Balancer.

## Architecture Overview

```
Internet
    │
    ▼
┌─────────────────────┐
│  Application Load   │
│     Balancer        │
│   (ALB) - HTTPS     │
└──────┬─────┬────────┘
       │     │
   ┌───▼─────▼───┐
   │   Target    │
   │   Groups    │
   └───┬─────┬───┘
       │     │
┌──────▼─────▼──────┐
│   ECS Service     │
│ ┌──────┬────────┐ │
│ │Front │Backend │ │
│ │ end  │        │ │
│ │Task  │ Task   │ │
│ └──────┴────────┘ │
└────────┬──────────┘
         │
    ┌────▼─────┐
    │   RDS    │
    │PostgreSQL│
    └──────────┘
```

## Prerequisites

- AWS Account with appropriate permissions
- AWS CLI installed and configured
- Docker installed locally
- Domain name (optional, for custom domain)

## Step-by-Step Deployment

### 1. Set Up VPC and Networking

```bash
# Create VPC
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=invoiceme-vpc}]'

# Create public subnets (for ALB)
aws ec2 create-subnet \
  --vpc-id <vpc-id> \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a

aws ec2 create-subnet \
  --vpc-id <vpc-id> \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b

# Create private subnets (for ECS tasks and RDS)
aws ec2 create-subnet \
  --vpc-id <vpc-id> \
  --cidr-block 10.0.10.0/24 \
  --availability-zone us-east-1a

aws ec2 create-subnet \
  --vpc-id <vpc-id> \
  --cidr-block 10.0.11.0/24 \
  --availability-zone us-east-1b

# Create Internet Gateway
aws ec2 create-internet-gateway \
  --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=invoiceme-igw}]'

# Attach to VPC
aws ec2 attach-internet-gateway \
  --vpc-id <vpc-id> \
  --internet-gateway-id <igw-id>
```

### 2. Create Security Groups

```bash
# ALB Security Group
aws ec2 create-security-group \
  --group-name invoiceme-alb-sg \
  --description "Security group for InvoiceMe ALB" \
  --vpc-id <vpc-id>

# Allow HTTPS
aws ec2 authorize-security-group-ingress \
  --group-id <alb-sg-id> \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

# Allow HTTP (redirect to HTTPS)
aws ec2 authorize-security-group-ingress \
  --group-id <alb-sg-id> \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# ECS Tasks Security Group
aws ec2 create-security-group \
  --group-name invoiceme-ecs-sg \
  --description "Security group for InvoiceMe ECS tasks" \
  --vpc-id <vpc-id>

# Allow traffic from ALB
aws ec2 authorize-security-group-ingress \
  --group-id <ecs-sg-id> \
  --protocol tcp \
  --port 3000 \
  --source-group <alb-sg-id>

aws ec2 authorize-security-group-ingress \
  --group-id <ecs-sg-id> \
  --protocol tcp \
  --port 8080 \
  --source-group <alb-sg-id>

# RDS Security Group
aws ec2 create-security-group \
  --group-name invoiceme-rds-sg \
  --description "Security group for InvoiceMe RDS" \
  --vpc-id <vpc-id>

# Allow PostgreSQL from ECS
aws ec2 authorize-security-group-ingress \
  --group-id <rds-sg-id> \
  --protocol tcp \
  --port 5432 \
  --source-group <ecs-sg-id>
```

### 3. Create RDS PostgreSQL Database

```bash
# Create DB subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name invoiceme-db-subnet \
  --db-subnet-group-description "Subnet group for InvoiceMe database" \
  --subnet-ids <private-subnet-1-id> <private-subnet-2-id>

# Create RDS instance
aws rds create-db-instance \
  --db-instance-identifier invoiceme-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username postgres \
  --master-user-password <secure-password> \
  --allocated-storage 20 \
  --db-name invoiceme \
  --vpc-security-group-ids <rds-sg-id> \
  --db-subnet-group-name invoiceme-db-subnet \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --publicly-accessible false \
  --storage-encrypted \
  --enable-cloudwatch-logs-exports '["postgresql"]'

# Get database endpoint
aws rds describe-db-instances \
  --db-instance-identifier invoiceme-db \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

### 4. Create ECR Repositories

```bash
# Create repository for backend
aws ecr create-repository \
  --repository-name invoiceme/backend \
  --image-scanning-configuration scanOnPush=true

# Create repository for frontend
aws ecr create-repository \
  --repository-name invoiceme/frontend \
  --image-scanning-configuration scanOnPush=true

# Get login command
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
```

### 5. Build and Push Docker Images

```bash
# Build images
docker build -t invoiceme-backend:latest ./backend
docker build -t invoiceme-frontend:latest .

# Tag images
docker tag invoiceme-backend:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/invoiceme/backend:latest
docker tag invoiceme-frontend:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/invoiceme/frontend:latest

# Push to ECR
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/invoiceme/backend:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/invoiceme/frontend:latest
```

### 6. Create ECS Cluster

```bash
# Create Fargate cluster
aws ecs create-cluster \
  --cluster-name invoiceme-cluster \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy \
    capacityProvider=FARGATE,weight=1 \
    capacityProvider=FARGATE_SPOT,weight=2
```

### 7. Create Task Definitions

Create `backend-task-definition.json`:

```json
{
  "family": "invoiceme-backend",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<account-id>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "backend",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/invoiceme/backend:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        },
        {
          "name": "DATABASE_URL",
          "value": "jdbc:postgresql://<rds-endpoint>:5432/invoiceme"
        }
      ],
      "secrets": [
        {
          "name": "DATABASE_USERNAME",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:<account-id>:secret:invoiceme/db-username"
        },
        {
          "name": "DATABASE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:<account-id>:secret:invoiceme/db-password"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:<account-id>:secret:invoiceme/jwt-secret"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/invoiceme-backend",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

Create `frontend-task-definition.json`:

```json
{
  "family": "invoiceme-frontend",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::<account-id>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "frontend",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/invoiceme/frontend:latest",
      "portMappings": [
        {
          "containerPort": 3000,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "NEXT_PUBLIC_API_URL",
          "value": "https://api.yourdomain.com/api"
        },
        {
          "name": "NODE_ENV",
          "value": "production"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/invoiceme-frontend",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "node -e \"require('http').get('http://localhost:3000', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})\""],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 30
      }
    }
  ]
}
```

Register task definitions:

```bash
# Create CloudWatch log groups first
aws logs create-log-group --log-group-name /ecs/invoiceme-backend
aws logs create-log-group --log-group-name /ecs/invoiceme-frontend

# Register task definitions
aws ecs register-task-definition --cli-input-json file://backend-task-definition.json
aws ecs register-task-definition --cli-input-json file://frontend-task-definition.json
```

### 8. Create Application Load Balancer

```bash
# Create ALB
aws elbv2 create-load-balancer \
  --name invoiceme-alb \
  --subnets <public-subnet-1-id> <public-subnet-2-id> \
  --security-groups <alb-sg-id> \
  --scheme internet-facing \
  --type application \
  --ip-address-type ipv4

# Create target groups
aws elbv2 create-target-group \
  --name invoiceme-backend-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id <vpc-id> \
  --target-type ip \
  --health-check-path /api/actuator/health \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3

aws elbv2 create-target-group \
  --name invoiceme-frontend-tg \
  --protocol HTTP \
  --port 3000 \
  --vpc-id <vpc-id> \
  --target-type ip \
  --health-check-path / \
  --health-check-interval-seconds 30

# Create listeners (after obtaining SSL certificate)
aws elbv2 create-listener \
  --load-balancer-arn <alb-arn> \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=<certificate-arn> \
  --default-actions Type=forward,TargetGroupArn=<frontend-tg-arn>

# Add rule for backend API
aws elbv2 create-rule \
  --listener-arn <listener-arn> \
  --priority 1 \
  --conditions Field=path-pattern,Values='/api/*' \
  --actions Type=forward,TargetGroupArn=<backend-tg-arn>
```

### 9. Create ECS Services

```bash
# Create backend service
aws ecs create-service \
  --cluster invoiceme-cluster \
  --service-name invoiceme-backend \
  --task-definition invoiceme-backend \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[<private-subnet-1>,<private-subnet-2>],securityGroups=[<ecs-sg-id>],assignPublicIp=DISABLED}" \
  --load-balancers "targetGroupArn=<backend-tg-arn>,containerName=backend,containerPort=8080" \
  --health-check-grace-period-seconds 60 \
  --enable-execute-command

# Create frontend service
aws ecs create-service \
  --cluster invoiceme-cluster \
  --service-name invoiceme-frontend \
  --task-definition invoiceme-frontend \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[<private-subnet-1>,<private-subnet-2>],securityGroups=[<ecs-sg-id>],assignPublicIp=DISABLED}" \
  --load-balancers "targetGroupArn=<frontend-tg-arn>,containerName=frontend,containerPort=3000" \
  --health-check-grace-period-seconds 30 \
  --enable-execute-command
```

### 10. Configure Auto Scaling

```bash
# Register scalable target for backend
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/invoiceme-cluster/invoiceme-backend \
  --min-capacity 2 \
  --max-capacity 10

# Create scaling policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/invoiceme-cluster/invoiceme-backend \
  --policy-name cpu-scaling \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration file://scaling-policy.json
```

## Monitoring and Logging

### CloudWatch Logs

View logs in CloudWatch:
```bash
aws logs tail /ecs/invoiceme-backend --follow
aws logs tail /ecs/invoiceme-frontend --follow
```

### CloudWatch Alarms

```bash
# High CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name invoiceme-backend-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --evaluation-periods 2 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=ServiceName,Value=invoiceme-backend Name=ClusterName,Value=invoiceme-cluster
```

## Costs Estimation

Approximate monthly costs (us-east-1):

- **ECS Fargate** (2 tasks × 2 services): ~$60-100/month
- **RDS db.t3.micro**: ~$15-20/month
- **Application Load Balancer**: ~$20-25/month
- **Data Transfer**: Variable (~$10-50/month)
- **CloudWatch Logs**: ~$5-10/month

**Total**: ~$110-205/month

## Security Best Practices

1. Use AWS Secrets Manager for sensitive data
2. Enable VPC Flow Logs
3. Use IAM roles with least privilege
4. Enable RDS encryption at rest
5. Use SSL/TLS for all connections
6. Enable AWS WAF on ALB
7. Regular security scanning
8. Enable MFA for AWS account

## Backup and Disaster Recovery

1. RDS automated backups (7-day retention)
2. Manual RDS snapshots before major changes
3. Store Docker images in ECR with lifecycle policies
4. Infrastructure as Code (CloudFormation/Terraform)
5. Cross-region replication (optional)

## Updating the Application

```bash
# Build new images
./scripts/build-all.sh

# Tag and push to ECR
docker tag invoiceme-backend:latest <ecr-uri>/invoiceme/backend:v1.1.0
docker push <ecr-uri>/invoiceme/backend:v1.1.0

# Update task definition with new image
# Update ECS service to use new task definition
aws ecs update-service \
  --cluster invoiceme-cluster \
  --service invoiceme-backend \
  --force-new-deployment
```

## Troubleshooting

### Tasks Failing Health Checks

```bash
# Check task logs
aws logs tail /ecs/invoiceme-backend --follow

# Describe tasks
aws ecs describe-tasks \
  --cluster invoiceme-cluster \
  --tasks <task-id>
```

### Database Connection Issues

- Verify security group allows traffic from ECS
- Check RDS endpoint in environment variables
- Verify secrets in Secrets Manager
- Check VPC routing and NACLs

## Next Steps

- Set up CI/CD with GitHub Actions
- Configure custom domain with Route 53
- Enable CloudFront CDN
- Set up monitoring dashboards
- Implement backup strategy
