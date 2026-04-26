import { useEffect, useMemo, useRef, useState } from 'react';
import { Copy, Download, FolderOpen, ImagePlus, Pause, Play, Plus, RefreshCcw, Trash2 } from 'lucide-react';

type BlockType = 'straight' | 'curved';

interface RouteBlock {
  id: string;
  type: BlockType;
  endXIn: number;
  endYIn: number;
  endHeadingDeg: number;
  controlXIn?: number;
  controlYIn?: number;
  controlHeadingDeg?: number;
  controlScale: number;
  headingWeight: number;
}

interface StartPoseOption {
  key: string;
  label: string;
  xIn: number;
  yIn: number;
  headingDeg: number;
}

interface PoseLike {
  xIn: number;
  yIn: number;
  headingDeg: number;
}

interface StoredRouteV1 {
  version: 1;
  routeName: string;
  start: PoseLike & { presetKey?: string | null };
  robotSizeIn: number;
  fieldImageOpacity: number;
  fieldImageDataUrl?: string | null;
  blocks: RouteBlock[];
}

interface StoredRouteV2 {
  version: 2;
  routeName: string;
  start: PoseLike & { presetKey?: string | null };
  robotSizeIn: number;
  fieldImageOpacity: number;
  fieldImageDataUrl?: string | null;
  blocks: RouteBlock[];
  selectedAutoFile?: string;
  showRobotSilhouettes?: boolean;
}

interface SampledPose extends PoseLike {
  segmentId?: string;
}

type DragTarget =
  | { type: 'start' }
  | { type: 'startHeading' }
  | { type: 'end'; id: string }
  | { type: 'endHeading'; id: string }
  | { type: 'control'; id: string };

const FIELD_HALF_IN = 72;
const ROBOT_SIZE_DEFAULT_IN = 16;
const HEADING_HANDLE_DISTANCE_IN = 11;
const FIELD_VIEWBOX_PADDING = 8;
const TILE_SIZE_IN = 24;
const SNAP_THRESHOLD_IN = 3;
const ROUTE_DESIGNER_STORAGE_KEY = 'swerve-scope.route-designer.v2';

const startOptions: StartPoseOption[] = [
  { key: 'RED_BASE_CORNER', label: 'Red Base Corner', xIn: -60, yIn: -60, headingDeg: 0 },
  { key: 'RED_BASE_CENTER', label: 'Red Base Center', xIn: -48, yIn: -60, headingDeg: 0 },
  { key: 'BLUE_BASE_CORNER', label: 'Blue Base Corner', xIn: -60, yIn: 60, headingDeg: 0 },
  { key: 'BLUE_BASE_CENTER', label: 'Blue Base Center', xIn: -48, yIn: 60, headingDeg: 0 },
];

const initialBlocks: RouteBlock[] = [
  { id: 'b1', type: 'straight', endXIn: -30, endYIn: -60, endHeadingDeg: 0, controlScale: 1, headingWeight: 1 },
  { id: 'b2', type: 'curved', endXIn: 18, endYIn: -28, endHeadingDeg: 32, controlXIn: -8, controlYIn: -46, controlHeadingDeg: 0, controlScale: 1, headingWeight: 0.8 },
  { id: 'b3', type: 'straight', endXIn: 28, endYIn: -6, endHeadingDeg: 90, controlScale: 1, headingWeight: 0.9 },
  { id: 'b4', type: 'curved', endXIn: -52, endYIn: -44, endHeadingDeg: 180, controlXIn: -10, controlYIn: -8, controlHeadingDeg: 135, controlScale: 1, headingWeight: 0.9 },
];

function cloneInitialBlocks() {
  return initialBlocks.map((block, index) => ({
    ...block,
    id: `b${Date.now()}-${index}`,
  }));
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value));
}

function fmt(value: number) {
  return Number(value.toFixed(2));
}

function normalizeDeg(angleDeg: number) {
  let angle = angleDeg % 360;
  if (angle < 0) angle += 360;
  return angle;
}

function headingFromPoint(origin: PoseLike, targetXIn: number, targetYIn: number) {
  return normalizeDeg((Math.atan2(targetYIn - origin.yIn, targetXIn - origin.xIn) * 180) / Math.PI);
}

function samePose(a: PoseLike, b: PoseLike) {
  return Math.abs(a.xIn - b.xIn) < 0.01
    && Math.abs(a.yIn - b.yIn) < 0.01
    && Math.abs(normalizeDeg(a.headingDeg) - normalizeDeg(b.headingDeg)) < 0.01;
}

function headingHandlePosition(pose: PoseLike) {
  const headingRad = (pose.headingDeg * Math.PI) / 180;
  return {
    xIn: pose.xIn + Math.cos(headingRad) * HEADING_HANDLE_DISTANCE_IN,
    yIn: pose.yIn + Math.sin(headingRad) * HEADING_HANDLE_DISTANCE_IN,
  };
}

export function RouteDesigner() {
  const fieldImageInputRef = useRef<HTMLInputElement>(null);
  const routeFileInputRef = useRef<HTMLInputElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const [selectedStart, setSelectedStart] = useState<StartPoseOption>(startOptions[0]);
  const [blocks, setBlocks] = useState<RouteBlock[]>(() => cloneInitialBlocks());
  const [dragTarget, setDragTarget] = useState<DragTarget | null>(null);
  const [robotSizeIn, setRobotSizeIn] = useState(ROBOT_SIZE_DEFAULT_IN);
  const [fieldImageDataUrl, setFieldImageDataUrl] = useState<string | null>(null);
  const [fieldImageOpacity, setFieldImageOpacity] = useState(0.45);
  const [routeName, setRouteName] = useState('decode-lane');
  const [existingAutoFiles, setExistingAutoFiles] = useState<string[]>([]);
  const [selectedAutoFile, setSelectedAutoFile] = useState('');
  const [automationStatus, setAutomationStatus] = useState('');
  const [isPreviewPlaying, setIsPreviewPlaying] = useState(false);
  const [previewDistance, setPreviewDistance] = useState(0);
  const [snapToIntersections, setSnapToIntersections] = useState(true);
  const [snapToTileCenters, setSnapToTileCenters] = useState(false);
  const [snapToMidpoints, setSnapToMidpoints] = useState(false);
  const [showRobotSilhouettes, setShowRobotSilhouettes] = useState(true);

  const presetMatch = useMemo(
    () => startOptions.find(option => samePose(option, selectedStart)) ?? null,
    [selectedStart],
  );

  const previewSegments = useMemo(() => {
    const out: Array<{
      id: string;
      start: PoseLike;
      end: PoseLike;
      control?: { xIn: number; yIn: number; headingDeg: number };
      type: BlockType;
    }> = [];

    let current: PoseLike = {
      xIn: selectedStart.xIn,
      yIn: selectedStart.yIn,
      headingDeg: selectedStart.headingDeg,
    };

    for (const block of blocks) {
      const end = {
        xIn: block.endXIn,
        yIn: block.endYIn,
        headingDeg: block.endHeadingDeg,
      };
      out.push({
        id: block.id,
        start: current,
        end,
        control: block.type === 'curved'
          ? {
              xIn: block.controlXIn ?? ((current.xIn + end.xIn) * 0.5),
              yIn: block.controlYIn ?? ((current.yIn + end.yIn) * 0.5),
              headingDeg: block.controlHeadingDeg ?? current.headingDeg,
            }
          : undefined,
        type: block.type,
      });
      current = end;
    }

    return out;
  }, [blocks, selectedStart]);

  const routeSamples = useMemo(() => {
    const samples: Array<{
      distanceStart: number;
      distanceEnd: number;
      pointAt: (t: number) => { xIn: number; yIn: number };
      headingAt: (t: number) => number;
      segmentId: string;
    }> = [];

    let totalDistance = 0;
    for (const segment of previewSegments) {
      const control = segment.control;
      const pointAt = (t: number) => {
        if (!control || segment.type === 'straight') {
          return {
            xIn: segment.start.xIn + (segment.end.xIn - segment.start.xIn) * t,
            yIn: segment.start.yIn + (segment.end.yIn - segment.start.yIn) * t,
          };
        }

        const oneMinusT = 1 - t;
        return {
          xIn: oneMinusT * oneMinusT * segment.start.xIn + 2 * oneMinusT * t * control.xIn + t * t * segment.end.xIn,
          yIn: oneMinusT * oneMinusT * segment.start.yIn + 2 * oneMinusT * t * control.yIn + t * t * segment.end.yIn,
        };
      };

      const headingAt = (t: number) => normalizeDeg(segment.start.headingDeg + (segment.end.headingDeg - segment.start.headingDeg) * t);

      let length = 0;
      let previous = pointAt(0);
      const subdivisions = segment.type === 'curved' ? 40 : 2;
      for (let i = 1; i <= subdivisions; i++) {
        const current = pointAt(i / subdivisions);
        length += Math.hypot(current.xIn - previous.xIn, current.yIn - previous.yIn);
        previous = current;
      }

      samples.push({
        distanceStart: totalDistance,
        distanceEnd: totalDistance + length,
        pointAt,
        headingAt,
        segmentId: segment.id,
      });
      totalDistance += length;
    }

    return {
      segments: samples,
      totalDistance,
    };
  }, [previewSegments]);

  const previewPose = useMemo<SampledPose | null>(() => {
    if (routeSamples.totalDistance <= 0 || routeSamples.segments.length === 0) {
      return null;
    }

    const clampedDistance = clamp(previewDistance, 0, routeSamples.totalDistance);
    const activeSegment = routeSamples.segments.find(segment => clampedDistance <= segment.distanceEnd) ?? routeSamples.segments[routeSamples.segments.length - 1];
    const segmentDistance = activeSegment.distanceEnd - activeSegment.distanceStart;
    const t = segmentDistance <= 1e-6 ? 1 : (clampedDistance - activeSegment.distanceStart) / segmentDistance;
    const point = activeSegment.pointAt(clamp(t, 0, 1));
    return {
      xIn: point.xIn,
      yIn: point.yIn,
      headingDeg: activeSegment.headingAt(clamp(t, 0, 1)),
      segmentId: activeSegment.segmentId,
    };
  }, [previewDistance, routeSamples]);

  const generatedCode = useMemo(() => {
    const startPoseLine = presetMatch
      ? `Pose startPose = PedroDecodeRoute.startPose(PedroStartPose.${presetMatch.key});`
      : `Pose startPose = PedroStartPose.custom(${fmt(selectedStart.xIn)}, ${fmt(selectedStart.yIn)}, ${fmt(selectedStart.headingDeg)});`;

    return [
      startPoseLine,
      'PathChain route = PedroBlockRouteBuilder.build(',
      '        follower,',
      '        startPose,',
      ...blocks.map((block, index) => {
        const suffix = index === blocks.length - 1 ? '' : ',';
        if (block.type === 'straight') {
          return `        PedroBlockCommand.straight(${fmt(block.endXIn)}, ${fmt(block.endYIn)}, ${fmt(block.endHeadingDeg)}, ${fmt(block.headingWeight)})${suffix}`;
        }

        return `        PedroBlockCommand.curved(${fmt(block.endXIn)}, ${fmt(block.endYIn)}, ${fmt(block.endHeadingDeg)}, ${fmt(block.controlXIn ?? 0)}, ${fmt(block.controlYIn ?? 0)}, ${fmt(block.controlHeadingDeg ?? block.endHeadingDeg)}, ${fmt(block.controlScale)}, ${fmt(block.headingWeight)})${suffix}`;
      }),
      ');',
    ].join('\n');
  }, [blocks, presetMatch, selectedStart]);

  const className = useMemo(() => {
    const base = (routeName.trim() || 'generated-auto')
      .replace(/[^A-Za-z0-9]+/g, ' ')
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join('');
    return `${base || 'Generated'}Auto`;
  }, [routeName]);

  const opModeName = useMemo(() => {
    const trimmed = routeName.trim();
    return trimmed ? `${trimmed} Auto` : 'Generated Auto';
  }, [routeName]);

  const toSvg = (xIn: number, yIn: number) => ({
    x: ((xIn + FIELD_HALF_IN) / (FIELD_HALF_IN * 2)) * 100,
    y: ((FIELD_HALF_IN - yIn) / (FIELD_HALF_IN * 2)) * 100,
  });

  const fromSvg = (xPct: number, yPct: number) => ({
    xIn: clamp(((xPct / 100) * FIELD_HALF_IN * 2) - FIELD_HALF_IN, -FIELD_HALF_IN, FIELD_HALF_IN),
    yIn: clamp(FIELD_HALF_IN - ((yPct / 100) * FIELD_HALF_IN * 2), -FIELD_HALF_IN, FIELD_HALF_IN),
  });

  const snapValue = (value: number, step: number, offset: number) => offset + Math.round((value - offset) / step) * step;

  const applySnapping = (xIn: number, yIn: number) => {
    const candidates: Array<{ xIn: number; yIn: number }> = [];

    if (snapToIntersections) {
      candidates.push({
        xIn: snapValue(xIn, TILE_SIZE_IN, 0),
        yIn: snapValue(yIn, TILE_SIZE_IN, 0),
      });
    }

    if (snapToTileCenters) {
      candidates.push({
        xIn: snapValue(xIn, TILE_SIZE_IN, TILE_SIZE_IN / 2),
        yIn: snapValue(yIn, TILE_SIZE_IN, TILE_SIZE_IN / 2),
      });
    }

    if (snapToMidpoints) {
      candidates.push(
        {
          xIn: snapValue(xIn, TILE_SIZE_IN, TILE_SIZE_IN / 2),
          yIn: snapValue(yIn, TILE_SIZE_IN, 0),
        },
        {
          xIn: snapValue(xIn, TILE_SIZE_IN, 0),
          yIn: snapValue(yIn, TILE_SIZE_IN, TILE_SIZE_IN / 2),
        },
      );
    }

    let best = { xIn, yIn };
    let bestDistance = SNAP_THRESHOLD_IN;
    for (const candidate of candidates) {
      const distance = Math.hypot(candidate.xIn - xIn, candidate.yIn - yIn);
      if (distance <= bestDistance) {
        best = candidate;
        bestDistance = distance;
      }
    }

    return {
      xIn: clamp(best.xIn, -FIELD_HALF_IN, FIELD_HALF_IN),
      yIn: clamp(best.yIn, -FIELD_HALF_IN, FIELD_HALF_IN),
    };
  };

  const pointerToField = (event: React.PointerEvent<SVGSVGElement>) => {
    const svg = svgRef.current;
    if (!svg) {
      return { xIn: 0, yIn: 0 };
    }

    const point = svg.createSVGPoint();
    point.x = event.clientX;
    point.y = event.clientY;
    const ctm = svg.getScreenCTM();
    if (!ctm) {
      return { xIn: 0, yIn: 0 };
    }

    const local = point.matrixTransform(ctm.inverse());
    const xPct = clamp(local.x, 0, 100);
    const yPct = clamp(local.y, 0, 100);
    const raw = fromSvg(xPct, yPct);
    return applySnapping(raw.xIn, raw.yIn);
  };

  const robotSizePct = (robotSizeIn / (FIELD_HALF_IN * 2)) * 100;

  useEffect(() => {
    if (!isPreviewPlaying) return;
    if (routeSamples.totalDistance <= 0) {
      setIsPreviewPlaying(false);
      return;
    }

    let animationFrame = 0;
    let lastTime = performance.now();
    const speedInPerSec = 36;

    const tick = (now: number) => {
      const dtSec = (now - lastTime) / 1000;
      lastTime = now;
      setPreviewDistance(prev => {
        const next = prev + speedInPerSec * dtSec;
        if (next >= routeSamples.totalDistance) {
          setIsPreviewPlaying(false);
          return routeSamples.totalDistance;
        }
        return next;
      });
      animationFrame = requestAnimationFrame(tick);
    };

    animationFrame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(animationFrame);
  }, [isPreviewPlaying, routeSamples.totalDistance]);

  const updateBlock = (id: string, patch: Partial<RouteBlock>) => {
    setBlocks(prev => prev.map(block => (block.id === id ? { ...block, ...patch } : block)));
  };

  const beginDrag = (event: React.PointerEvent<SVGElement>, target: DragTarget) => {
    event.preventDefault();
    event.stopPropagation();
    svgRef.current?.setPointerCapture(event.pointerId);
    setDragTarget(target);
  };

  const updateHeadingFromPointer = (event: React.PointerEvent<SVGSVGElement>, pose: PoseLike, apply: (headingDeg: number) => void) => {
    const { xIn, yIn } = pointerToField(event);
    apply(headingFromPoint(pose, xIn, yIn));
  };

  const handlePointerMove = (event: React.PointerEvent<SVGSVGElement>) => {
    if (!dragTarget) return;
    const { xIn, yIn } = pointerToField(event);

    if (dragTarget.type === 'start') {
      setSelectedStart(prev => ({ ...prev, xIn, yIn }));
      return;
    }

    if (dragTarget.type === 'startHeading') {
      updateHeadingFromPointer(event, selectedStart, headingDeg => {
        setSelectedStart(prev => ({ ...prev, headingDeg }));
      });
      return;
    }

    if (dragTarget.type === 'end') {
      updateBlock(dragTarget.id, { endXIn: xIn, endYIn: yIn });
      return;
    }

    if (dragTarget.type === 'endHeading') {
      const segment = previewSegments.find(candidate => candidate.id === dragTarget.id);
      if (!segment) return;
      updateHeadingFromPointer(event, segment.end, headingDeg => {
        updateBlock(dragTarget.id, { endHeadingDeg: headingDeg });
      });
      return;
    }

    updateBlock(dragTarget.id, { controlXIn: xIn, controlYIn: yIn });
  };

  const addBlock = (type: BlockType) => {
    const last = blocks[blocks.length - 1];
    const endX = last?.endXIn ?? selectedStart.xIn;
    const endY = last?.endYIn ?? selectedStart.yIn;
    const endHeading = last?.endHeadingDeg ?? selectedStart.headingDeg;

    setBlocks(prev => [
      ...prev,
      {
        id: `b${Date.now()}`,
        type,
        endXIn: clamp(endX + 24, -FIELD_HALF_IN, FIELD_HALF_IN),
        endYIn: endY,
        endHeadingDeg: endHeading,
        controlXIn: type === 'curved' ? clamp(endX + 12, -FIELD_HALF_IN, FIELD_HALF_IN) : undefined,
        controlYIn: type === 'curved' ? clamp(endY + 12, -FIELD_HALF_IN, FIELD_HALF_IN) : undefined,
        controlHeadingDeg: type === 'curved' ? endHeading : undefined,
        controlScale: 1,
        headingWeight: 0.9,
      },
    ]);
  };

  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(generatedCode);
    } catch {
      // ignore clipboard failures
    }
  };

  const loadFieldImage = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setFieldImageDataUrl(reader.result);
      }
    };
    reader.readAsDataURL(file);
  };

  const refreshAutoFiles = async () => {
    try {
      const response = await fetch(`http://${window.location.hostname || 'localhost'}:8080/api/pedro/autos`);
      if (!response.ok) {
        return;
      }

      const files = await response.json() as string[];
      setExistingAutoFiles(files);
      setSelectedAutoFile(prev => {
        if (prev && files.includes(prev)) {
          return prev;
        }

        const savedRaw = window.localStorage.getItem(ROUTE_DESIGNER_STORAGE_KEY);
        if (savedRaw) {
          try {
            const saved = JSON.parse(savedRaw) as Partial<StoredRouteV2>;
            if (saved.selectedAutoFile && files.includes(saved.selectedAutoFile)) {
              return saved.selectedAutoFile;
            }
          } catch {
            // ignore broken local state
          }
        }

        return files[0] ?? '';
      });
    } catch {
      // leave empty if backend is unavailable
    }
  };

  const reloadSimAutos = async (options?: { silent?: boolean; successPrefix?: string }) => {
    if (!options?.silent) {
      setAutomationStatus('Reloading sim autos...');
    }
    try {
      const response = await fetch(`http://${window.location.hostname || 'localhost'}:8080/api/pedro/autos/reload`, {
        method: 'POST',
      });
      if (!response.ok) {
        if (!options?.silent) {
          setAutomationStatus('Failed to reload sim autos');
        }
        return;
      }

      const simOpModes = await response.json() as string[];
      await refreshAutoFiles();
      if (!options?.silent) {
        const prefix = options?.successPrefix ? `${options.successPrefix} ` : '';
        setAutomationStatus(`${prefix}Reloaded ${simOpModes.length} sim autos`);
      }
    } catch {
      if (!options?.silent) {
        setAutomationStatus('Failed to reload sim autos');
      }
    }
  };

  useEffect(() => {
    refreshAutoFiles();
  }, []);

  useEffect(() => {
    const savedRaw = window.localStorage.getItem(ROUTE_DESIGNER_STORAGE_KEY);
    if (!savedRaw) {
      return;
    }

    try {
      const parsed = JSON.parse(savedRaw) as Partial<StoredRouteV1 | StoredRouteV2>;
      if (!parsed.start || !Array.isArray(parsed.blocks)) {
        return;
      }

      const preset = parsed.start.presetKey
        ? startOptions.find(option => option.key === parsed.start?.presetKey)
        : null;

      setRouteName(parsed.routeName?.trim() || 'decode-lane');
      setSelectedStart(preset ?? {
        key: 'CUSTOM',
        label: 'Custom Start',
        xIn: Number(parsed.start.xIn ?? startOptions[0].xIn),
        yIn: Number(parsed.start.yIn ?? startOptions[0].yIn),
        headingDeg: normalizeDeg(Number(parsed.start.headingDeg ?? startOptions[0].headingDeg)),
      });
      setRobotSizeIn(clamp(Number(parsed.robotSizeIn ?? ROBOT_SIZE_DEFAULT_IN), 8, 30));
      setFieldImageOpacity(clamp(Number(parsed.fieldImageOpacity ?? 0.45), 0, 1));
      setFieldImageDataUrl(parsed.fieldImageDataUrl ?? null);
      setBlocks(parsed.blocks.map((block, index) => ({
        id: block.id || `b${Date.now()}-${index}`,
        type: block.type === 'curved' ? 'curved' : 'straight',
        endXIn: Number(block.endXIn ?? 0),
        endYIn: Number(block.endYIn ?? 0),
        endHeadingDeg: normalizeDeg(Number(block.endHeadingDeg ?? 0)),
        controlXIn: block.controlXIn !== undefined ? Number(block.controlXIn) : undefined,
        controlYIn: block.controlYIn !== undefined ? Number(block.controlYIn) : undefined,
        controlHeadingDeg: block.controlHeadingDeg !== undefined ? normalizeDeg(Number(block.controlHeadingDeg)) : undefined,
        controlScale: Number(block.controlScale ?? 1),
        headingWeight: Number(block.headingWeight ?? 1),
      })));
      const parsedV2 = parsed as Partial<StoredRouteV2>;
      setSelectedAutoFile(typeof parsedV2.selectedAutoFile === 'string' ? parsedV2.selectedAutoFile : '');
      setShowRobotSilhouettes(parsedV2.showRobotSilhouettes !== false);
    } catch {
      // ignore malformed local route state
    }
  }, []);

  useEffect(() => {
    const payload: StoredRouteV2 = {
      version: 2,
      routeName: routeName.trim() || 'decode-lane',
      start: {
        xIn: selectedStart.xIn,
        yIn: selectedStart.yIn,
        headingDeg: selectedStart.headingDeg,
        presetKey: presetMatch?.key ?? null,
      },
      robotSizeIn,
      fieldImageOpacity,
      fieldImageDataUrl,
      blocks,
      selectedAutoFile,
      showRobotSilhouettes,
    };
    window.localStorage.setItem(ROUTE_DESIGNER_STORAGE_KEY, JSON.stringify(payload));
  }, [blocks, fieldImageDataUrl, fieldImageOpacity, presetMatch, robotSizeIn, routeName, selectedAutoFile, selectedStart, showRobotSilhouettes]);

  const createAutoFile = async () => {
    setAutomationStatus('Creating auto file...');
    try {
      const response = await fetch(`http://${window.location.hostname || 'localhost'}:8080/api/pedro/route/create`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          className,
          opModeName,
          routeCode: generatedCode,
        }),
      });
      const result = await response.json() as { ok?: boolean; message?: string; filePath?: string };
      await refreshAutoFiles();
      if (result.ok) {
        setSelectedAutoFile(`${className}.java`);
        await reloadSimAutos({
          silent: true,
        });
        setAutomationStatus(result.message || (result.filePath ? `Created ${className}.java at ${result.filePath}` : 'Created auto file'));
      } else {
        setAutomationStatus(result.message || 'Failed to create auto file');
      }
    } catch {
      setAutomationStatus('Failed to create auto file');
    }
  };

  const deleteSelectedAuto = async () => {
    if (!selectedAutoFile) {
      setAutomationStatus('Choose an auto file to delete');
      return;
    }

    setAutomationStatus(`Deleting ${selectedAutoFile}...`);
    try {
      const response = await fetch(`http://${window.location.hostname || 'localhost'}:8080/api/pedro/route/delete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          targetFileName: selectedAutoFile,
        }),
      });
      const result = await response.json() as { ok?: boolean; message?: string };
      if (result.ok) {
        const deletedFile = selectedAutoFile;
        setSelectedAutoFile('');
        await refreshAutoFiles();
        await reloadSimAutos({ silent: true });
        setAutomationStatus(result.message || `Deleted ${deletedFile}`);
      } else {
        setAutomationStatus(result.message || 'Failed to delete auto file');
      }
    } catch {
      setAutomationStatus('Failed to delete auto file');
    }
  };

  const patchExistingAuto = async () => {
    if (!selectedAutoFile) {
      setAutomationStatus('Choose an existing auto file first');
      return;
    }

    setAutomationStatus(`Updating ${selectedAutoFile}...`);
    try {
      const response = await fetch(`http://${window.location.hostname || 'localhost'}:8080/api/pedro/route/patch`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          targetFileName: selectedAutoFile,
          routeCode: generatedCode,
        }),
      });
      const result = await response.json() as { ok?: boolean; message?: string };
      await refreshAutoFiles();
      if (result.ok) {
        await reloadSimAutos({
          silent: true,
        });
        setAutomationStatus(result.message || 'Updated auto file');
      } else {
        setAutomationStatus(result.message || 'Failed to update auto file');
      }
    } catch {
      setAutomationStatus('Failed to update auto file');
    }
  };

  const saveRouteJson = () => {
    const payload: StoredRouteV2 = {
      version: 2,
      routeName: routeName.trim() || 'route',
      start: {
        xIn: selectedStart.xIn,
        yIn: selectedStart.yIn,
        headingDeg: selectedStart.headingDeg,
        presetKey: presetMatch?.key ?? null,
      },
      robotSizeIn,
      fieldImageOpacity,
      fieldImageDataUrl,
      blocks,
      selectedAutoFile,
      showRobotSilhouettes,
    };

    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${(routeName.trim() || 'route').replace(/\s+/g, '-').toLowerCase()}.route.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const loadRouteJson = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      const text = await file.text();
      const parsed = JSON.parse(text) as Partial<StoredRouteV1 | StoredRouteV2>;
      if ((parsed.version !== 1 && parsed.version !== 2) || !parsed.start || !Array.isArray(parsed.blocks)) {
        return;
      }

      const preset = parsed.start.presetKey
        ? startOptions.find(option => option.key === parsed.start?.presetKey)
        : null;

      setRouteName(parsed.routeName?.trim() || file.name.replace(/\.route\.json$/i, '').replace(/\.json$/i, ''));
      setSelectedStart(preset ?? {
        key: 'CUSTOM',
        label: 'Custom Start',
        xIn: Number(parsed.start.xIn ?? 0),
        yIn: Number(parsed.start.yIn ?? 0),
        headingDeg: normalizeDeg(Number(parsed.start.headingDeg ?? 0)),
      });
      setRobotSizeIn(clamp(Number(parsed.robotSizeIn ?? ROBOT_SIZE_DEFAULT_IN), 8, 30));
      setFieldImageOpacity(clamp(Number(parsed.fieldImageOpacity ?? 0.45), 0, 1));
      setFieldImageDataUrl(parsed.fieldImageDataUrl ?? null);
      const parsedV2 = parsed as Partial<StoredRouteV2>;
      setSelectedAutoFile(typeof parsedV2.selectedAutoFile === 'string' ? parsedV2.selectedAutoFile : '');
      setShowRobotSilhouettes(parsedV2.showRobotSilhouettes !== false);
      setBlocks(parsed.blocks.map((block, index) => ({
        id: block.id || `b${Date.now()}-${index}`,
        type: block.type === 'curved' ? 'curved' : 'straight',
        endXIn: Number(block.endXIn ?? 0),
        endYIn: Number(block.endYIn ?? 0),
        endHeadingDeg: normalizeDeg(Number(block.endHeadingDeg ?? 0)),
        controlXIn: block.controlXIn !== undefined ? Number(block.controlXIn) : undefined,
        controlYIn: block.controlYIn !== undefined ? Number(block.controlYIn) : undefined,
        controlHeadingDeg: block.controlHeadingDeg !== undefined ? normalizeDeg(Number(block.controlHeadingDeg)) : undefined,
        controlScale: Number(block.controlScale ?? 1),
        headingWeight: Number(block.headingWeight ?? 1),
      })));
    } catch {
      // ignore malformed route files for now
    } finally {
      event.target.value = '';
    }
  };

  const clearPath = () => {
    setBlocks([]);
    setPreviewDistance(0);
    setIsPreviewPlaying(false);
    setAutomationStatus('Cleared route blocks');
  };

  const resetWorkspace = () => {
    setSelectedStart(startOptions[0]);
    setBlocks(cloneInitialBlocks());
    setRobotSizeIn(ROBOT_SIZE_DEFAULT_IN);
    setFieldImageDataUrl(null);
    setFieldImageOpacity(0.45);
    setRouteName('decode-lane');
    setSelectedAutoFile('');
    setDragTarget(null);
    setPreviewDistance(0);
    setIsPreviewPlaying(false);
    setShowRobotSilhouettes(true);
    setAutomationStatus('Reset route workspace');
  };

  const startHandle = headingHandlePosition(selectedStart);
  const startSvg = toSvg(selectedStart.xIn, selectedStart.yIn);
  const startHeadingSvg = toSvg(startHandle.xIn, startHandle.yIn);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: '0.5rem', padding: '0.5rem' }}>
      <div className="glass-card" style={{ padding: '0.5rem', display: 'grid', gap: '0.5rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <input
            value={routeName}
            onChange={(event) => setRouteName(event.target.value)}
            style={{ ...inputStyle, width: 150 }}
            placeholder="Route name"
          />
          <select
            value={presetMatch?.key ?? selectedStart.key}
            onChange={(event) => {
              const found = startOptions.find(option => option.key === event.target.value);
              if (found) setSelectedStart(found);
            }}
            style={selectStyle}
          >
            {startOptions.map(option => (
              <option key={option.key} value={option.key}>{option.label}</option>
            ))}
          </select>
          <button onClick={() => addBlock('straight')} style={btnStyle}><Plus size={12} /> Straight</button>
          <button onClick={() => addBlock('curved')} style={btnStyle}><Plus size={12} /> Curved</button>
          <button onClick={copyCode} style={btnStyle}><Copy size={12} /> Copy Code</button>
          <button onClick={saveRouteJson} style={btnStyle}><Download size={12} /> Save Route</button>
          <button onClick={() => routeFileInputRef.current?.click()} style={btnStyle}><FolderOpen size={12} /> Load Route</button>
          <button onClick={createAutoFile} style={btnStyle}><Download size={12} /> Create Auto</button>
          <button onClick={patchExistingAuto} style={btnStyle}><FolderOpen size={12} /> Patch @path</button>
          <button onClick={deleteSelectedAuto} style={btnStyle}><Trash2 size={12} /> Delete Auto</button>
          <button onClick={() => { void reloadSimAutos(); }} style={btnStyle}><RefreshCcw size={12} /> Reload Sim Autos</button>
          <button onClick={() => fieldImageInputRef.current?.click()} style={btnStyle}><ImagePlus size={12} /> Field Image</button>
          <button onClick={() => {
            if (routeSamples.totalDistance <= 0) return;
            if (previewDistance >= routeSamples.totalDistance) {
              setPreviewDistance(0);
            }
            setIsPreviewPlaying(prev => !prev);
          }} style={btnStyle}>
            {isPreviewPlaying ? <Pause size={12} /> : <Play size={12} />} Preview
          </button>
          <button onClick={clearPath} style={btnStyle}><Trash2 size={12} /> Clear Path</button>
          <button onClick={resetWorkspace} style={btnStyle}><Trash2 size={12} /> Reset Workspace</button>
          <input ref={routeFileInputRef} type="file" accept=".json,.route.json" onChange={loadRouteJson} style={{ display: 'none' }} />
          <input ref={fieldImageInputRef} type="file" accept="image/*" onChange={loadFieldImage} style={{ display: 'none' }} />
          {fieldImageDataUrl && (
            <button onClick={() => setFieldImageDataUrl(null)} style={btnStyle}><Trash2 size={12} /> Clear Image</button>
          )}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(6, minmax(0, 1fr))', gap: '0.4rem' }}>
          <label style={labelStyle}>
            <span>Start X</span>
            <input value={selectedStart.xIn} onChange={(e) => setSelectedStart(prev => ({ ...prev, xIn: Number(e.target.value) }))} style={inputStyle} />
          </label>
          <label style={labelStyle}>
            <span>Start Y</span>
            <input value={selectedStart.yIn} onChange={(e) => setSelectedStart(prev => ({ ...prev, yIn: Number(e.target.value) }))} style={inputStyle} />
          </label>
          <label style={labelStyle}>
            <span>Start Heading</span>
            <input value={selectedStart.headingDeg} onChange={(e) => setSelectedStart(prev => ({ ...prev, headingDeg: normalizeDeg(Number(e.target.value)) }))} style={inputStyle} />
          </label>
          <label style={labelStyle}>
            <span>Robot Size</span>
            <input value={robotSizeIn} onChange={(e) => setRobotSizeIn(clamp(Number(e.target.value), 8, 30))} style={inputStyle} />
          </label>
          <label style={labelStyle}>
            <span>Image Opacity</span>
            <input type="range" min={0} max={1} step={0.05} value={fieldImageOpacity} onChange={(e) => setFieldImageOpacity(Number(e.target.value))} />
          </label>
          <label style={labelStyle}>
            <span>Snap Grid</span>
            <div style={{ display: 'flex', gap: '0.45rem', flexWrap: 'wrap', alignItems: 'center' }}>
              <label style={toggleLabel}><input type="checkbox" checked={snapToIntersections} onChange={(e) => setSnapToIntersections(e.target.checked)} /> Intersections</label>
              <label style={toggleLabel}><input type="checkbox" checked={snapToTileCenters} onChange={(e) => setSnapToTileCenters(e.target.checked)} /> Tile Centers</label>
              <label style={toggleLabel}><input type="checkbox" checked={snapToMidpoints} onChange={(e) => setSnapToMidpoints(e.target.checked)} /> Midpoints</label>
              <label style={toggleLabel}><input type="checkbox" checked={showRobotSilhouettes} onChange={(e) => setShowRobotSilhouettes(e.target.checked)} /> Robot</label>
            </div>
          </label>
          <label style={labelStyle}>
            <span>Existing Auto</span>
            <select value={selectedAutoFile} onChange={(event) => setSelectedAutoFile(event.target.value)} style={autoSelectStyle}>
              <option value="">Choose file</option>
              {existingAutoFiles.map(file => (
                <option key={file} value={file}>{file}</option>
              ))}
            </select>
          </label>
          <div style={{ ...labelStyle, justifyContent: 'flex-end' }}>
            <span style={{ color: 'var(--text-dim)', fontSize: '0.63rem' }}>
              {presetMatch ? `Preset ${presetMatch.label}` : 'Custom start pose'}
            </span>
            <span style={{ color: 'var(--text-dim)', fontSize: '0.63rem' }}>
              {className}
            </span>
            <span style={{ color: 'var(--accent)', fontSize: '0.63rem' }}>
              {automationStatus}
            </span>
          </div>
        </div>
      </div>

      <div className="glass-card" style={{ flex: 1, minHeight: 300, position: 'relative', overflow: 'hidden' }}>
        <svg
          ref={svgRef}
          viewBox={`${-FIELD_VIEWBOX_PADDING} ${-FIELD_VIEWBOX_PADDING} ${100 + FIELD_VIEWBOX_PADDING * 2} ${100 + FIELD_VIEWBOX_PADDING * 2}`}
          style={{ width: '100%', height: '100%', display: 'block', background: 'rgba(17, 21, 31, 0.96)' }}
          onPointerMove={handlePointerMove}
          onPointerUp={(event) => {
            if (svgRef.current?.hasPointerCapture(event.pointerId)) {
              svgRef.current.releasePointerCapture(event.pointerId);
            }
            setDragTarget(null);
          }}
          onPointerLeave={() => setDragTarget(null)}
        >
          <rect x="0" y="0" width="100" height="100" fill="rgba(17, 21, 31, 0.96)" />
          {fieldImageDataUrl && (
            <image href={fieldImageDataUrl} x="0" y="0" width="100" height="100" preserveAspectRatio="none" opacity={fieldImageOpacity} />
          )}

          {[0, 16.67, 33.33, 50, 66.67, 83.33, 100].map(p => (
            <g key={`grid-${p}`}>
              <line x1={p} y1="0" x2={p} y2="100" stroke="rgba(255,255,255,0.08)" strokeWidth="0.25" />
              <line x1="0" y1={p} x2="100" y2={p} stroke="rgba(255,255,255,0.08)" strokeWidth="0.25" />
            </g>
          ))}
          <rect x="0" y="0" width="100" height="100" fill="none" stroke="rgba(255,255,255,0.14)" strokeWidth="0.35" />

          {previewSegments.map(segment => {
            const start = toSvg(segment.start.xIn, segment.start.yIn);
            const end = toSvg(segment.end.xIn, segment.end.yIn);
            const control = segment.control ? toSvg(segment.control.xIn, segment.control.yIn) : null;
            const endHeadingHandle = headingHandlePosition(segment.end);
            const endHeadingSvg = toSvg(endHeadingHandle.xIn, endHeadingHandle.yIn);
            const pathData = segment.type === 'curved' && control
              ? `M ${start.x} ${start.y} Q ${control.x} ${control.y} ${end.x} ${end.y}`
              : `M ${start.x} ${start.y} L ${end.x} ${end.y}`;

            return (
              <g key={segment.id}>
                {control && (
                  <>
                    <line x1={start.x} y1={start.y} x2={control.x} y2={control.y} stroke="rgba(255,255,255,0.2)" strokeDasharray="1.2 1.2" strokeWidth="0.35" />
                    <line x1={control.x} y1={control.y} x2={end.x} y2={end.y} stroke="rgba(255,255,255,0.2)" strokeDasharray="1.2 1.2" strokeWidth="0.35" />
                  </>
                )}
                <path d={pathData} fill="none" stroke="rgba(124,77,255,0.88)" strokeWidth="0.85" />

                <line x1={end.x} y1={end.y} x2={endHeadingSvg.x} y2={endHeadingSvg.y} stroke="rgba(68,138,255,0.7)" strokeWidth="0.35" />
                <circle cx={endHeadingSvg.x} cy={endHeadingSvg.y} r="2.1" fill="#448aff" stroke="white" strokeWidth="0.35" onPointerDown={(event) => beginDrag(event, { type: 'endHeading', id: segment.id })} />
                <circle cx={end.x} cy={end.y} r="2.5" fill="#7c4dff" stroke="white" strokeWidth="0.35" onPointerDown={(event) => beginDrag(event, { type: 'end', id: segment.id })} />

                {control && (
                  <circle cx={control.x} cy={control.y} r="2.2" fill="#00e676" stroke="white" strokeWidth="0.35" onPointerDown={(event) => beginDrag(event, { type: 'control', id: segment.id })} />
                )}

                {showRobotSilhouettes && (
                  <PoseOutline pose={segment.end} toSvg={toSvg} robotSizePct={robotSizePct} opacity={0.38} stroke="rgba(68,138,255,0.75)" />
                )}
              </g>
            );
          })}

          <line x1={startSvg.x} y1={startSvg.y} x2={startHeadingSvg.x} y2={startHeadingSvg.y} stroke="rgba(255,82,82,0.8)" strokeWidth="0.4" />
          <circle cx={startHeadingSvg.x} cy={startHeadingSvg.y} r="2.1" fill="#ff5252" stroke="white" strokeWidth="0.35" onPointerDown={(event) => beginDrag(event, { type: 'startHeading' })} />
          {showRobotSilhouettes && (
            <PoseOutline pose={selectedStart} toSvg={toSvg} robotSizePct={robotSizePct} opacity={0.7} stroke="rgba(68,138,255,0.95)" />
          )}
          <circle cx={startSvg.x} cy={startSvg.y} r="2.7" fill="#448aff" stroke="white" strokeWidth="0.35" onPointerDown={(event) => beginDrag(event, { type: 'start' })} />

          {previewPose && showRobotSilhouettes && (
            <PoseOutline pose={previewPose} toSvg={toSvg} robotSizePct={robotSizePct} opacity={0.95} stroke="rgba(255,255,255,0.95)" />
          )}
        </svg>
      </div>

      <div className="glass-card" style={{ padding: '0.5rem', display: 'grid', gap: '0.35rem', maxHeight: '28vh', minHeight: 170, overflow: 'auto' }}>
        {blocks.map((block, index) => (
          <div key={block.id} style={{ display: 'grid', gridTemplateColumns: 'repeat(6, minmax(0, 1fr)) auto', gap: '0.35rem', alignItems: 'center' }}>
            <span style={miniLabel}>{index + 1}</span>
            <select value={block.type} onChange={(e) => updateBlock(block.id, { type: e.target.value as BlockType })} style={selectStyle}>
              <option value="straight">Straight</option>
              <option value="curved">Curved</option>
            </select>
            <input value={block.endXIn} onChange={(e) => updateBlock(block.id, { endXIn: Number(e.target.value) })} style={inputStyle} />
            <input value={block.endYIn} onChange={(e) => updateBlock(block.id, { endYIn: Number(e.target.value) })} style={inputStyle} />
            <input value={block.endHeadingDeg} onChange={(e) => updateBlock(block.id, { endHeadingDeg: normalizeDeg(Number(e.target.value)) })} style={inputStyle} />
            <input value={block.headingWeight} onChange={(e) => updateBlock(block.id, { headingWeight: Number(e.target.value) })} style={inputStyle} />
            <button onClick={() => setBlocks(prev => prev.filter(candidate => candidate.id !== block.id))} style={iconBtnStyle}><Trash2 size={12} /></button>

            {block.type === 'curved' && (
              <>
                <span style={{ ...miniLabel, gridColumn: '1 / 2' }}>ctrl</span>
                <input value={block.controlXIn ?? 0} onChange={(e) => updateBlock(block.id, { controlXIn: Number(e.target.value) })} style={inputStyle} />
                <input value={block.controlYIn ?? 0} onChange={(e) => updateBlock(block.id, { controlYIn: Number(e.target.value) })} style={inputStyle} />
                <input value={block.controlHeadingDeg ?? 0} onChange={(e) => updateBlock(block.id, { controlHeadingDeg: normalizeDeg(Number(e.target.value)) })} style={inputStyle} />
                <input value={block.controlScale} onChange={(e) => updateBlock(block.id, { controlScale: Number(e.target.value) })} style={inputStyle} />
              </>
            )}
          </div>
        ))}
      </div>

      <textarea
        readOnly
        value={generatedCode}
        style={{
          width: '100%',
          minHeight: 180,
          maxHeight: '24vh',
          resize: 'vertical',
          overflow: 'auto',
          background: 'rgba(0,0,0,0.28)',
          color: 'var(--text-primary)',
          border: '1px solid var(--border)',
          borderRadius: '8px',
          padding: '0.65rem',
          fontFamily: 'var(--font-mono)',
          fontSize: '0.68rem',
          lineHeight: 1.45,
        }}
      />
    </div>
  );
}

function PoseOutline({
  pose,
  toSvg,
  robotSizePct,
  opacity,
  stroke,
}: {
  pose: PoseLike;
  toSvg: (xIn: number, yIn: number) => { x: number; y: number };
  robotSizePct: number;
  opacity: number;
  stroke: string;
}) {
  const svgPose = toSvg(pose.xIn, pose.yIn);
  return (
    <g transform={`translate(${svgPose.x}, ${svgPose.y}) rotate(${-pose.headingDeg})`} pointerEvents="none">
      <rect
        x={-robotSizePct / 2}
        y={-robotSizePct / 2}
        width={robotSizePct}
        height={robotSizePct}
        rx="1"
        fill={`rgba(68,138,255,${opacity * 0.3})`}
        stroke={stroke}
        strokeWidth="0.45"
      />
      <line
        x1={-robotSizePct / 2}
        y1={-robotSizePct / 2}
        x2={robotSizePct / 2}
        y2={-robotSizePct / 2}
        stroke="rgba(255,82,82,0.95)"
        strokeWidth="0.8"
      />
    </g>
  );
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  background: 'rgba(255,255,255,0.05)',
  color: 'var(--text-primary)',
  border: '1px solid var(--border)',
  borderRadius: 6,
  padding: '0.35rem 0.45rem',
  fontSize: '0.65rem',
};

const selectStyle: React.CSSProperties = {
  ...inputStyle,
  appearance: 'none',
};

const autoSelectStyle: React.CSSProperties = {
  ...selectStyle,
  background: '#ffffff',
  color: '#111111',
};

const btnStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 6,
  background: 'rgba(124,77,255,0.14)',
  color: '#d9ccff',
  border: '1px solid rgba(124,77,255,0.25)',
  borderRadius: 8,
  padding: '0.4rem 0.6rem',
  cursor: 'pointer',
  fontSize: '0.66rem',
  fontWeight: 600,
};

const iconBtnStyle: React.CSSProperties = {
  ...btnStyle,
  padding: '0.35rem',
  justifyContent: 'center',
};

const miniLabel: React.CSSProperties = {
  fontSize: '0.62rem',
  color: 'var(--text-dim)',
  fontWeight: 700,
  textTransform: 'uppercase',
};

const labelStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '0.25rem',
  fontSize: '0.62rem',
  color: 'var(--text-dim)',
};

const toggleLabel: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: '0.3rem',
  fontSize: '0.62rem',
  color: 'var(--text-primary)',
};
