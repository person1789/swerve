import Dexie, { type Table } from 'dexie';
import type { TelemetryEntry } from '../types/telemetry';

export interface SavedSession {
    id?: number;
    name: string;
    description: string;
    date: number;
    duration: number;
    frames: TelemetryEntry[];
}

export class SwerveScopeDB extends Dexie {
    sessions!: Table<SavedSession>;

    constructor() {
        super('SwerveScopeDB');
        this.version(1).stores({
            sessions: '++id, name, date' 
        });
    }
}

export const db = new SwerveScopeDB();
