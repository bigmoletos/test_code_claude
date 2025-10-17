import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SyncLog } from '../models/sync-task.model';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class SyncLogService {
  private apiUrl = 'http://localhost:8080/api/sync-logs';

  constructor(private http: HttpClient) {}

  getAllLogs(page: number = 0, size: number = 20): Observable<PageResponse<SyncLog>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<SyncLog>>(this.apiUrl, { params });
  }

  getLogsByTask(taskId: number, page: number = 0, size: number = 20): Observable<PageResponse<SyncLog>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<SyncLog>>(`${this.apiUrl}/task/${taskId}`, { params });
  }

  getLogById(id: number): Observable<SyncLog> {
    return this.http.get<SyncLog>(`${this.apiUrl}/${id}`);
  }
}
