export interface SyncTask {
  id?: number;
  name: string;
  sourcePath: string;
  destinationPath: string;
  intervalMinutes: number;
  active: boolean;
  useChecksum: boolean;
  lastSyncTime?: string;
  nextSyncTime?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SyncLog {
  id: number;
  syncTaskId: number;
  syncTaskName: string;
  startTime: string;
  endTime?: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  filesScanned: number;
  filesCopied: number;
  filesUpdated: number;
  filesDeleted: number;
  filesSkipped: number;
  totalBytes: number;
  errorMessage?: string;
  details?: string;
}
