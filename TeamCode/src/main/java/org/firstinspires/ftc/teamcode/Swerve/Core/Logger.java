package org.firstinspires.ftc.teamcode.Swerve.Core;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;

import org.firstinspires.ftc.robotcore.external.Telemetry;



public class Logger {
    public static enum LogLevels {
        DEBUG,
        PRODUCTION,
        DRIVER_DATA
    }

    private Telemetry telemetry;
    private LogLevels state;

    public Logger(Telemetry telemetry) {
        if (SwerveConfig.DASHBOARD_ENABLED) {
            this.telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        } else {
            this.telemetry = telemetry;
        }
        this.telemetry.setDisplayFormat(Telemetry.DisplayFormat.HTML);
        this.telemetry.setMsTransmissionInterval(100);
        state = LogLevels.PRODUCTION;
    }

    public void log(String caption, Object data, LogLevels logLevel) {
        switch (state) {
            case DEBUG:
                if (!(logLevel == LogLevels.DRIVER_DATA)) {
                    telemetry.addData(caption, data);

                }
                break;
            case PRODUCTION:
                if (logLevel == LogLevels.PRODUCTION) {
                    telemetry.addData(caption, data);
                }
                break;
            case DRIVER_DATA:
                if (logLevel == LogLevels.DRIVER_DATA) {
                    telemetry.addData(caption, data);
                }
                break;
        }
    }

    public void updateLoggingLevel(boolean D_Pad_Right) {
        if (D_Pad_Right) {
            if (state == LogLevels.PRODUCTION) {
                state = LogLevels.DEBUG;
                telemetry.addData("CURRENT LOGGER STATE", state);
            } else if (state == LogLevels.DEBUG) {
                state = LogLevels.DRIVER_DATA;
                telemetry.addData("CURRENT LOGGER STATE", state);
            } else if(state == LogLevels.DRIVER_DATA)
                state = LogLevels.PRODUCTION;
            telemetry.addData("CURRENT LOGGER STATE", state);
        }
    }

    public void print() {
        telemetry.update();
    }

    public boolean DEBUG() {
        return state == LogLevels.DEBUG;
    }

    public boolean PRODUCTION() {
        return state == LogLevels.PRODUCTION;
    }

    public boolean DRIVER_DATA() {
        return state == LogLevels.DRIVER_DATA;
    }
}
