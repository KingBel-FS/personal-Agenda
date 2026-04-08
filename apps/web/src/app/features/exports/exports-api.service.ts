import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export interface ExportAuditItem {
  id: string;
  exportFormat: 'EXCEL' | 'PDF' | string;
  exportScope: 'TASKS' | 'HISTORY' | 'FULL' | string;
  periodFrom: string | null;
  periodTo: string | null;
  status: string;
  fileName: string | null;
  rowCount: number | null;
  durationMs: number | null;
  createdAt: string;
  completedAt: string | null;
}

export interface ExportHistoryResponse {
  items: ExportAuditItem[];
}

export interface ExportRequestPayload {
  format: 'EXCEL' | 'PDF';
  scope: 'TASKS' | 'HISTORY' | 'FULL';
  fromDate?: string | null;
  toDate?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ExportsApiService {
  private static readonly baseUrl = '/api/v1/exports';

  private readonly http = inject(HttpClient);

  listHistory() {
    return this.http.get<{ data: ExportHistoryResponse }>(`${ExportsApiService.baseUrl}/history`);
  }

  download(payload: ExportRequestPayload) {
    return this.http.post(`${ExportsApiService.baseUrl}/download`, payload, {
      observe: 'response',
      responseType: 'blob'
    });
  }
}
