# Pedro Route Designer

## Purpose

The route designer in SwerveScope is a frontend tool for laying out Pedro block routes visually before copying them into robot code.

It is meant to help with:

- picking a start pose
- checking robot footprint against the field
- sketching straight and curved route segments
- setting target headings at each block endpoint
- saving and reopening route drafts as JSON
- creating or patching Pedro auto files directly from the simulator
- generating Java code that matches the existing Pedro route helpers

## What it generates

The designer generates Java for:

- a start pose
- a `PedroBlockRouteBuilder.build(...)` call
- a sequence of `PedroBlockCommand.straight(...)` and `PedroBlockCommand.curved(...)` blocks

That means the generated code already matches the route layer used by:

- `PedroDecodeRoute`
- robot autonomous code
- the Pedro simulator path definitions

## Start pose handling

There are two start-pose modes:

1. Preset start pose
2. Custom start pose

### Preset

If the start pose matches one of the known field presets, the generated code uses:

```java
Pose startPose = PedroDecodeRoute.startPose(PedroStartPose.RED_BASE_CORNER);
```

### Custom

If the start pose is dragged or edited away from a preset, the generated code uses:

```java
Pose startPose = PedroStartPose.custom(xIn, yIn, headingDeg);
```

This keeps route editing fast without forcing every path to be tied to an enum value.

## Curved segments

Curved blocks use one control point and generate a quadratic Bezier segment through `PedroBlockRouteBuilder`.

For each curved block, the designer tracks:

- end X
- end Y
- end heading
- control X
- control Y
- control heading
- control scale
- heading interpolation weight

## Using a field image

The designer supports loading a local field image as a background overlay.

Recommended workflow:

1. Open the simulator UI.
2. Go to the route-designer panel.
3. Click the field-image button.
4. Choose a field reference image from disk.
5. Adjust opacity until the path, robot outline, and field image are all easy to read.

This gives a better layout reference than relying on memory alone, especially when checking lane widths and turning room.

## Saving and loading routes

The designer can save the full route workspace as a JSON file.

That file includes:

- route name
- start pose
- preset start key if one is being used
- robot size
- field-image opacity
- optional embedded field image
- all straight and curved blocks

This is meant for iteration in the simulator before code is finalized.

Recommended loop:

1. Lay out the route in the designer.
2. Save a route JSON file.
3. Reopen that JSON later to keep editing.
4. Copy the generated Java only when the route shape is ready to move into robot code.

Because the optional field image is embedded, these route JSON files can get fairly large. That is expected if you save a workspace with a reference image included.

## Creating or patching auto files

The route designer can also talk to the local simulator backend and write Java files in `pedroPathing`.

There are two modes:

1. Create Auto
2. Patch `@path`

### Create Auto

This creates a new autonomous OpMode file in the Pedro package using the currently drawn route.

The generated file includes a tagged route section:

```java
// @path-start
...
// @path-end
```

That makes it easy to update later without rewriting the rest of the OpMode.

### Patch `@path`

This updates an existing `.java` auto file in the Pedro package.

Supported marker styles:

1. A block between `// @path-start` and `// @path-end`
2. A single line containing `@path`

If the file contains a tagged block, only the route section is replaced. That is the preferred format for repeat editing.

## Robot sizing

The designer lets you set robot size in inches.

Use the real bumper-to-bumper or frame size you care about for path clearance. The outline is only a 2D planning aid, but it is enough to catch obvious route shapes that would clip into protected areas, walls, or staging zones.

## Typical workflow

1. Choose the start pose.
2. Set the robot size.
3. Load a field reference image if needed.
4. Add a straight first block out of the start area.
5. Add curved blocks where direction changes matter.
6. Adjust endpoint headings so the robot arrives mechanism-ready.
7. Save the route JSON so the visual draft is preserved.
8. Either save the route JSON, create a new auto file, or patch an existing tagged auto.
9. Run the same route in sim before moving to the robot.

## Related files

- `PedroStartPose.java`
- `PedroBlockCommand.java`
- `PedroBlockRouteBuilder.java`
- `PedroDecodeRoute.java`
- `PedroUnifiedArchitecture.md`
