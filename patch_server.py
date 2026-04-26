import subprocess
import base64
import os

def run_vm_cmd(cmd_str):
    full_cmd = [
        "az", "vm", "run-command", "invoke",
        "--resource-group", "SMART-ATTENDANCE-RG",
        "--name", "smart-attendance-server",
        "--command-id", "RunShellScript",
        "--scripts", cmd_str
    ]
    result = subprocess.run(full_cmd, capture_output=True, text=True, shell=True)
    return result

def patch_file(local_path, remote_path):
    print(f"Patching {remote_path}...")
    with open(local_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    b64_code = base64.b64encode(content.encode("utf-8")).decode()
    chunk_size = 1000
    chunks = [b64_code[i:i+chunk_size] for i in range(0, len(b64_code), chunk_size)]
    
    # Clear temp file
    run_vm_cmd(f"> {remote_path}.b64")
    
    # Append chunks
    for i, chunk in enumerate(chunks):
        print(f"  Uploading chunk {i+1}/{len(chunks)}...")
        run_vm_cmd(f"echo -n '{chunk}' >> {remote_path}.b64")
    
    # Decode
    print(f"  Finalizing {remote_path}...")
    run_vm_cmd(f"base64 -d {remote_path}.b64 > {remote_path}")
    run_vm_cmd(f"rm {remote_path}.b64")

files_to_patch = [
    ("Backend/src/main/java/com/example/smartAttendence/controller/v1/AuthV1Controller.java", "/home/azureuser/Backend/src/main/java/com/example/smartAttendence/controller/v1/AuthV1Controller.java"),
    ("Backend/src/main/java/com/example/smartAttendence/service/v1/AuthenticationService.java", "/home/azureuser/Backend/src/main/java/com/example/smartAttendence/service/v1/AuthenticationService.java"),
    ("Backend/src/main/java/com/example/smartAttendence/repository/v1/AttendanceRecordV1Repository.java", "/home/azureuser/Backend/src/main/java/com/example/smartAttendence/repository/v1/AttendanceRecordV1Repository.java"),
    ("Backend/src/main/java/com/example/smartAttendence/service/v1/StudentV1Service.java", "/home/azureuser/Backend/src/main/java/com/example/smartAttendence/service/v1/StudentV1Service.java"),
    ("Backend/src/main/java/com/example/smartAttendence/controller/v1/StudentV1Controller.java", "/home/azureuser/Backend/src/main/java/com/example/smartAttendence/controller/v1/StudentV1Controller.java")
]

for local, remote in files_to_patch:
    patch_file(local, remote)

print("Rebuilding Backend...")
final_script = """
export HOME=/home/azureuser
cd /home/azureuser/Backend
./mvnw clean package -DskipTests && bash /home/azureuser/final_start.sh
"""
res = run_vm_cmd(final_script)
print(res.stdout)
print(res.stderr)
