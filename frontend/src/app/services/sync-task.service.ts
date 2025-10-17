import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SyncTask } from '../models/sync-task.model';

@Injectable({
  providedIn: 'root'
})
export class SyncTaskService {
  private apiUrl = 'http://localhost:8080/api/sync-tasks';

  constructor(private http: HttpClient) {}

  getAllTasks(): Observable<SyncTask[]> {
    return this.http.get<SyncTask[]>(this.apiUrl);
  }

  getTaskById(id: number): Observable<SyncTask> {
    return this.http.get<SyncTask>(`${this.apiUrl}/${id}`);
  }

  createTask(task: SyncTask): Observable<SyncTask> {
    return this.http.post<SyncTask>(this.apiUrl, task);
  }

  updateTask(id: number, task: SyncTask): Observable<SyncTask> {
    return this.http.put<SyncTask>(`${this.apiUrl}/${id}`, task);
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  toggleTaskStatus(id: number): Observable<SyncTask> {
    return this.http.post<SyncTask>(`${this.apiUrl}/${id}/toggle`, {});
  }

  triggerSync(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/trigger`, {});
  }

  getSyncStatus(id: number): Observable<{running: boolean}> {
    return this.http.get<{running: boolean}>(`${this.apiUrl}/${id}/status`);
  }
}
