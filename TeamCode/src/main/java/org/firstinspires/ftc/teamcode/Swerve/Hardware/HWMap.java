package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.hardware.limelightvision.Limelight3A;

import java.util.List;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

public class HWMap {

    public final DcMotorEx FLM;
    public final CRServo FLS;
    public final AnalogInput FLE;

    public final DcMotorEx FRM;
    public final CRServo FRS;
    public final AnalogInput FRE;

    public final DcMotorEx BRM;
    public final CRServo BRS;
    public final AnalogInput BRE;

    public final DcMotorEx BLM;
    public final CRServo BLS;
    public final AnalogInput BLE;

    public final IMU imu;
    public final GoBildaPinpointDriver odo;
    public final VoltageSensor voltageSensor;
    public final Limelight3A limelight;

    private final List<LynxModule> allHubs;

    public HWMap(HardwareMap hardwareMap) {
        FLM = hardwareMap.get(DcMotorEx.class, "FLM");
        FRM = hardwareMap.get(DcMotorEx.class, "FRM");
        BRM = hardwareMap.get(DcMotorEx.class, "BRM");
        BLM = hardwareMap.get(DcMotorEx.class, "BLM");

        FLS = hardwareMap.get(CRServo.class, "FLS");
        FRS = hardwareMap.get(CRServo.class, "FRS");
        BRS = hardwareMap.get(CRServo.class, "BRS");
        BLS = hardwareMap.get(CRServo.class, "BLS");

        FLE = hardwareMap.get(AnalogInput.class, "FLE");
        FRE = hardwareMap.get(AnalogInput.class, "FRE");
        BRE = hardwareMap.get(AnalogInput.class, "BRE");
        BLE = hardwareMap.get(AnalogInput.class, "BLE");

        odo = hardwareMap.get(GoBildaPinpointDriver.class, "odo");
        voltageSensor = hardwareMap.voltageSensor.iterator().next();
        limelight = hardwareMap.get(Limelight3A.class, "limelight");

        imu = hardwareMap.get(IMU.class, "imu");
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                SwerveConfig.HUB_LOGO_DIR, SwerveConfig.HUB_USB_DIR)));

        allHubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }
    }

    public void clearBulkCache() {
        for (LynxModule hub : allHubs) {
            hub.clearBulkCache();
        }
    }

    public GoBildaPinpointDriver getOdo() {
        return odo;
    }
}
