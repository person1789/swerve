const { app, BrowserWindow } = require('electron');
const path = require('path');
const { spawn } = require('child_process');

// Disable GPU shader cache globally BEFORE app is ready
app.commandLine.appendSwitch('disable-gpu-shader-disk-cache');

let mainWindow;
let javaProcess;

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1280,
        height: 850,
        title: "Swerve SITL - Engineering Sandbox",
        backgroundColor: '#020617', // Match slate-950
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
        },
        autoHideMenuBar: true
    });

    const startUrl = process.env.ELECTRON_START_URL || 'http://localhost:5173';
    mainWindow.loadURL(startUrl).catch(e => {
        console.error("SITL App: Failed to load URL:", e);
    });

    mainWindow.on('closed', function () {
        mainWindow = null;
    });
}

/**
 * Automatically launch the Java SITL backend on startup
 */
function launchJavaBackend() {
    console.log("SITL App: Launching Java Simulation Brain...");
    
    // We navigate up to the root project folder to find gradlew
    const rootPath = path.join(__dirname, '..');
    
    // Using --no-daemon and -Dorg.gradle.daemon=false for isolation
    javaProcess = spawn('cmd.exe', ['/c', 'gradlew.bat', 'testDebugUnitTest', '--tests', 'org.firstinspires.ftc.teamcode.Swerve.Tests.MasterTestSuite.launchSITL', '--no-daemon', '-Dorg.gradle.daemon=false'], {
        cwd: rootPath,
        shell: true
    });

    javaProcess.stdout.on('data', (data) => {
        console.log(`[Java Brain]: ${data}`);
    });

    javaProcess.stderr.on('data', (data) => {
        console.error(`[Java Error]: ${data}`);
    });
}

app.on('ready', () => {
    launchJavaBackend();
    createWindow();
});

app.on('window-all-closed', function () {
    // Kill the Java process when the app closes
    if (javaProcess) {
        console.log("SITL App: Shutting down Java Brain...");
        javaProcess.kill();
        // Force kill if necessary after a timeout
        spawn("taskkill", ["/pid", javaProcess.pid, '/f', '/t']);
    }
    
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

app.on('activate', function () {
    if (mainWindow === null) {
        createWindow();
    }
});
