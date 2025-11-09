# Azure Deployment Guide

This guide covers deploying InvoiceMe to Microsoft Azure using Azure Container Instances, Azure Database for PostgreSQL, and Azure Application Gateway.

## Architecture Overview

```
Internet
    │
    ▼
┌──────────────────────┐
│ Application Gateway  │
│   + WAF (HTTPS)      │
└──────┬───────────────┘
       │
   ┌───▼────────┐
   │   Backend  │
   │    Pool    │
   └───┬────┬───┘
       │    │
┌──────▼────▼──────────┐
│ Container Instances  │
│ ┌────────┬─────────┐ │
│ │Frontend│ Backend │ │
│ │Container│Container│ │
│ └────────┴─────────┘ │
└────────┬─────────────┘
         │
    ┌────▼──────────┐
    │  Azure DB for │
    │  PostgreSQL   │
    └───────────────┘
```

## Prerequisites

- Azure Account with active subscription
- Azure CLI installed and configured
- Docker installed locally
- Domain name (optional)

## Step-by-Step Deployment

### 1. Set Up Resource Group and Networking

```bash
# Login to Azure
az login

# Set variables
RESOURCE_GROUP="invoiceme-rg"
LOCATION="eastus"
VNET_NAME="invoiceme-vnet"
SUBNET_NAME="invoiceme-subnet"

# Create resource group
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION

# Create virtual network
az network vnet create \
  --resource-group $RESOURCE_GROUP \
  --name $VNET_NAME \
  --address-prefix 10.0.0.0/16 \
  --subnet-name $SUBNET_NAME \
  --subnet-prefix 10.0.1.0/24

# Create additional subnets
az network vnet subnet create \
  --resource-group $RESOURCE_GROUP \
  --vnet-name $VNET_NAME \
  --name db-subnet \
  --address-prefix 10.0.2.0/24

az network vnet subnet create \
  --resource-group $RESOURCE_GROUP \
  --vnet-name $VNET_NAME \
  --name appgw-subnet \
  --address-prefix 10.0.3.0/24
```

### 2. Create Azure Database for PostgreSQL

```bash
# Set database variables
DB_SERVER_NAME="invoiceme-db-server"
DB_ADMIN_USER="invoicemeadmin"
DB_ADMIN_PASSWORD="<secure-password>"
DB_NAME="invoiceme"

# Create PostgreSQL server
az postgres flexible-server create \
  --resource-group $RESOURCE_GROUP \
  --name $DB_SERVER_NAME \
  --location $LOCATION \
  --admin-user $DB_ADMIN_USER \
  --admin-password $DB_ADMIN_PASSWORD \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --version 15 \
  --storage-size 32 \
  --backup-retention 7 \
  --high-availability Disabled \
  --public-access None \
  --vnet $VNET_NAME \
  --subnet db-subnet

# Create database
az postgres flexible-server db create \
  --resource-group $RESOURCE_GROUP \
  --server-name $DB_SERVER_NAME \
  --database-name $DB_NAME

# Configure firewall (for development, remove in production)
# Allow Azure services
az postgres flexible-server firewall-rule create \
  --resource-group $RESOURCE_GROUP \
  --name $DB_SERVER_NAME \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

# Get connection string
DB_HOST="${DB_SERVER_NAME}.postgres.database.azure.com"
DB_CONNECTION_STRING="jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}"
echo "Database connection string: $DB_CONNECTION_STRING"
```

### 3. Create Azure Container Registry

```bash
ACR_NAME="invoicemeacr"

# Create ACR
az acr create \
  --resource-group $RESOURCE_GROUP \
  --name $ACR_NAME \
  --sku Basic \
  --admin-enabled true

# Login to ACR
az acr login --name $ACR_NAME

# Get ACR login server
ACR_LOGIN_SERVER=$(az acr show \
  --name $ACR_NAME \
  --query loginServer \
  --output tsv)

echo "ACR Login Server: $ACR_LOGIN_SERVER"
```

### 4. Build and Push Docker Images

```bash
# Build images locally
docker build -t invoiceme-backend:latest ./backend
docker build -t invoiceme-frontend:latest .

# Tag for ACR
docker tag invoiceme-backend:latest ${ACR_LOGIN_SERVER}/invoiceme-backend:latest
docker tag invoiceme-frontend:latest ${ACR_LOGIN_SERVER}/invoiceme-frontend:latest

# Push to ACR
docker push ${ACR_LOGIN_SERVER}/invoiceme-backend:latest
docker push ${ACR_LOGIN_SERVER}/invoiceme-frontend:latest

# Verify images
az acr repository list --name $ACR_NAME --output table
```

### 5. Create Azure Key Vault for Secrets

```bash
KEY_VAULT_NAME="invoiceme-kv-${RANDOM}"

# Create Key Vault
az keyvault create \
  --resource-group $RESOURCE_GROUP \
  --name $KEY_VAULT_NAME \
  --location $LOCATION \
  --enabled-for-deployment true \
  --enabled-for-template-deployment true

# Store secrets
az keyvault secret set \
  --vault-name $KEY_VAULT_NAME \
  --name db-username \
  --value $DB_ADMIN_USER

az keyvault secret set \
  --vault-name $KEY_VAULT_NAME \
  --name db-password \
  --value $DB_ADMIN_PASSWORD

az keyvault secret set \
  --vault-name $KEY_VAULT_NAME \
  --name jwt-secret \
  --value "$(openssl rand -hex 32)"

# Get ACR password
ACR_PASSWORD=$(az acr credential show \
  --name $ACR_NAME \
  --query "passwords[0].value" \
  --output tsv)

az keyvault secret set \
  --vault-name $KEY_VAULT_NAME \
  --name acr-password \
  --value $ACR_PASSWORD
```

### 6. Create Container Instances

#### Backend Container

```bash
# Get secrets
DB_PASSWORD=$(az keyvault secret show \
  --vault-name $KEY_VAULT_NAME \
  --name db-password \
  --query value \
  --output tsv)

JWT_SECRET=$(az keyvault secret show \
  --vault-name $KEY_VAULT_NAME \
  --name jwt-secret \
  --query value \
  --output tsv)

# Create backend container instance
az container create \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-backend \
  --image ${ACR_LOGIN_SERVER}/invoiceme-backend:latest \
  --registry-login-server $ACR_LOGIN_SERVER \
  --registry-username $ACR_NAME \
  --registry-password $ACR_PASSWORD \
  --dns-name-label invoiceme-backend-${RANDOM} \
  --ports 8080 \
  --cpu 1 \
  --memory 2 \
  --vnet $VNET_NAME \
  --subnet $SUBNET_NAME \
  --environment-variables \
    SPRING_PROFILES_ACTIVE=prod \
    DATABASE_URL=$DB_CONNECTION_STRING \
    DATABASE_USERNAME=$DB_ADMIN_USER \
  --secure-environment-variables \
    DATABASE_PASSWORD=$DB_PASSWORD \
    JWT_SECRET=$JWT_SECRET
```

#### Frontend Container

```bash
# Get backend FQDN
BACKEND_FQDN=$(az container show \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-backend \
  --query ipAddress.fqdn \
  --output tsv)

# Create frontend container instance
az container create \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-frontend \
  --image ${ACR_LOGIN_SERVER}/invoiceme-frontend:latest \
  --registry-login-server $ACR_LOGIN_SERVER \
  --registry-username $ACR_NAME \
  --registry-password $ACR_PASSWORD \
  --dns-name-label invoiceme-frontend-${RANDOM} \
  --ports 3000 \
  --cpu 0.5 \
  --memory 1 \
  --vnet $VNET_NAME \
  --subnet $SUBNET_NAME \
  --environment-variables \
    NEXT_PUBLIC_API_URL=https://${BACKEND_FQDN}/api \
    NODE_ENV=production
```

### 7. Create Application Gateway (Optional but Recommended)

```bash
# Create public IP
az network public-ip create \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-appgw-pip \
  --allocation-method Static \
  --sku Standard

# Create Application Gateway
az network application-gateway create \
  --name invoiceme-appgw \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku Standard_v2 \
  --capacity 2 \
  --vnet-name $VNET_NAME \
  --subnet appgw-subnet \
  --public-ip-address invoiceme-appgw-pip \
  --http-settings-cookie-based-affinity Disabled \
  --http-settings-port 3000 \
  --http-settings-protocol Http \
  --frontend-port 80

# Get container private IPs
FRONTEND_IP=$(az container show \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-frontend \
  --query ipAddress.ip \
  --output tsv)

BACKEND_IP=$(az container show \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-backend \
  --query ipAddress.ip \
  --output tsv)

# Add backend containers to application gateway backend pool
az network application-gateway address-pool create \
  --resource-group $RESOURCE_GROUP \
  --gateway-name invoiceme-appgw \
  --name frontend-pool \
  --servers $FRONTEND_IP

az network application-gateway address-pool create \
  --resource-group $RESOURCE_GROUP \
  --gateway-name invoiceme-appgw \
  --name backend-pool \
  --servers $BACKEND_IP
```

### 8. Configure SSL/TLS (Production)

```bash
# Upload SSL certificate to Key Vault
az keyvault certificate import \
  --vault-name $KEY_VAULT_NAME \
  --name ssl-cert \
  --file /path/to/certificate.pfx \
  --password <cert-password>

# Configure HTTPS listener on Application Gateway
az network application-gateway http-listener create \
  --gateway-name invoiceme-appgw \
  --resource-group $RESOURCE_GROUP \
  --name https-listener \
  --frontend-port 443 \
  --ssl-cert <certificate-name>
```

### 9. Configure Monitoring

```bash
# Create Log Analytics workspace
az monitor log-analytics workspace create \
  --resource-group $RESOURCE_GROUP \
  --workspace-name invoiceme-logs \
  --location $LOCATION

# Get workspace ID
WORKSPACE_ID=$(az monitor log-analytics workspace show \
  --resource-group $RESOURCE_GROUP \
  --workspace-name invoiceme-logs \
  --query customerId \
  --output tsv)

# Get workspace key
WORKSPACE_KEY=$(az monitor log-analytics workspace get-shared-keys \
  --resource-group $RESOURCE_GROUP \
  --workspace-name invoiceme-logs \
  --query primarySharedKey \
  --output tsv)

# Update container instances with logging
az container create \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-backend \
  --image ${ACR_LOGIN_SERVER}/invoiceme-backend:latest \
  --log-analytics-workspace $WORKSPACE_ID \
  --log-analytics-workspace-key $WORKSPACE_KEY \
  # ... other parameters
```

### 10. Set Up Auto-Restart (Container Groups)

For production, consider using Azure Container Apps or Azure Kubernetes Service (AKS) for better orchestration and auto-scaling.

## Alternative: Azure Container Apps (Recommended for Production)

Azure Container Apps provides better scaling, load balancing, and management:

```bash
# Install Container Apps extension
az extension add --name containerapp --upgrade

# Create Container Apps environment
az containerapp env create \
  --name invoiceme-env \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION

# Create backend app
az containerapp create \
  --name invoiceme-backend \
  --resource-group $RESOURCE_GROUP \
  --environment invoiceme-env \
  --image ${ACR_LOGIN_SERVER}/invoiceme-backend:latest \
  --target-port 8080 \
  --ingress external \
  --registry-server $ACR_LOGIN_SERVER \
  --registry-username $ACR_NAME \
  --registry-password $ACR_PASSWORD \
  --cpu 1.0 \
  --memory 2.0Gi \
  --min-replicas 2 \
  --max-replicas 10 \
  --env-vars \
    SPRING_PROFILES_ACTIVE=prod \
    DATABASE_URL=$DB_CONNECTION_STRING \
  --secrets \
    db-password=$DB_PASSWORD \
    jwt-secret=$JWT_SECRET
```

## Monitoring and Logging

### View Container Logs

```bash
# View backend logs
az container logs \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-backend \
  --follow

# View frontend logs
az container logs \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-frontend \
  --follow
```

### Azure Monitor Queries

```kusto
ContainerInstanceLog_CL
| where ContainerGroup_s == "invoiceme-backend"
| order by TimeGenerated desc
| take 100
```

## Cost Estimation

Approximate monthly costs (East US):

- **Container Instances** (2 instances): ~$30-50/month
- **Azure Database for PostgreSQL** (B1ms): ~$25-35/month
- **Application Gateway** (Standard_v2): ~$130-150/month
- **Container Registry** (Basic): ~$5/month
- **Key Vault**: ~$3/month
- **Log Analytics**: ~$10-20/month
- **Data Transfer**: Variable (~$10-30/month)

**Total**: ~$213-293/month

## Security Best Practices

1. Use Azure Key Vault for all secrets
2. Enable managed identities for containers
3. Use private endpoints for database
4. Enable Azure Defender for containers
5. Configure Network Security Groups (NSGs)
6. Use Application Gateway WAF
7. Enable diagnostic logging
8. Regular security updates and patching

## Backup and Disaster Recovery

```bash
# Database backups are automatic (7-day retention)
# Manual backup
az postgres flexible-server backup create \
  --resource-group $RESOURCE_GROUP \
  --name $DB_SERVER_NAME \
  --backup-name manual-backup-$(date +%Y%m%d)

# Configure geo-redundant backup
az postgres flexible-server update \
  --resource-group $RESOURCE_GROUP \
  --name $DB_SERVER_NAME \
  --geo-redundant-backup Enabled
```

## Scaling

### Manual Scaling

```bash
# Scale container instances
az container create \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-backend-2 \
  # ... same configuration as first instance
```

### Using Container Apps for Auto-Scaling

```bash
# Update scaling rules
az containerapp update \
  --name invoiceme-backend \
  --resource-group $RESOURCE_GROUP \
  --min-replicas 2 \
  --max-replicas 10 \
  --scale-rule-name http-scale \
  --scale-rule-type http \
  --scale-rule-http-concurrency 100
```

## Updating the Application

```bash
# Build and push new images
./scripts/build-all.sh
docker tag invoiceme-backend:latest ${ACR_LOGIN_SERVER}/invoiceme-backend:v1.1.0
docker push ${ACR_LOGIN_SERVER}/invoiceme-backend:v1.1.0

# Update container instance
az container create \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-backend \
  --image ${ACR_LOGIN_SERVER}/invoiceme-backend:v1.1.0 \
  # ... same configuration with new image tag
```

## Troubleshooting

### Container Won't Start

```bash
# Check container status
az container show \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-backend

# View events
az container logs \
  --resource-group $RESOURCE_GROUP \
  --name invoiceme-backend \
  --tail 100
```

### Database Connection Issues

```bash
# Test database connectivity
az postgres flexible-server connect \
  --name $DB_SERVER_NAME \
  --admin-user $DB_ADMIN_USER \
  --database-name $DB_NAME

# Check firewall rules
az postgres flexible-server firewall-rule list \
  --resource-group $RESOURCE_GROUP \
  --name $DB_SERVER_NAME
```

## Cleanup Resources

```bash
# Delete entire resource group (WARNING: deletes everything)
az group delete \
  --name $RESOURCE_GROUP \
  --yes \
  --no-wait
```

## Next Steps

- Configure custom domain with Azure DNS
- Set up Azure Front Door for CDN
- Implement Azure DevOps CI/CD pipeline
- Configure Azure Monitor dashboards
- Set up Azure Backup for long-term retention
- Consider migrating to AKS for advanced orchestration
