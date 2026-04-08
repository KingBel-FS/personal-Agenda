import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ExportsApiService, type ExportAuditItem } from './exports-api.service';

@Component({
  selector: 'app-exports-page',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './exports-page.component.html',
  styleUrl: './exports-page.component.scss'
})
export class ExportsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly exportsApi = inject(ExportsApiService);

  protected readonly loading = signal(true);
  protected readonly downloading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly history = signal<ExportAuditItem[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    format: ['EXCEL' as 'EXCEL' | 'PDF', Validators.required],
    scope: ['FULL' as 'TASKS' | 'HISTORY' | 'FULL', Validators.required],
    fromDate: [''],
    toDate: ['']
  });

  constructor() {
    this.loadHistory();
  }

  protected submit(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.downloading.set(true);
    const payload = this.form.getRawValue();
    this.exportsApi.download({
      format: payload.format,
      scope: payload.scope,
      fromDate: payload.fromDate || null,
      toDate: payload.toDate || null
    }).subscribe({
      next: (response) => {
        if (!response.body) {
          this.errorMessage.set("Le fichier exporté est vide.");
          this.downloading.set(false);
          return;
        }
        const fileName = this.extractFileName(response.headers.get('content-disposition')) ?? 'export.bin';
        const url = URL.createObjectURL(response.body);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = fileName;
        anchor.click();
        URL.revokeObjectURL(url);
        this.successMessage.set('Export généré et téléchargé.');
        this.downloading.set(false);
        this.loadHistory();
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error?.message ?? "Impossible de générer l'export.");
        this.downloading.set(false);
      }
    });
  }

  protected formatLabel(format: string): string {
    return format === 'PDF' ? 'PDF' : 'Excel';
  }

  protected scopeLabel(scope: string): string {
    switch (scope) {
      case 'TASKS':
        return 'Tâches';
      case 'HISTORY':
        return 'Historique';
      default:
        return 'Complet';
    }
  }

  protected statusLabel(status: string): string {
    switch (status) {
      case 'SUCCESS':
        return 'Réussi';
      case 'FAILED':
        return 'Échoué';
      default:
        return 'En cours';
    }
  }

  private loadHistory(): void {
    this.loading.set(true);
    this.exportsApi.listHistory().subscribe({
      next: ({ data }) => {
        this.history.set(data.items);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error?.message ?? "Impossible de charger l'historique des exports.");
        this.loading.set(false);
      }
    });
  }

  private extractFileName(contentDisposition: string | null): string | null {
    if (!contentDisposition) {
      return null;
    }
    const match = /filename="([^"]+)"/.exec(contentDisposition);
    return match ? match[1] : null;
  }
}
