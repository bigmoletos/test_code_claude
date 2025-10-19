import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { SyncLog } from '../../models/sync-task.model';
import { SyncLogService, PageResponse } from '../../services/sync-log.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-log-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './log-list.component.html',
  styleUrl: './log-list.component.css'
})
export class LogListComponent implements OnInit, OnDestroy {
  logs: SyncLog[] = [];
  currentPage = 0;
  totalPages = 0;
  pageSize = 20;
  taskId?: number;
  private refreshInterval?: number;

  constructor(
    private logService: SyncLogService,
    private route: ActivatedRoute,
    private logger: LoggerService
  ) {
    this.logger.info('LogListComponent créé');
  }

  ngOnInit(): void {
    this.logger.info('Initialisation LogListComponent');
    this.route.paramMap.subscribe(params => {
      const taskIdParam = params.get('taskId');
      this.taskId = taskIdParam ? Number(taskIdParam) : undefined;

      if (this.taskId) {
        this.logger.info('Mode filtré par tâche - ID: {}', this.taskId);
      } else {
        this.logger.info('Mode affichage tous les logs');
      }

      this.loadLogs();

      // Rafraîchir automatiquement toutes les 5 secondes
      this.logger.debug('Configuration du rafraîchissement automatique (5s)');
      this.refreshInterval = window.setInterval(() => {
        this.logger.trace('Rafraîchissement automatique des logs');
        this.loadLogs();
      }, 5000);
    });
  }

  ngOnDestroy(): void {
    this.logger.debug('Destruction LogListComponent - Nettoyage interval');
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadLogs(): void {
    this.logger.debug('Chargement des logs - Page: {}, TaskID: {}', this.currentPage, this.taskId || 'tous');

    if (this.taskId) {
      this.logService.getLogsByTask(this.taskId, this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            this.logger.info('Logs reçus pour la tâche ID: {} - {} logs', this.taskId, response.content.length);
            this.handleResponse(response);
          },
          error: (err) => {
            this.logger.error('Erreur lors du chargement des logs pour la tâche ID: {}', err, this.taskId);
          }
        });
    } else {
      this.logService.getAllLogs(this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            this.logger.info('Tous les logs reçus - {} logs', response.content.length);
            this.handleResponse(response);
          },
          error: (err) => {
            this.logger.error('Erreur lors du chargement de tous les logs', err);
          }
        });
    }
  }

  handleResponse(response: PageResponse<SyncLog>): void {
    this.logger.debug('Traitement de la réponse - Logs: {}, Total pages: {}', response.content?.length, response.totalPages);
    this.logs = response.content;
    this.totalPages = response.totalPages;
    this.currentPage = response.number;
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.logger.debug('Navigation vers la page suivante: {}', this.currentPage + 1);
      this.currentPage++;
      this.loadLogs();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.logger.debug('Navigation vers la page précédente: {}', this.currentPage - 1);
      this.currentPage--;
      this.loadLogs();
    }
  }

  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleString('fr-FR');
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  getDuration(log: SyncLog): string {
    if (!log.endTime) return 'En cours...';
    const start = new Date(log.startTime).getTime();
    const end = new Date(log.endTime).getTime();
    const duration = (end - start) / 1000;
    if (duration < 60) return `${Math.round(duration)}s`;
    return `${Math.floor(duration / 60)}m ${Math.round(duration % 60)}s`;
  }

  getStatusClass(status: string): string {
    const classes: any = {
      'COMPLETED': 'status-success',
      'RUNNING': 'status-running',
      'FAILED': 'status-error',
      'CANCELLED': 'status-cancelled'
    };
    return classes[status] || '';
  }
}
