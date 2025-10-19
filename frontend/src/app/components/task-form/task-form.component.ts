import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { SyncTask } from '../../models/sync-task.model';
import { SyncTaskService } from '../../services/sync-task.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './task-form.component.html',
  styleUrl: './task-form.component.css'
})
export class TaskFormComponent implements OnInit {
  task: SyncTask = {
    name: '',
    sourcePath: '',
    destinationPath: '',
    intervalMinutes: 120,
    active: true,
    useChecksum: true
  };

  isEditMode = false;
  taskId?: number;

  constructor(
    private taskService: SyncTaskService,
    private route: ActivatedRoute,
    private router: Router,
    private logger: LoggerService
  ) {
    this.logger.info('TaskFormComponent créé');
  }

  ngOnInit(): void {
    this.taskId = Number(this.route.snapshot.paramMap.get('id'));
    this.logger.debug('Initialisation TaskFormComponent - ID: {}', this.taskId);

    if (this.taskId) {
      this.isEditMode = true;
      this.logger.info('Mode édition activé pour la tâche ID: {}', this.taskId);
      this.loadTask();
    } else {
      this.logger.info('Mode création de nouvelle tâche');
    }
  }

  loadTask(): void {
    if (this.taskId) {
      this.logger.debug('Chargement des données de la tâche ID: {}', this.taskId);
      this.taskService.getTaskById(this.taskId).subscribe({
        next: (task) => {
          this.task = task;
          this.logger.info('Tâche chargée: {}', task.name);
        },
        error: (err) => {
          this.logger.error('Erreur lors du chargement de la tâche ID: {}', err, this.taskId);
        }
      });
    }
  }

  onSubmit(): void {
    if (this.isEditMode && this.taskId) {
      this.logger.info('Soumission du formulaire - Mise à jour tâche: {}', this.task.name);
      this.taskService.updateTask(this.taskId, this.task).subscribe({
        next: () => {
          this.logger.info('Tâche mise à jour avec succès - Redirection vers /tasks');
          this.router.navigate(['/tasks']);
        },
        error: (err) => {
          this.logger.error('Erreur lors de la mise à jour de la tâche', err);
        }
      });
    } else {
      this.logger.info('Soumission du formulaire - Création tâche: {}', this.task.name);
      this.taskService.createTask(this.task).subscribe({
        next: () => {
          this.logger.info('Tâche créée avec succès - Redirection vers /tasks');
          this.router.navigate(['/tasks']);
        },
        error: (err) => {
          this.logger.error('Erreur lors de la création de la tâche', err);
        }
      });
    }
  }
}
