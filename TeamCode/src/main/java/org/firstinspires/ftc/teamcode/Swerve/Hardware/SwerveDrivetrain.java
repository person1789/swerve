/**
 * SwerveDrivetrain: The high-level manager for the entire swerve drive system.
 * It coordinates how the robot moves by connecting driver commands to the correct 
 * mathematical calculations and then sending those results to each individual wheel module.
 */
package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveAuditor;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.core.HWMap;
import org.firstinspires.ftc.teamcode.core.Logger;

/**
 * SwerveDrivetrain — Phase C
 *
 * Top-level drivetrain coordinator. Wires together the full pipeline:
 * Kinematics → Auditor → SwerveModule.
 */
public class SwerveDrivetrain {

    public enum States {
        DRIVING,
        WAITING_TO_LOCK,
        LOCKED
    }

    public final SwerveModule frontLeftModule;
    public final SwerveModule frontRightModule;
    public final SwerveModule backRightModule;
    public final SwerveModule backLeftModule;
    public final SwerveModule[] modules;

    private final SwerveKinematics kinematics;
    private final SwerveAuditor auditor;

    private States state = States.DRIVING;
    private final ElapsedTime lockTimer = new ElapsedTime();

    private final Logger logger;

    /**
     * Construct the drivetrain and initialize modules using SwerveConfig.
     */
    public SwerveDrivetrain(HWMap hwMap, Logger logger) {
        this.logger = logger;

        // Initialize modules using offsets and inversions from SwerveConfig
        frontLeftModule = new SwerveModule(hwMap.FLM, hwMap.FLS, hwMap.FLE,
                SwerveConfig.OFFSETS[0], SwerveConfig.INVERSIONS[0], logger);
        frontRightModule = new SwerveModule(hwMap.FRM, hwMap.FRS, hwMap.FRE,
                SwerveConfig.OFFSETS[1], SwerveConfig.INVERSIONS[1], logger);
        backRightModule = new SwerveModule(hwMap.BRM, hwMap.BRS, hwMap.BRE,
                SwerveConfig.OFFSETS[2], SwerveConfig.INVERSIONS[2], logger);
        backLeftModule = new SwerveModule(hwMap.BLM, hwMap.BLS, hwMap.BLE,
                SwerveConfig.OFFSETS[3], SwerveConfig.INVERSIONS[3], logger);

        modules = new SwerveModule[] {
                frontLeftModule,  // 0 — FL
                frontRightModule, // 1 — FR
                backRightModule,  // 2 — RR
                backLeftModule    // 3 — RL
        };

        for (SwerveModule m : modules) {
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        kinematics = new SwerveKinematics();
        auditor = new SwerveAuditor();
    }

    /**
     * Command the robot using a chassis-level pose velocity.
     * 
     * @param pose {x = robot-forward m/s, y = robot-left m/s, heading = rad/s}
     */
    public void setPose(Pose pose) {
        boolean hasInput = (Math.hypot(pose.x, pose.y) > 0.01 || Math.abs(pose.heading) > 0.01);

        switch (state) {
            case DRIVING:
                driveWithPipeline(pose);
                if (!hasInput) {
                    lockTimer.reset();
                    state = States.WAITING_TO_LOCK;
                }
                break;

            case WAITING_TO_LOCK:
                driveWithPipeline(new Pose(0, 0, 0));
                if (hasInput) {
                    state = States.DRIVING;
                } else if (lockTimer.milliseconds() > SwerveConfig.LOCK_DELAY_MS) {
                    state = States.LOCKED;
                }
                break;

            case LOCKED:
                applyXStance();
                if (hasInput) state = States.DRIVING;
                break;
        }
    }

    private void driveWithPipeline(Pose pose) {
        // Step 1: IK
        SwerveModuleState[] raw = kinematics.toModuleStates(pose.x, pose.y, pose.heading);

        // Step 2: Audit & Optimize
        double[] currentAngles = new double[4];
        for (int i = 0; i < 4; i++) currentAngles[i] = modules[i].getCurrentRotation();

        SwerveModuleState[] optimized = auditor.optimize(raw, currentAngles, SwerveConfig.MAX_SPEED_MPS);

        // Step 3: Command hardware
        for (int i = 0; i < 4; i++) {
            modules[i].update(optimized[i]);
        }
    }

    private void applyXStance() {
        // X-stance angles: [FL, FR, RR, RL]
        double[] xAngles = {
                Math.toRadians(45),  // FL
                Math.toRadians(-45), // FR
                Math.toRadians(45),  // RR
                Math.toRadians(-45)  // RL
        };
        for (int i = 0; i < 4; i++) {
            modules[i].update(xAngles[i], 0.0);
        }
    }

    public void log() {
        for (int i = 0; i < 4; i++) {
            modules[i].log(i);
        }
    }

    public void setOffsets(double[] offsets) {
        for (int i = 0; i < 4; i++) modules[i].setOffset(offsets[i]);
    }

    public void setMotorScaling(double[] scalings) {
        for (int i = 0; i < 4; i++) modules[i].setMotorScaling(scalings[i]);
    }

    public States getState() { return state; }
}