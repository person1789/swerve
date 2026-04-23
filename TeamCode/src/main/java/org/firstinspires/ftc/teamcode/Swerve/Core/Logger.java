package org.firstinspires.ftc.teamcode.Swerve.Core;

import org.firstinspires.ftc.robotcore.external.Telemetry;



public class Logger {
    public static enum LogLevels {
        DEBUG,
        PRODUCTION,
        DRIVER_DATA
    }

    private Telemetry telemetry;
    private LogLevels state;
    private boolean previousTogglePressed;

    public Logger(Telemetry telemetry) {
        this.telemetry = telemetry;
        this.telemetry.setDisplayFormat(Telemetry.DisplayFormat.HTML);
        this.telemetry.setMsTransmissionInterval(100);
        state = LogLevels.PRODUCTION;
        previousTogglePressed = false;
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

    public void updateLoggingLevel(boolean toggleButtonPressed) {
        boolean togglePressed = toggleButtonPressed && !previousTogglePressed;
        previousTogglePressed = toggleButtonPressed;

        if (togglePressed) {
            if (state == LogLevels.PRODUCTION) {
                state = LogLevels.DEBUG;
            } else if (state == LogLevels.DEBUG) {
                state = LogLevels.DRIVER_DATA;
            } else if (state == LogLevels.DRIVER_DATA) {
                state = LogLevels.PRODUCTION;
            }
            telemetry.addData("CURRENT LOGGER STATE", state);
        }
    }

    public void print() {
        telemetry.update();
    }

    public LogLevels getState() {
        return state;
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
