package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * MasterTestSuite
 * 
 * Central coordinator for all logic verification.
 * Fully compatible with Android Studio's JUnit Test Runner.
 */
public class MasterTestSuite {

    /**
     * Entry point for Android Studio and CLI testing.
     * Right-click and select "Run 'MasterTestSuite'" to execute.
     */
    @Test
    public void runFullSystemDiagnostics() {
        System.out.println("\n\u001B[34m" + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + "\u001B[0m");
        System.out.println("\u001B[1m" + "  SWERVE CONTROL SYSTEM - MASTER TEST RUNNER" + "\u001B[0m");
        System.out.println("\u001B[34m" + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + "\u001B[0m");

        List<Class<?>> testClasses = new ArrayList<>();
        testClasses.add(AdvancedSystemTest.class);
        testClasses.add(DiagnosticSystemTest.class);
        testClasses.add(FinalPolishTest.class);
        testClasses.add(HeadingRetentionTest.class);
        testClasses.add(MotionSmootherTest.class);
        testClasses.add(PIDControllerTest.class);
        testClasses.add(SwerveKinematicsTest.class);
        testClasses.add(SwerveStressTest.class);
        testClasses.add(SwerveVelocityObserverTest.class);
        testClasses.add(PinpointTest.class);

        int totalTests = 0;
        int passedTests = 0;

        for (Class<?> testClass : testClasses) {
            System.out.println("\n\u001B[33m" + "➤ Executing: " + testClass.getSimpleName() + "\u001B[0m");
            
            try {
                Object testInstance = testClass.getDeclaredConstructor().newInstance();
                Method[] methods = testClass.getDeclaredMethods();
                
                for (Method m : methods) {
                    if (m.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                        totalTests++;
                        try {
                            m.invoke(testInstance);
                            System.out.println("  \u001B[32m[ PASS ]\u001B[0m " + m.getName());
                            passedTests++;
                        } catch (Exception e) {
                            System.out.println("  \u001B[31m[ FAIL ]\u001B[0m " + m.getName());
                            if (e.getCause() != null) {
                                System.out.println("    \u001B[31mError: " + e.getCause().getMessage() + "\u001B[0m");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("\u001B[31m" + "[ ERR ] Failed to initialize suite: " + testClass.getSimpleName() + "\u001B[0m");
            }
        }

        System.out.println("\n\u001B[34m" + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + "\u001B[0m");
        System.out.print("  \u001B[1mRESULT:\u001B[0m ");
        
        if (passedTests == totalTests) {
            System.out.println("\u001B[32m" + "SYSTEM READY" + "\u001B[0m");
        } else {
            System.out.println("\u001B[31m" + "VERIFICATION FAILED" + "\u001B[0m");
        }
        
        System.out.println("  TOTAL:   " + totalTests + " | PASSED: " + passedTests + " | FAILED: " + (totalTests - passedTests));
        System.out.println("\u001B[34m" + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + "\u001B[0m\n");
    }
}
