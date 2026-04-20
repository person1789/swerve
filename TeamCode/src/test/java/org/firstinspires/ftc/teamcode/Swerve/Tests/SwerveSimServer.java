package org.firstinspires.ftc.teamcode.Swerve.Tests; // Indexing Heartbeat

import com.fasterxml.jackson.databind.ObjectMapper;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveController;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SwerveSimServer
 * 
 * Host-mode simulation runner. 
 * Connects the Java SwerveController to the Browser Visualizer.
 */
public class SwerveSimServer extends WebSocketServer {

    /**
     * Entry point for launching the SITL Server from a test runner.
     */
    @Test
    public void startSITLServer() throws InterruptedException {
        main(new String[0]);
        // Keep the test alive forever
        Thread.sleep(Long.MAX_VALUE);
    }

    private final SwerveController controller = new SwerveController();
    private final SwerveKinematics kinematics = new SwerveKinematics();
    private final ObjectMapper mapper = new ObjectMapper();
    
    // Simulation State
    private double currentX = 0;
    private double currentY = 0;
    private double currentHeading = 0;
    
    // Inputs from Client
    private double lastDrive = 0;
    private double lastStrafe = 0;
    private double lastTurn = 0;

    // Measured Hardware State (for Pro Visualization)
    private final SwerveModuleState[] measuredStates = {
        new SwerveModuleState(), new SwerveModuleState(), new SwerveModuleState(), new SwerveModuleState()
    };

    private static final int TARGET_HZ = 50; 
    private static final double DT = 1.0 / TARGET_HZ;
    
    // Physics Constants
    private static final double MAX_ACCEL = 15.0; // m/s^2
    private static final double MAX_STEER_VEL = Math.PI * 4; // rad/s (720 deg/s)

    public SwerveSimServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("SITL: Client Connected from " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("SITL: Client Disconnected");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            // Processing Gamepad Inputs from Browser
            Map<String, Object> input = mapper.readValue(message, Map.class);
            lastDrive = (double) input.getOrDefault("drive", 0.0);
            lastStrafe = (double) input.getOrDefault("strafe", 0.0);
            lastTurn = (double) input.getOrDefault("turn", 0.0);
            
            if ((boolean) input.getOrDefault("dpad_up", false))    controller.setSnapTarget(0);
            if ((boolean) input.getOrDefault("dpad_left", false))  controller.setSnapTarget(Math.PI/2.0);
            if ((boolean) input.getOrDefault("dpad_down", false))  controller.setSnapTarget(Math.PI);
            if ((boolean) input.getOrDefault("dpad_right", false)) controller.setSnapTarget(-Math.PI/2.0);
            
        } catch (Exception e) {
            // Ignore malformed packets
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("SITL: Server Started on port " + getPort());
        System.out.println("Target Loop Time: " + (int)(DT * 1000) + "ms (" + TARGET_HZ + "Hz)");
        
        // Main Simulation Loop
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::step, 0, (long)(DT * 1000), TimeUnit.MILLISECONDS);
    }

    private void step() {
        // 1. Run the PRODUCTION Java Controller
        Pose targetVel = controller.update(lastDrive, lastStrafe, lastTurn, currentHeading, DT);

        // 2. Kinematics (Target Setpoints)
        SwerveModuleState[] targetStates = kinematics.toModuleStates(targetVel.x, targetVel.y, targetVel.heading);

        // 3. Hardware Physics Emulation (Measured Response)
        for (int i = 0; i < 4; i++) {
            // Steering Lag
            double angleError = targetStates[i].angleRadians - measuredStates[i].angleRadians;
            // Normalize angle error to (-pi, pi)
            while (angleError > Math.PI) angleError -= 2 * Math.PI;
            while (angleError < -Math.PI) angleError += 2 * Math.PI;
            
            double steerStep = Math.signum(angleError) * Math.min(Math.abs(angleError), MAX_STEER_VEL * DT);
            measuredStates[i].angleRadians += steerStep;

            // Velocity Lag (Acceleration)
            double speedError = targetStates[i].speedMetersPerSecond - measuredStates[i].speedMetersPerSecond;
            double accelStep = Math.signum(speedError) * Math.min(Math.abs(speedError), MAX_ACCEL * DT);
            measuredStates[i].speedMetersPerSecond += accelStep;
        }

        // 4. Integrated Pose (Calculated from MEASURED states)
        Pose actualVel = kinematics.toChassisSpeeds(measuredStates);
        double dx = actualVel.x * Math.cos(currentHeading) - actualVel.y * Math.sin(currentHeading);
        double dy = actualVel.x * Math.sin(currentHeading) + actualVel.y * Math.cos(currentHeading);
        
        currentX += dx * DT;
        currentY += dy * DT;
        currentHeading += actualVel.heading * DT;

        // 5. Send State to Browser
        broadcastState(targetStates, measuredStates);
    }

    private void broadcastState(SwerveModuleState[] targets, SwerveModuleState[] actuals) {
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("x", currentX);
            state.put("y", currentY);
            state.put("heading", currentHeading);
            state.put("isMaintaining", controller.isMaintaining());
            state.put("isSnapping", controller.isSnapping());
            
            double[][] targetArr = new double[4][2];
            double[][] actualArr = new double[4][2];
            for(int i=0; i<4; i++) {
                targetArr[i][0] = targets[i].speedMetersPerSecond;
                targetArr[i][1] = targets[i].angleRadians;
                actualArr[i][0] = actuals[i].speedMetersPerSecond;
                actualArr[i][1] = actuals[i].angleRadians;
            }
            state.put("targets", targetArr);
            state.put("actuals", actualArr);

            broadcast(mapper.writeValueAsString(state));
        } catch (Exception e) {
            // broadcast failed
        }
    }

    public static void main(String[] args) {
        new SwerveSimServer(8080).start();
    }
}
