import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule],
  template: `
    <nav class="navbar">
      <div class="nav-container">
        <h1 class="logo">Folder Sync</h1>
        <div class="nav-links">
          <a routerLink="/tasks" routerLinkActive="active">Tâches</a>
          <a routerLink="/logs" routerLinkActive="active">Logs</a>
        </div>
      </div>
    </nav>
    <main>
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .navbar {
      background: #2c3e50;
      color: white;
      padding: 15px 0;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .nav-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .logo {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }

    .nav-links {
      display: flex;
      gap: 20px;
    }

    .nav-links a {
      color: white;
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 4px;
      transition: background 0.3s;
    }

    .nav-links a:hover {
      background: rgba(255,255,255,0.1);
    }

    .nav-links a.active {
      background: rgba(255,255,255,0.2);
    }

    main {
      min-height: calc(100vh - 70px);
      background: #f5f5f5;
    }
  `]
})
export class AppComponent {
  title = 'Folder Sync';
}
