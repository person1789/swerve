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
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SwerveSimServer
 * 
 * Host-mode simulation runner. 
 * Connects the Java SwerveController to the Browser Visualizer.
 * Updated for Phase 3: Enhanced Simulation Fidelity (Physics/Battery/FF).
 */
public class SwerveSimServer extends WebSocketServer {

    @Test
    public void startSITLServer() throws InterruptedException {
        main(new String[0]);
        Thread.sleep(Long.MAX_VALUE);
    }

    private final SwerveController controller = new SwerveController();
    private final SwerveKinematics kinematics = new SwerveKinematics();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random random = new Random();
    
    // Simulation Parameters
    private static final int TARGET_HZ = 50; 
    private static final double DT = 1.0 / TARGET_HZ;

    // Physics Constants (GoBILDA 5203 series approximation)
    private static final double MAX_ACCEL = 18.0; 
    private static final double MAX_STEER_VEL = Math.PI * 6; // 1080 deg/s
    private static final double KV = 4.2;  // V / (m/s)
    private static final double KA = 0.45; // V / (m/s^2)
    private static final double KS = 1.05; // V (Static friction)
    private static final double NOMINAL_VOLTAGE = 12.8; 
    private static final double INTERNAL_RESISTANCE = 0.018; // Ohms
    private static final double ODO_NOISE_STD_DEV = 0.002; // 2mm jitter

    // Current State
    private double currentX = 0;
    private double currentY = 0;
    private double currentHeading = 0;
    private long startTimeNanos = System.nanoTime();
    private double batteryVoltage = NOMINAL_VOLTAGE;
    private final double[] currentDraw = new double[4];
    private double totalAmps = 0;
    
    // Inputs
    private double lastDrive = 0, lastStrafe = 0, lastTurn = 0;
    private boolean dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false;

    // Hardware State
    private final SwerveModuleState[] measuredStates = {
        new SwerveModuleState(), new SwerveModuleState(), new SwerveModuleState(), new SwerveModuleState()
    };

    public SwerveSimServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("SITL: Client Connected");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("SITL: Client Disconnected");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Map<String, Object> input = mapper.readValue(message, Map.class);
            lastDrive = (double) input.getOrDefault("drive", 0.0);
            lastStrafe = (double) input.getOrDefault("strafe", 0.0);
            lastTurn = (double) input.getOrDefault("turn", 0.0);

            dpadUp = (boolean) input.getOrDefault("dpad_up", false);
            dpadDown = (boolean) input.getOrDefault("dpad_down", false);
            dpadLeft = (boolean) input.getOrDefault("dpad_left", false);
            dpadRight = (boolean) input.getOrDefault("dpad_right", false);
            
            if (dpadUp)    controller.setSnapTarget(0);
            if (dpadLeft)  controller.setSnapTarget(Math.PI/2.0);
            if (dpadDown)  controller.setSnapTarget(Math.PI);
            if (dpadRight) controller.setSnapTarget(-Math.PI/2.0);
        } catch (Exception e) {}
    }

    @Override
    public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }

    @Override
    public void onStart() {
        System.out.println("SITL: Server Running on " + getPort());
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::step, 0, (long)(DT * 1000), TimeUnit.MILLISECONDS);
    }

    private void step() {
        long stepStart = System.nanoTime();

        // 1. Controller Update
        Pose targetVel = controller.update(lastDrive, lastStrafe, lastTurn, currentHeading, DT);
        SwerveModuleState[] targets = kinematics.toModuleStates(targetVel.x, targetVel.y, targetVel.heading);

        // 2. Physics Simulation
        double aggregateCurrent = 1.2; // Electronics baseline

        for (int i = 0; i < 4; i++) {
            // A. Steering (Simple lag with higher fidelity speed)
            double angleError = targets[i].angleRadians - measuredStates[i].angleRadians;
            while (angleError > Math.PI) angleError -= 2 * Math.PI;
            while (angleError < -Math.PI) angleError += 2 * Math.PI;
            double steerStep = Math.signum(angleError) * Math.min(Math.abs(angleError), MAX_STEER_VEL * DT);
            measuredStates[i].angleRadians += steerStep;

            // B. Drive (Voltage-Aware Feedforward Model)
            double v = measuredStates[i].speedMetersPerSecond;
            double dvRequested = (targets[i].speedMetersPerSecond - v) / DT;
            
            // Limit acceleration by battery voltage
            double ffVolts = KS * Math.signum(targets[i].speedMetersPerSecond) + KV * v + KA * dvRequested;
            if (Math.abs(ffVolts) > batteryVoltage) {
                double aMax = (batteryVoltage - KS - KV * Math.abs(v)) / KA;
                dvRequested = Math.signum(dvRequested) * Math.max(0, aMax);
            }
            dvRequested = Math.signum(dvRequested) * Math.min(Math.abs(dvRequested), MAX_ACCEL);
            measuredStates[i].speedMetersPerSecond += dvRequested * DT;

            // C. Current Estimation
            currentDraw[i] = Math.max(0, (Math.abs(dvRequested) * 4.2) + (Math.abs(v) * 1.8) + 0.5);
            aggregateCurrent += currentDraw[i];
        }

        totalAmps = aggregateCurrent;
        batteryVoltage = NOMINAL_VOLTAGE - (totalAmps * INTERNAL_RESISTANCE);

        // 3. Integration with Encoder Noise
        Pose actVel = kinematics.toChassisSpeeds(measuredStates);
        double rotHeading = currentHeading;
        double dx = actVel.x * Math.cos(rotHeading) - actVel.y * Math.sin(rotHeading);
        double dy = actVel.x * Math.sin(rotHeading) + actVel.y * Math.cos(rotHeading);
        
        currentX += (dx + random.nextGaussian() * ODO_NOISE_STD_DEV) * DT;
        currentY += (dy + random.nextGaussian() * ODO_NOISE_STD_DEV) * DT;
        currentHeading += actVel.heading * DT;

        double loopTime = (System.nanoTime() - stepStart) / 1_000_000.0;
        broadcastState(targets, measuredStates, actVel, loopTime);
    }

    private void broadcastState(SwerveModuleState[] targets, SwerveModuleState[] actuals, Pose obsVel, double loopTime) {
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("schemaVersion", 1);
            state.put("timestamp", (System.nanoTime() - startTimeNanos) / 1_000_000_000.0);
            state.put("x", currentX); state.put("y", currentY); state.put("heading", currentHeading);
            state.put("isMaintaining", controller.isMaintaining());
            state.put("isSnapping", controller.isSnapping());
            state.put("snapTargetRad", controller.getTargetHeading());
            state.put("driveX", lastStrafe);
            state.put("driveY", lastDrive);
            state.put("turn", lastTurn);
            state.put("controllerMode", controller.isSnapping() ? "snap" : controller.isMaintaining() ? "maintain" : "manual");
            state.put("drivetrainState", controller.isSnapping() ? "SNAPPING" : "DRIVING");
            
            double[][] tArr = new double[4][2], aArr = new double[4][2];
            for(int i=0; i<4; i++) {
                tArr[i][0] = targets[i].speedMetersPerSecond; tArr[i][1] = targets[i].angleRadians;
                aArr[i][0] = actuals[i].speedMetersPerSecond; aArr[i][1] = actuals[i].angleRadians;
            }
            state.put("targets", tArr); state.put("actuals", aArr);
            state.put("currentDraw", currentDraw);
            state.put("batteryVoltage", batteryVoltage);
            state.put("loopTimeMs", loopTime);

            Map<String, Object> gamepad = new HashMap<>();
            gamepad.put("lx", lastStrafe); gamepad.put("ly", lastDrive);
            gamepad.put("rx", lastTurn); gamepad.put("ry", 0.0);
            gamepad.put("lt", 0.0); gamepad.put("rt", 0.0);
            gamepad.put("dpad_up", dpadUp); gamepad.put("dpad_down", dpadDown);
            gamepad.put("dpad_left", dpadLeft); gamepad.put("dpad_right", dpadRight);
            state.put("gamepad", gamepad);

            Map<String, Object> oVel = new HashMap<>();
            oVel.put("vx", obsVel.x); oVel.put("vy", obsVel.y); oVel.put("omega", obsVel.heading);
            state.put("observerVel", oVel);

            Map<String, Object> smootherState = new HashMap<>();
            smootherState.put("vx", obsVel.x);
            smootherState.put("vy", obsVel.y);
            smootherState.put("omega", obsVel.heading);
            state.put("smootherState", smootherState);

            broadcast(mapper.writeValueAsString(state));
        } catch (Exception e) {}
    }

    public static void main(String[] args) { new SwerveSimServer(8080).start(); }
}
