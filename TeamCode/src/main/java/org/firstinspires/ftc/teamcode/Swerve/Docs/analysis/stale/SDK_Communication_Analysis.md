# Analysis: SDK Communication & Multithreading

## The Core Finding

Based on the [gm0 SDK Communication](https://gm0.org/en/latest/docs/software/adv-control-system/sdk-communication.html) research:

> **LynxCommands being blocking (and more specifically a master lock being present on each USB device) means that multithreading hardware calls is at best not helpful and typically harmful to performance.**

Every hardware call — `motor.setPower()`, `encoder.getVoltage()`, `odo.update()`, `imu.getRobotYawPitchRollAngles()` — sends a blocking `LynxCommand`. The SDK holds a **per-USB-device master lock** while each command transacts. A second thread trying to issue a hardware call on the same USB bus is blocked at the mutex anyway, so you pay thread-switch overhead and get zero parallelism on the hardware side.

### Latency Estimates
| Link | Per-command latency |
|---|---|
| Control Hub (UART) | ~2 ms |
| Phone + Expansion Hub (USB) | ~3 ms |
| I²C device on Control Hub | 7+ ms (multiple commands) |

---

## Hardware Call Budget (Current Drivetrain)

Per loop, the drivetrain issues:

| Call | Where | Commands |
|---|---|---|
| `odo.update()` | `SwerveLocalizer.update()` | I²C burst (~7 ms on Expansion Hub) |
| `imu.getRobotYawPitchRollAngles()` | `SwerveLocalizer` fallback path | ~2–3 ms |
| `encoder.getVoltage()` × 4 | `SwerveModule.getCurrentRotation()` each call | ~8 ms total |
| `motor.getVelocity()` × 4 | `VelocityObserver` | ~8 ms total |
| `motor.getCurrent()` × 4 | Stall detection | ~8 ms total |
| `motor.setPower()` × 4 | `SwerveModule.update()` | ~8 ms total |
| `servo.setPower()` × 4 | `SwerveModule.update()` | ~8 ms total |

Total raw hardware time: **~50ms+** if executed sequentially without caching.

---

## The Solution: Bulk Reads

Instead of multithreading (which fails due to the master lock), the correct optimization is **Bulk Reads**. 

By setting `LynxModule.BulkCachingMode.AUTO` in `HWMap`, all read commands within a single loop cycle are satisfied by a single bulk request to the hub. This reduces the ~24ms of individual reads down to **~2ms total**.

---

## Why Not Thread Vision?

The Limelight 3A communicates over **Ethernet/WiFi**, not LynxCommands. `getLatestResult()` is a local memory read of the SDK's internal network cache. 

- `LimelightLocalizer.update()` cost: **~1-2ms**.
- Parallelizing this 2ms would introduce significant thread-safety complexity (synchronizing `masterPose`) for negligible gain.

---

## Verdict

1. **Do not multithread hardware calls.** Contention on the Lynx bus makes it a net negative.
2. **Use Bulk Reads.** This is the single largest performance gain available.
3. **Keep Vision on Main Thread.** The cost is low enough that the complexity of a background thread isn't justified.
