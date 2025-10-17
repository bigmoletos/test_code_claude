import { Component, OnInit } from '@angular/core';
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
export class LogListComponent implements OnInit {
  logs: SyncLog[] = [];
  currentPage = 0;
  totalPages = 0;
  pageSize = 20;
  taskId?: number;

  constructor(
    private logService: SyncLogService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const taskIdParam = params.get('taskId');
      this.taskId = taskIdParam ? Number(taskIdParam) : undefined;
      this.loadLogs();
    });
  }

  loadLogs(): void {
    if (this.taskId) {
      this.logService.getLogsByTask(this.taskId, this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => this.handleResponse(response),
          error: (err) => console.error('Erreur chargement logs', err)
        });
    } else {
      this.logService.getAllLogs(this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => this.handleResponse(response),
          error: (err) => console.error('Erreur chargement logs', err)
        });
    }
  }

  handleResponse(response: PageResponse<SyncLog>): void {
    this.logs = response.content;
    this.totalPages = response.totalPages;
    this.currentPage = response.number;
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
