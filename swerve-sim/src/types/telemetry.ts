export interface TelemetryEntry {
    timestamp: number;
    x: number;
    y: number;
    heading: number;
    targets: number[][]; // [speed, angle] x 4
    actuals: number[][]; // [speed, angle] x 4
    isMaintaining: boolean;
    isSnapping: boolean;
    mode: string;
    [key: string]: any;
}
