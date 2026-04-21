# Java SITL Schema Audit

Authoritative Java emitter:

- `TeamCode/src/test/java/org/firstinspires/ftc/teamcode/Swerve/Tests/SwerveSimServer.java`

Frontend contract:

- `swerve-scope/src/types/telemetry.ts`
- `swerve-scope/src/hooks/useSITL.ts`

## Confirmed Fields Emitted By Java

- `schemaVersion`
- `timestamp`
- `x`
- `y`
- `heading`
- `isMaintaining`
- `isSnapping`
- `snapTargetRad`
- `driveX`
- `driveY`
- `turn`
- `controllerMode`
- `drivetrainState`
- `targets`
- `actuals`
- `currentDraw`
- `batteryVoltage`
- `loopTimeMs`
- `gamepad`
- `observerVel`
- `smootherState`

## Confirmed Frontend Handling

- Java payload shape is normalized directly in `parseTelemetryFrame(...)`.
- Missing optional arrays and nested objects are safely defaulted.
- Java `gamepad` shape without `buttons` is accepted.
- Java `smootherState` shape with only `vx/vy/omega` is accepted.

## Intentional Contract Gaps

- `logs`
  - Supported by the frontend contract.
  - Not currently emitted by `SwerveSimServer.java`.
- `gamepad.buttons`
  - Supported by the frontend contract.
  - Not currently emitted by `SwerveSimServer.java`.
- `smootherState.ax/ay/alpha`
  - Supported by the frontend contract.
  - Not currently emitted by `SwerveSimServer.java`.

## Behavioral Note

- Java currently emits `drivetrainState` as `SNAPPING` or `DRIVING`.
- Local sim may also emit `IDLE`.
- This is acceptable today because the frontend treats `drivetrainState` as a free string, not a strict enum.

## Verification

- `src/hooks/useSITL.test.ts` contains a fixture matching the Java payload shape.
- `npm test`
- `npm run build`
