# 🚀 THE ULTIMATE USB DEPLOYMENT GUIDE (MASTER VERSION)
**Target: Local College Server (Windows or Linux Support)**

This guide walks you through every single step to launch your pre-compiled, optimized application from your USB drive onto the college server. Follow these steps exactly in order.

## 🛑 PART 1: BEFORE YOU LEAVE HOME (USB PREP)
1. Plug your USB Drive into your home computer.
2. Ensure you have downloaded these 4 installers to the USB Drive:
   - Java 21 Installer (`.msi` for Windows, or keep the link handy for Linux)
   - PostgreSQL 16 Installer (`.exe` for Windows)
   - Node.js Installer (`.msi` for Windows)
   - Cloudflared Installer (`.msi` for Windows)
3. Copy the **entire `smartAttendence` folder** that is on your Desktop directly onto the USB Drive. (This copy already has your compiled Java `.jar` and your built Next.js frontend).

---

## 🏫 PART 2: AT THE COLLEGE (INSTALLING SOFTWARE)
When you sit at the college computer, follow the instructions for the Operating System they provide you.

### OPTION A: If the College computer is WINDOWS
1. Plug in your USB Drive.
2. Install **Java 21** (`.msi`), **Node.js** (`.msi`), and **Cloudflared** (`.msi`) by clicking Next > Next > Finish.
3. Install **PostgreSQL 16** (`.exe`). 
   - ⚠️ **CRITICAL:** When the installer asks for a Master Password, you **MUST type `Pragathi@2105`**. Do not forget this, or the backend will fail.
4. Once PostgreSQL is installed, open the application called **"pgAdmin 4"**.
   - Input the same password (`Pragathi@2105`).
   - On the left panel, right-click on **Databases** -> **Create** -> **Database...**
   - 🛠️ **DATABASE NAME:** Inside pgAdmin, name your database exactly: `smart_attendance`
5. **Install pnpm**: Open a PowerShell window and run:
   ```powershell
   npm install -g pnpm
   ```

### OPTION B: If the College computer is LINUX (Ubuntu)
1. Plug in your USB and copy the `smartAttendence` folder to the Desktop.
2. Open the Terminal (`Ctrl + Alt + T`).
3. Run these exact commands, one line at a time:
   ```bash
   # Update system and install Java
   sudo apt update
   sudo apt install -y openjdk-21-jdk curl

   # Install Node.js
   curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
   sudo apt install -y nodejs

   # Install PostgreSQL
   sudo apt install -y postgresql postgresql-contrib

   # 🔑 PASSWORD: Set the PostgreSQL password to Pragathi@2105
   sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'Pragathi@2105';"

   # 🛠️ DATABASE NAME: Create the database
   sudo -u postgres createdb smart_attendance

   # Install pnpm 
   sudo npm install -g pnpm

   # Install Cloudflare (Required for generating links)
   curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
   sudo dpkg -i cloudflared.deb
   ```

---

## 🚀 PART 3: LAUNCHING THE SYSTEM
Now that the software is installed, you will turn on your application.

1. **Move the Project**: Copy the `smartAttendence` folder from your USB Drive onto the main drive of the college computer (e.g., `C:\smartAttendence` on Windows, or `/home/user/smartAttendence` on Linux).

2. **Open the Start Script**:
   - **On WINDOWS**: 
     - Open the `smartAttendence` folder.
     - Right-click the file named `start-system.ps1`.
     - Click **"Run with PowerShell"**.
     *(It will pop open two blue windows: One running the Spring Boot Backend on **Port 10000**, and one running the Next.js Frontend on **Port 3000**).*

   - **On LINUX**:
     - Open a terminal inside the `smartAttendence` folder.
     - Make the start script executable, and run it:
       ```bash
       chmod +x start-system.sh
       ./start-system.sh
       ```

3. **Install Frontend Packages**:
   - Open a terminal inside `frontend/web-dashboard`.
   - Run: `pnpm install`
   *(This downloads the website components. It takes about 2-3 minutes).*

---

## 🌍 PART 4: OPENING IT TO STUDENT PHONES
Right now, the site only works on the big computer. We need to generate a Cloudflare link so the Frontend can talk to the Backend over the internet.

1. Open a **brand new** PowerShell window (Windows) or Terminal (Linux).
2. Type this exact command to expose your **BACKEND**:
   ```bash
   # 📍 PORT: We are tunneling the BACKEND so the Frontend can reach it
   cloudflared tunnel --url http://localhost:10000
   ```
3. The screen will output a lot of text. Look for the line that says:
   `+ https://some-random-words.trycloudflare.com`
4. **Highlight and Copy that URL!** This is your **API URL**. 
5. (Keep this terminal running in the background forever).

---

## 🔌 PART 5: THE FINAL CONNECTION
Now we tell the Frontend to connect to that "API URL".

**On WINDOWS:**
1. Open the `smartAttendence` folder.
2. Right-click `update-client-url.ps1` and click **"Run with PowerShell"**.
3. When it asks "Please enter the Cloudflare Tunnel URL:", **Paste the API URL** you just copied.

**On LINUX:**
1. Open a terminal inside the folder.
2. Run `./update-client-url.sh` and paste the API URL.

**⚠️ CRITICAL STEP: ACCESSING THE WEBSITE**
If students need to access the **full website** from their phones:
1. Open **ANOTHER** terminal/PowerShell.
2. Run: `cloudflared tunnel --url http://localhost:3000`
3. Give students the **NEW** `.trycloudflare.com` link that appears. 
4. This link will take them to the login screen!

**⚠️ CRITICAL STEP: LOCK IN THE LINK:**
To apply that URL permanently, you **MUST** run a final build. 
Open a terminal inside the `frontend/web-dashboard` folder and run:
   ```bash
   pnpm run build
   ```

## 🎉 YOU ARE DONE!
Open the browser on the college computer and go to `http://localhost:3000`. You will see the login screen, and the system is officially live! Students can now access it using the Cloudflare `.trycloudflare.com` link on their phones.

The system is now **Hardened, Optimized, and Ready for Launch**. 🚀

---

## 🛠️ PART 6: MAINTENANCE & REMOTE MONITORING

### 1. How to Check if things are Working
- **Real-time**: Look at the two blue PowerShell windows. If they are scrolling text, the system is alive.
- **History**: Open the file `smart-attendance-audit.log` in the project folder to see a list of all recent activity.
- **Errors**: Check `backend_error.log` if something stops working.

### 2. Remote Access (Pro Tip)
To avoid walking to the server room every time you need to check something:
1. Install **AnyDesk** or **TeamViewer** on the College Server.
2. Set a "Fixed Password" for unattended access.
3. You can now login to the server screen from your own laptop or phone!

### 3. What to tell College IT Staff
If they ask why you need remote access, use this professional explanation:
> *"This is for **Remote Administrative Monitoring**. It allows me to monitor system health and database logs remotely to ensure the attendance system stays online for students without requiring physical access to the server."*

