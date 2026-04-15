#!/bin/bash

# ==============================================================================
# 🚀 Smart Attendance - Cloud Run Deployment Script
# ==============================================================================

# 1. Configuration - EDIT THESE VALUES
PROJECT_ID="smartattendance-22b94"  # Found in GCP Console
REGION="us-central1"          # Or your preferred region
SERVICE_NAME="smart-attendance-backend"
IMAGE_NAME="gcr.io/$PROJECT_ID/$SERVICE_NAME"

echo "---------------------------------------------------"
echo "Starting Deployment for $SERVICE_NAME..."
echo "---------------------------------------------------"

# 2. Check for gcloud CLI
if ! command -v gcloud &> /dev/null
then
    echo "❌ Error: gcloud CLI is not installed. Please install it first."
    exit
fi

# 3. Authenticate and set project (uncomment if needed)
# gcloud auth login
# gcloud config set project $PROJECT_ID

# 4. Build the image locally using the Dockerfile
echo "🏗️  Building Docker Image..."
docker build -t $IMAGE_NAME .

# 5. Push to Google Container Registry (or Artifact Registry)
echo "📤 Pushing Image to GCR..."
docker push $IMAGE_NAME

# 6. Deploy to Cloud Run
echo "🚀 Deploying to Cloud Run..."
gcloud run deploy $SERVICE_NAME \
    --image $IMAGE_NAME \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 1Gi \
    --cpu 1 \
    --timeout 300 \
    --set-env-vars "SPRING_PROFILES_ACTIVE=prod" \
    --port 8080

echo "---------------------------------------------------"
echo "✅ Deployment Complete!"
echo "---------------------------------------------------"
