import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { SyncTask } from '../../models/sync-task.model';
import { SyncTaskService } from '../../services/sync-task.service';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './task-list.component.html',
  styleUrl: './task-list.component.css'
})
export class TaskListComponent implements OnInit {
  tasks: SyncTask[] = [];
  syncStatuses: Map<number, boolean> = new Map();

  constructor(
    private taskService: SyncTaskService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadTasks();
    // Rafraîchir les statuts toutes les 5 secondes
    setInterval(() => this.updateSyncStatuses(), 5000);
  }

  loadTasks(): void {
    this.taskService.getAllTasks().subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.updateSyncStatuses();
      },
      error: (err) => console.error('Erreur lors du chargement des tâches', err)
    });
  }

  updateSyncStatuses(): void {
    this.tasks.forEach(task => {
      if (task.id) {
        this.taskService.getSyncStatus(task.id).subscribe({
          next: (status) => this.syncStatuses.set(task.id!, status.running),
          error: (err) => console.error('Erreur statut sync', err)
        });
      }
    });
  }

  toggleTask(task: SyncTask): void {
    if (task.id) {
      this.taskService.toggleTaskStatus(task.id).subscribe({
        next: () => this.loadTasks(),
        error: (err) => console.error('Erreur toggle', err)
      });
    }
  }

  triggerSync(task: SyncTask): void {
    if (task.id && !this.syncStatuses.get(task.id)) {
      this.taskService.triggerSync(task.id).subscribe({
        next: () => {
          alert('Synchronisation démarrée');
          this.updateSyncStatuses();
        },
        error: (err) => console.error('Erreur trigger sync', err)
      });
    }
  }

  deleteTask(task: SyncTask): void {
    if (task.id && confirm(`Supprimer la tâche "${task.name}" ?`)) {
      this.taskService.deleteTask(task.id).subscribe({
        next: () => this.loadTasks(),
        error: (err) => console.error('Erreur suppression', err)
      });
    }
  }

  viewLogs(task: SyncTask): void {
    this.router.navigate(['/logs/task', task.id]);
  }

  formatDate(date: string | undefined): string {
    if (!date) return 'Jamais';
    return new Date(date).toLocaleString('fr-FR');
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
}
