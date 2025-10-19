import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { SyncLog } from '../../models/sync-task.model';
import { SyncLogService, PageResponse } from '../../services/sync-log.service';

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
    private route: ActivatedRoute
  ) {
    console.log('LogListComponent - Constructeur appelé');
  }

  ngOnInit(): void {
    console.log('LogListComponent - ngOnInit appelé');
    this.route.paramMap.subscribe(params => {
      const taskIdParam = params.get('taskId');
      this.taskId = taskIdParam ? Number(taskIdParam) : undefined;
      console.log('LogListComponent - taskId extrait des params:', this.taskId);
      this.loadLogs();

      // Rafraîchir automatiquement toutes les 5 secondes
      this.refreshInterval = window.setInterval(() => {
        this.loadLogs();
      }, 5000);
    });
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadLogs(): void {
    console.log('loadLogs appelé, taskId:', this.taskId, 'page:', this.currentPage);
    if (this.taskId) {
      this.logService.getLogsByTask(this.taskId, this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            console.log('Réponse logs reçue (par tâche):', response);
            this.handleResponse(response);
          },
          error: (err) => {
            console.error('Erreur chargement logs (par tâche):', err);
            console.error('Détails erreur:', err.message, err.status, err.error);
          }
        });
    } else {
      this.logService.getAllLogs(this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            console.log('Réponse logs reçue (tous):', response);
            this.handleResponse(response);
          },
          error: (err) => {
            console.error('Erreur chargement logs (tous):', err);
            console.error('Détails erreur:', err.message, err.status, err.error);
          }
        });
    }
  }

  handleResponse(response: PageResponse<SyncLog>): void {
    console.log('handleResponse appelé avec:', response);
    console.log('Nombre de logs dans content:', response.content?.length);
    this.logs = response.content;
    this.totalPages = response.totalPages;
    this.currentPage = response.number;
    console.log('Logs assignés, array length:', this.logs?.length);
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadLogs();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
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
