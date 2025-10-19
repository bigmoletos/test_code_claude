import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { SyncLog } from '../models/sync-task.model';
import { LoggerService } from './logger.service';

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
  private apiUrl = 'http://localhost:8081/api/sync-logs';

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {
    this.logger.info('SyncLogService initialisé - API URL: {}', this.apiUrl);
  }

  getAllLogs(page: number = 0, size: number = 20): Observable<PageResponse<SyncLog>> {
    this.logger.debug('Récupération des logs (page={}, size={})', page, size);
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    const url = `${this.apiUrl}?page=${page}&size=${size}`;
    this.logger.trace('URL: {}', url);

    return this.http.get<PageResponse<SyncLog>>(this.apiUrl, { params }).pipe(
      tap(response => this.logger.info('Reçu {} logs sur {} total', response.content.length, response.totalElements)),
      catchError(error => {
        this.logger.error('Erreur lors de la récupération des logs', error);
        throw error;
      })
    );
  }

  getLogsByTask(taskId: number, page: number = 0, size: number = 20): Observable<PageResponse<SyncLog>> {
    this.logger.debug('Récupération des logs pour la tâche ID: {} (page={}, size={})', taskId, page, size);
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    const url = `${this.apiUrl}/task/${taskId}?page=${page}&size=${size}`;
    this.logger.trace('URL: {}', url);

    return this.http.get<PageResponse<SyncLog>>(`${this.apiUrl}/task/${taskId}`, { params }).pipe(
      tap(response => this.logger.info('Reçu {} logs pour la tâche ID: {} sur {} total',
        response.content.length, taskId, response.totalElements)),
      catchError(error => {
        this.logger.error('Erreur lors de la récupération des logs pour la tâche ID: {}', error, taskId);
        throw error;
      })
    );
  }

  getLogById(id: number): Observable<SyncLog> {
    this.logger.debug('Récupération du log ID: {}', id);
    return this.http.get<SyncLog>(`${this.apiUrl}/${id}`).pipe(
      tap(log => this.logger.info('Log récupéré - ID: {}, Statut: {}', log.id, log.status)),
      catchError(error => {
        this.logger.error('Erreur lors de la récupération du log ID: {}', error, id);
        throw error;
      })
    );
  }
}
