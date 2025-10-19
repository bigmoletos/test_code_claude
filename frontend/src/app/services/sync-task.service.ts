import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { SyncTask } from '../models/sync-task.model';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root'
})
export class SyncTaskService {
  private apiUrl = 'http://localhost:8081/api/sync-tasks';

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {
    this.logger.info('SyncTaskService initialisé - API URL: {}', this.apiUrl);
  }

  getAllTasks(): Observable<SyncTask[]> {
    this.logger.debug('Récupération de toutes les tâches');
    return this.http.get<SyncTask[]>(this.apiUrl).pipe(
      tap(tasks => this.logger.info('Reçu {} tâches du backend', tasks.length)),
      catchError(error => {
        this.logger.error('Erreur lors de la récupération des tâches', error);
        throw error;
      })
    );
  }

  getTaskById(id: number): Observable<SyncTask> {
    this.logger.debug('Récupération de la tâche ID: {}', id);
    return this.http.get<SyncTask>(`${this.apiUrl}/${id}`).pipe(
      tap(task => this.logger.info('Tâche récupérée: {}', task.name)),
      catchError(error => {
        this.logger.error('Erreur lors de la récupération de la tâche ID: {}', error, id);
        throw error;
      })
    );
  }

  createTask(task: SyncTask): Observable<SyncTask> {
    this.logger.info('Création d\'une nouvelle tâche: {}', task.name);
    this.logger.debug('Source: {} -> Destination: {}', task.sourcePath, task.destinationPath);
    return this.http.post<SyncTask>(this.apiUrl, task).pipe(
      tap(createdTask => this.logger.info('Tâche créée avec succès - ID: {}', createdTask.id)),
      catchError(error => {
        this.logger.error('Erreur lors de la création de la tâche: {}', error, task.name);
        throw error;
      })
    );
  }

  updateTask(id: number, task: SyncTask): Observable<SyncTask> {
    this.logger.info('Mise à jour de la tâche ID: {}', id);
    return this.http.put<SyncTask>(`${this.apiUrl}/${id}`, task).pipe(
      tap(() => this.logger.info('Tâche ID: {} mise à jour avec succès', id)),
      catchError(error => {
        this.logger.error('Erreur lors de la mise à jour de la tâche ID: {}', error, id);
        throw error;
      })
    );
  }

  deleteTask(id: number): Observable<void> {
    this.logger.warn('Suppression de la tâche ID: {}', id);
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.logger.info('Tâche ID: {} supprimée avec succès', id)),
      catchError(error => {
        this.logger.error('Erreur lors de la suppression de la tâche ID: {}', error, id);
        throw error;
      })
    );
  }

  toggleTaskStatus(id: number): Observable<SyncTask> {
    this.logger.info('Basculement du statut de la tâche ID: {}', id);
    return this.http.post<SyncTask>(`${this.apiUrl}/${id}/toggle`, {}).pipe(
      tap(task => this.logger.info('Statut basculé - Tâche {} maintenant: {}', task.name, task.active ? 'ACTIVE' : 'INACTIVE')),
      catchError(error => {
        this.logger.error('Erreur lors du basculement du statut de la tâche ID: {}', error, id);
        throw error;
      })
    );
  }

  triggerSync(id: number): Observable<any> {
    this.logger.info('Déclenchement manuel de la synchronisation pour la tâche ID: {}', id);
    return this.http.post(`${this.apiUrl}/${id}/trigger`, {}).pipe(
      tap(() => this.logger.info('Synchronisation démarrée avec succès pour la tâche ID: {}', id)),
      catchError(error => {
        this.logger.error('Erreur lors du déclenchement de la synchronisation pour la tâche ID: {}', error, id);
        throw error;
      })
    );
  }

  getSyncStatus(id: number): Observable<{running: boolean}> {
    this.logger.debug('Vérification du statut de synchronisation pour la tâche ID: {}', id);
    return this.http.get<{running: boolean}>(`${this.apiUrl}/${id}/status`).pipe(
      tap(status => this.logger.debug('Statut pour tâche ID: {} = {}', id, status.running ? 'EN COURS' : 'ARRÊTÉE')),
      catchError(error => {
        this.logger.error('Erreur lors de la vérification du statut pour la tâche ID: {}', error, id);
        throw error;
      })
    );
  }
}
