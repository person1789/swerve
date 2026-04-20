package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * MasterTestSuite
 * 
 * Central coordinator for all logic verification.
 * Discoverable and runnable via Android Studio Unit Test Runner.
 */
public class MasterTestSuite {

    /**
     * Entry point for Android Studio "Run Test" context menu.
     */
    @Test
    public void runFullSystemDiagnostics() {
        main(new String[0]);
    }

    public static void main(String[] args) {
        System.out.println("\n\u001B[34m" + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + "\u001B[0m");
        System.out.println("\u001B[1m" + "  SWERVE CONTROL SYSTEM - LOCAL MASTER RUNNER" + "\u001B[0m");
        System.out.println("\u001B[34m" + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + "\u001B[0m");

        List<Class<?>> testClasses = new ArrayList<>();
        testClasses.add(AdvancedSystemTest.class);
        testClasses.add(DiagnosticSystemTest.class);
        testClasses.add(GeometryTest.class);
        testClasses.add(HeadingRetentionTest.class);
        testClasses.add(MotionSmootherTest.class);
        testClasses.add(PIDControllerTest.class);
        testClasses.add(SwerveKinematicsTest.class);
        testClasses.add(SwerveVelocityObserverTest.class);

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;

        for (Class<?> testClass : testClasses) {
            System.out.println("\n\u001B[1m" + "➤ CLASS: " + testClass.getSimpleName() + "\u001B[0m");
            
            try {
                Object testObject = testClass.getDeclaredConstructor().newInstance();
                Method[] methods = testClass.getDeclaredMethods();

                for (Method m : methods) {
                    if (m.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                        totalTests++;
                        try {
                            m.setAccessible(true);
                            m.invoke(testObject);
                            System.out.println("  \u001B[32m✔\u001B[0m " + m.getName());
                            passedTests++;
                        } catch (Exception e) {
                            System.out.println("  \u001B[31m✘\u001B[0m " + m.getName());
                            System.out.println("    \u001B[90m└─ Reason: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()) + "\u001B[0m");
                            failedTests++;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("  \u001B[31m⚠ ERROR\u001B[0m Could not initialize: " + testClass.getSimpleName());
            }
        }

        System.out.println("\n\u001B[34m" + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + "\u001B[0m");
        System.out.println("\u001B[1m" + "  FINAL VERIFICATION SCORECARD" + "\u001B[0m");
        System.out.println("\u001B[34m" + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + "\u001B[0m");
        System.out.print("  TOTAL:   " + totalTests);
        System.out.print(" | \u001B[32mPASSED: " + passedTests + "\u001B[0m");
        System.out.println(" | \u001B[31mFAILED: " + failedTests + "\u001B[0m");
        
        if (failedTests == 0) {
            System.out.println("\n  \u001B[42m\u001B[30m STATUS: SYSTEM READY \u001B[0m");
        } else {
            System.out.println("\n  \u001B[41m\u001B[37m STATUS: REVIEW REQUIRED \u001B[0m");
        }
        System.out.println("\u001B[34m" + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + "\u001B[0m\n");
    }
}
