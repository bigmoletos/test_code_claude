import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { SyncTask } from '../../models/sync-task.model';
import { SyncTaskService } from '../../services/sync-task.service';

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
    private router: Router
  ) {}

  ngOnInit(): void {
    this.taskId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.taskId) {
      this.isEditMode = true;
      this.loadTask();
    }
  }

  loadTask(): void {
    if (this.taskId) {
      this.taskService.getTaskById(this.taskId).subscribe({
        next: (task) => this.task = task,
        error: (err) => console.error('Erreur chargement tâche', err)
      });
    }
  }

  onSubmit(): void {
    if (this.isEditMode && this.taskId) {
      this.taskService.updateTask(this.taskId, this.task).subscribe({
        next: () => this.router.navigate(['/tasks']),
        error: (err) => console.error('Erreur mise à jour', err)
      });
    } else {
      this.taskService.createTask(this.task).subscribe({
        next: () => this.router.navigate(['/tasks']),
        error: (err) => console.error('Erreur création', err)
      });
    }
  }
}
