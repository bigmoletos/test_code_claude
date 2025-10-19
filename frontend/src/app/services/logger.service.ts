import { Injectable } from '@angular/core';

/**
 * Enum pour les niveaux de log
 */
export enum LogLevel {
  TRACE = 0,
  DEBUG = 1,
  INFO = 2,
  WARN = 3,
  ERROR = 4,
  FATAL = 5
}

/**
 * Service de logging pour Angular
 * Fournit des méthodes de logging avec différents niveaux
 * Compatible avec le CustomLogger du backend
 */
@Injectable({
  providedIn: 'root'
})
export class LoggerService {
  private currentLevel: LogLevel = LogLevel.INFO;
  private enableConsole: boolean = true;
  private enableRemoteLogging: boolean = false;
  private logHistory: LogEntry[] = [];
  private maxHistorySize: number = 100;

  constructor() {
    // Configuration par défaut
    this.loadConfig();
  }

  /**
   * Charge la configuration depuis le localStorage ou utilise les valeurs par défaut
   */
  private loadConfig(): void {
    const savedLevel = localStorage.getItem('logLevel');
    if (savedLevel) {
      this.currentLevel = parseInt(savedLevel, 10);
    }

    // En production, on monte le niveau à WARN
    if (!this.isDevMode()) {
      this.currentLevel = LogLevel.WARN;
    }
  }

  /**
   * Vérifie si on est en mode développement
   */
  private isDevMode(): boolean {
    return !window.location.hostname.includes('prod') &&
           (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');
  }

  /**
   * Configure le niveau de log
   */
  setLevel(level: LogLevel): void {
    this.currentLevel = level;
    localStorage.setItem('logLevel', level.toString());
  }

  /**
   * Active/désactive la console
   */
  setConsoleOutput(enabled: boolean): void {
    this.enableConsole = enabled;
  }

  /**
   * Active/désactive le logging distant (vers backend)
   */
  setRemoteLogging(enabled: boolean): void {
    this.enableRemoteLogging = enabled;
  }

  /**
   * Vérifie si un niveau de log est activé
   */
  isLevelEnabled(level: LogLevel): boolean {
    return level >= this.currentLevel;
  }

  /**
   * Log de niveau TRACE
   */
  trace(message: string, ...args: any[]): void {
    this.log(LogLevel.TRACE, message, args);
  }

  /**
   * Log de niveau DEBUG
   */
  debug(message: string, ...args: any[]): void {
    this.log(LogLevel.DEBUG, message, args);
  }

  /**
   * Log de niveau INFO
   */
  info(message: string, ...args: any[]): void {
    this.log(LogLevel.INFO, message, args);
  }

  /**
   * Log de niveau WARN
   */
  warn(message: string, ...args: any[]): void {
    this.log(LogLevel.WARN, message, args);
  }

  /**
   * Log de niveau ERROR
   */
  error(message: string, error?: Error, ...args: any[]): void {
    this.log(LogLevel.ERROR, message, args, error);
  }

  /**
   * Log de niveau FATAL
   */
  fatal(message: string, error?: Error, ...args: any[]): void {
    this.log(LogLevel.FATAL, message, args, error);
  }

  /**
   * Méthode principale de logging
   */
  private log(level: LogLevel, message: string, args: any[], error?: Error): void {
    if (!this.isLevelEnabled(level)) {
      return;
    }

    const timestamp = new Date();
    const formattedMessage = this.formatMessage(level, message, args);
    const logEntry: LogEntry = {
      timestamp,
      level,
      message: formattedMessage,
      error
    };

    // Ajouter à l'historique
    this.addToHistory(logEntry);

    // Afficher dans la console
    if (this.enableConsole) {
      this.writeToConsole(level, formattedMessage, error);
    }

    // Envoyer au backend si activé
    if (this.enableRemoteLogging) {
      this.sendToBackend(logEntry);
    }
  }

  /**
   * Formate le message avec les arguments
   */
  private formatMessage(level: LogLevel, message: string, args: any[]): string {
    const timestamp = new Date().toISOString();
    const levelStr = LogLevel[level];

    // Remplacer les {} par les arguments
    let formattedMsg = message;
    args.forEach((arg, index) => {
      formattedMsg = formattedMsg.replace('{}', this.stringify(arg));
    });

    return `[${timestamp}] [${levelStr}] [Frontend] - ${formattedMsg}`;
  }

  /**
   * Convertit un objet en string pour l'affichage
   */
  private stringify(obj: any): string {
    if (obj === null || obj === undefined) {
      return String(obj);
    }
    if (typeof obj === 'object') {
      try {
        return JSON.stringify(obj);
      } catch (e) {
        return String(obj);
      }
    }
    return String(obj);
  }

  /**
   * Écrit dans la console du navigateur
   */
  private writeToConsole(level: LogLevel, message: string, error?: Error): void {
    const style = this.getConsoleStyle(level);

    switch (level) {
      case LogLevel.TRACE:
      case LogLevel.DEBUG:
        console.log(`%c${message}`, style);
        break;
      case LogLevel.INFO:
        console.info(`%c${message}`, style);
        break;
      case LogLevel.WARN:
        console.warn(`%c${message}`, style);
        break;
      case LogLevel.ERROR:
      case LogLevel.FATAL:
        console.error(`%c${message}`, style);
        if (error) {
          console.error(error);
        }
        break;
    }
  }

  /**
   * Retourne le style CSS pour la console
   */
  private getConsoleStyle(level: LogLevel): string {
    switch (level) {
      case LogLevel.TRACE:
        return 'color: #888888';
      case LogLevel.DEBUG:
        return 'color: #00BCD4';
      case LogLevel.INFO:
        return 'color: #4CAF50';
      case LogLevel.WARN:
        return 'color: #FF9800';
      case LogLevel.ERROR:
        return 'color: #F44336; font-weight: bold';
      case LogLevel.FATAL:
        return 'color: #9C27B0; font-weight: bold';
      default:
        return '';
    }
  }

  /**
   * Ajoute une entrée à l'historique
   */
  private addToHistory(entry: LogEntry): void {
    this.logHistory.push(entry);

    // Limiter la taille de l'historique
    if (this.logHistory.length > this.maxHistorySize) {
      this.logHistory.shift();
    }
  }

  /**
   * Récupère l'historique des logs
   */
  getHistory(): LogEntry[] {
    return [...this.logHistory];
  }

  /**
   * Efface l'historique
   */
  clearHistory(): void {
    this.logHistory = [];
  }

  /**
   * Envoie les logs au backend (à implémenter selon vos besoins)
   */
  private sendToBackend(entry: LogEntry): void {
    // TODO: Implémenter l'envoi au backend via HTTP si nécessaire
    // Par exemple, créer un endpoint /api/frontend-logs
  }

  /**
   * Groupe de logs
   */
  group(title: string): void {
    if (this.enableConsole) {
      console.group(title);
    }
  }

  /**
   * Fin du groupe de logs
   */
  groupEnd(): void {
    if (this.enableConsole) {
      console.groupEnd();
    }
  }

  /**
   * Mesure de performance
   */
  time(label: string): void {
    if (this.enableConsole && this.isLevelEnabled(LogLevel.DEBUG)) {
      console.time(label);
    }
  }

  /**
   * Fin de mesure de performance
   */
  timeEnd(label: string): void {
    if (this.enableConsole && this.isLevelEnabled(LogLevel.DEBUG)) {
      console.timeEnd(label);
    }
  }

  /**
   * Log une table (utile pour les tableaux d'objets)
   */
  table(data: any): void {
    if (this.enableConsole && this.isLevelEnabled(LogLevel.DEBUG)) {
      console.table(data);
    }
  }
}

/**
 * Interface pour une entrée de log
 */
export interface LogEntry {
  timestamp: Date;
  level: LogLevel;
  message: string;
  error?: Error;
}

