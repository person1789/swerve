package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServoImplEx;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.hardware.lynx.LynxModule;

import java.util.List;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

public class HWMap {

    // Front Left Module Hardware
    public DcMotorEx FLM;
    public CRServoImplEx FLS;
    public AnalogInput FLE;

    // Front Right Module Hardware
    public DcMotorEx FRM;
    public CRServoImplEx FRS;
    public AnalogInput FRE;

    // Back Right Module Hardware
    public DcMotorEx BRM;
    public CRServoImplEx BRS;
    public AnalogInput BRE;

    // Back Left Module Hardware
    public DcMotorEx BLM;
    public CRServoImplEx BLS;
    public AnalogInput BLE;

    private VoltageSensor voltageSensor;

    private final GoBildaPinpointDriver odo;
    public final IMU imu;

    public HWMap(HardwareMap hardwareMap) {
        FLM = hardwareMap.get(DcMotorEx.class, "FLM");
        FRM = hardwareMap.get(DcMotorEx.class, "FRM");
        BLM = hardwareMap.get(DcMotorEx.class, "BLM");
        BRM = hardwareMap.get(DcMotorEx.class, "BRM");

        FLS = hardwareMap.get(CRServoImplEx.class, "FLS");
        FRS = hardwareMap.get(CRServoImplEx.class, "FRS");
        BLS = hardwareMap.get(CRServoImplEx.class, "BLS");
        BRS = hardwareMap.get(CRServoImplEx.class, "BRS");

        FLE = hardwareMap.get(AnalogInput.class, "FLE");
        FRE = hardwareMap.get(AnalogInput.class, "FRE");
        BLE = hardwareMap.get(AnalogInput.class, "BLE");
        BRE = hardwareMap.get(AnalogInput.class, "BRE");

        odo = hardwareMap.get(GoBildaPinpointDriver.class, "odo");

        imu = hardwareMap.get(IMU.class, "imu");
        IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
                SwerveConfig.HUB_LOGO_DIR, SwerveConfig.HUB_USB_DIR));
        imu.initialize(parameters);

        voltageSensor = hardwareMap.voltageSensor.iterator().next();

        List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
    }

    public GoBildaPinpointDriver getOdo() {
        return odo;
    }

    public VoltageSensor getVoltageSensor() {
        return voltageSensor;
    }
}
