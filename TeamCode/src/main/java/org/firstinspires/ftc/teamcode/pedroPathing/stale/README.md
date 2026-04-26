# Pedro Stale Files

This folder holds Pedro experiments that are not part of the active robot path.

Current active Pedro path:

- `PedroSwerveFactory`
- `PedroUnifiedSwerveStack`
- `PedroLocalizerAdapter`
- `PedroDrivetrainAdapter`
- `PedroBlockCommand`
- `PedroBlockRouteBuilder`
- `PedroDecodeRoute`
- the actual Pedro auto OpModes

Moved here:

- `PedroSwervePod.java`
- `generated/TestAuto.java`
- `generated/Test2Auto.java`
- `generated/Test3Auto.java`

Why it was moved:

- it is not referenced anywhere in the repo
- the current integration does not use Pedro's older `SwervePod` path
- leaving it in the active package made the Pedro folder look more broken than it is
- the generated `Test*.java` autos were route-designer experiments, not part of the intended live robot set
