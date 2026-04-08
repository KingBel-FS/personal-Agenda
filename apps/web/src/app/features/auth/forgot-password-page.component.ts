import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password-page.component.html',
  styleUrl: './forgot-password-page.component.scss'
})
export class ForgotPasswordPageComponent {
  private static readonly requestUrl = '/api/v1/auth/password-reset/request';
  private static readonly jsonHeaders = new HttpHeaders({
    'Content-Type': 'application/json',
    Accept: 'application/json'
  });

  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  protected readonly submitting = signal(false);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.http
      .post<{ data: { message: string } }>(
        ForgotPasswordPageComponent.requestUrl,
        this.form.getRawValue(),
        { headers: ForgotPasswordPageComponent.jsonHeaders }
      )
      .subscribe({
        next: ({ data }) => {
          this.successMessage.set(data.message);
          this.submitting.set(false);
        },
        error: (error) => {
          this.errorMessage.set(
            error?.error?.error?.message ?? "Impossible d'envoyer le mail de réinitialisation."
          );
          this.submitting.set(false);
        }
      });
  }
}
