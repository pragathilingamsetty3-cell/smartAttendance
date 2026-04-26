import subprocess

scripts = """
cat > /home/azureuser/Backend/src/main/resources/application.properties << 'EOF'
spring.application.name=Smart-Attendance-Engine
spring.profiles.active=local
server.tomcat.max-http-form-post-size=104857600
server.forward-headers-strategy=native

# JWT Config
jwt.issuer=${JWT_ISSUER:smart-attendance-college-server}
jwt.access-token.expiration-minutes=${JWT_ACCESS_TOKEN_EXPIRATION_MINUTES:60}
jwt.refresh-token.expiration-days=${JWT_REFRESH_TOKEN_EXPIRATION_DAYS:120}
jwt.public-key=${JWT_PUBLIC_KEY}
jwt.private-key=${JWT_PRIVATE_KEY}

# Database Config
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:smart_attendance}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# Security
security.cors.allowed-origins=https://smartattendance-b44.pages.dev,http://localhost:3000
security.threat.detection.enabled=true
security.jwt.blacklist.enabled=true

# Firebase
firebase.enabled=true
firebase.service-account-path=${FIREBASE_SERVICE_ACCOUNT_PATH}

# 📧 Mail Configuration (RESTORED)
spring.mail.host=${EMAIL_HOST:smtp.gmail.com}
spring.mail.port=${EMAIL_PORT:587}
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.trust=${EMAIL_HOST:smtp.gmail.com}
EOF
cat /home/azureuser/Backend/src/main/resources/application.properties
"""

cmd = [
    "az", "vm", "run-command", "invoke",
    "--resource-group", "SMART-ATTENDANCE-RG",
    "--name", "smart-attendance-server",
    "--command-id", "RunShellScript",
    "--scripts", scripts,
    "--query", "value[0].message",
    "-o", "tsv"
]

print(f"Executing command to update server application.properties...")
result = subprocess.run(cmd, capture_output=True, text=True, shell=True)
print(result.stdout)
print(result.stderr)
