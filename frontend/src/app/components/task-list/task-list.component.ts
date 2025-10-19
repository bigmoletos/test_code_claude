import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { SyncTask } from '../../models/sync-task.model';
import { SyncTaskService } from '../../services/sync-task.service';
import { LoggerService } from '../../services/logger.service';

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
    private router: Router,
    private logger: LoggerService
  ) {
    this.logger.info('TaskListComponent créé');
  }

  ngOnInit(): void {
    this.logger.info('Initialisation TaskListComponent');
    this.loadTasks();
    // Rafraîchir les statuts toutes les 5 secondes
    this.logger.debug('Configuration du rafraîchissement automatique (5s)');
    setInterval(() => this.updateSyncStatuses(), 5000);
  }

  loadTasks(): void {
    this.logger.debug('Chargement de la liste des tâches');
    this.taskService.getAllTasks().subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.logger.info('Liste des tâches chargée: {} tâche(s)', tasks.length);
        this.updateSyncStatuses();
      },
      error: (err) => {
        this.logger.error('Erreur lors du chargement des tâches', err);
      }
    });
  }

  updateSyncStatuses(): void {
    this.logger.trace('Mise à jour des statuts de synchronisation');
    this.tasks.forEach(task => {
      if (task.id) {
        this.taskService.getSyncStatus(task.id).subscribe({
          next: (status) => {
            this.syncStatuses.set(task.id!, status.running);
            if (status.running) {
              this.logger.debug('Tâche ID: {} en cours de synchronisation', task.id);
            }
          },
          error: (err) => {
            this.logger.warn('Erreur lors de la récupération du statut pour tâche ID: {}', task.id);
          }
        });
      }
    });
  }

  toggleTask(task: SyncTask): void {
    this.logger.info('Basculement du statut de la tâche: {} (ID: {})', task.name, task.id);
    if (task.id) {
      this.taskService.toggleTaskStatus(task.id).subscribe({
        next: () => {
          this.logger.info('Statut basculé avec succès pour: {}', task.name);
          this.loadTasks();
        },
        error: (err) => {
          this.logger.error('Erreur lors du basculement du statut pour: {}', err, task.name);
        }
      });
    }
  }

  triggerSync(task: SyncTask): void {
    this.logger.info('Déclenchement manuel de la synchronisation pour: {} (ID: {})', task.name, task.id);
    if (task.id && !this.syncStatuses.get(task.id)) {
      this.taskService.triggerSync(task.id).subscribe({
        next: () => {
          this.logger.info('Synchronisation démarrée avec succès pour: {}', task.name);
          alert('Synchronisation démarrée');
          this.updateSyncStatuses();
        },
        error: (err) => {
          this.logger.error('Erreur lors du déclenchement de la synchronisation pour: {}', err, task.name);
        }
      });
    } else if (this.syncStatuses.get(task.id)) {
      this.logger.warn('Synchronisation déjà en cours pour: {}', task.name);
    }
  }

  deleteTask(task: SyncTask): void {
    this.logger.warn('Demande de suppression de la tâche: {} (ID: {})', task.name, task.id);
    if (task.id && confirm(`Supprimer la tâche "${task.name}" ?`)) {
      this.logger.info('Suppression confirmée pour: {}', task.name);
      this.taskService.deleteTask(task.id).subscribe({
        next: () => {
          this.logger.info('Tâche supprimée avec succès: {}', task.name);
          this.loadTasks();
        },
        error: (err) => {
          this.logger.error('Erreur lors de la suppression de: {}', err, task.name);
        }
      });
    } else {
      this.logger.debug('Suppression annulée pour: {}', task.name);
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
