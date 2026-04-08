import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-activate-page',
  imports: [RouterLink],
  templateUrl: './activate-page.component.html',
  styleUrl: './activate-page.component.scss'
})
export class ActivatePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const token = params.get('token');

      if (!token) {
        this.loading.set(false);
        this.errorMessage.set("Lien d'activation invalide.");
        return;
      }

      this.loading.set(true);
      this.http
        .get<{ data: { status: string } }>(`/api/v1/auth/activate?token=${encodeURIComponent(token)}`)
        .subscribe({
          next: ({ data }) => {
            this.successMessage.set(`Compte activé. Statut : ${data.status}.`);
            this.errorMessage.set('');
            this.loading.set(false);
          },
          error: (error) => {
            this.errorMessage.set(
              error?.error?.error?.message ?? "Impossible d'activer le compte."
            );
            this.successMessage.set('');
            this.loading.set(false);
          }
        });
    });
  }
}
