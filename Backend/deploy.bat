@echo off
set PROJECT_ID=smartattendance-22b94
set REGION=us-central1
set SERVICE_NAME=smart-attendance-backend
set IMAGE_NAME=gcr.io/%PROJECT_ID%/%SERVICE_NAME%

echo ---------------------------------------------------
echo 🚀 Starting Deployment for %SERVICE_NAME%...
echo ---------------------------------------------------

echo 🏗️  Building Docker Image...
docker build -t %IMAGE_NAME% .

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker build failed. Make sure Docker Desktop is running.
    pause
    exit /b %ERRORLEVEL%
)

echo 📤 Pushing Image to Google Cloud...
docker push %IMAGE_NAME%

echo 🚀 Deploying to Cloud Run...
gcloud run deploy %SERVICE_NAME% ^
    --image %IMAGE_NAME% ^
    --platform managed ^
    --region %REGION% ^
    --allow-unauthenticated ^
    --memory 1Gi ^
    --cpu 1 ^
    --timeout 300 ^
    --set-env-vars "SPRING_PROFILES_ACTIVE=prod" ^
    --port 8080

echo ---------------------------------------------------
echo ✅ Deployment Complete!
echo ---------------------------------------------------
pause
